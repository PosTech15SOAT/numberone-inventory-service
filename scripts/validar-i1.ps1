param(
    [string]$Base = 'http://127.0.0.1:18081'
)
$ErrorActionPreference = 'Stop'
$correlation = [guid]::NewGuid().ToString()
$subject = [guid]::NewGuid().ToString()
$admin = @{
    'X-Authenticated-Subject' = $subject
    'X-Correlation-Id' = $correlation
}
$os = @{'X-Correlation-Id' = $correlation}
$billing = @{'X-Correlation-Id' = $correlation}
$client = [System.Net.Http.HttpClient]::new()

function Assert-That($Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
}

function Send-Request([string]$Method, [string]$Path, $Headers, $Body) {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$Base$Path")
    foreach ($header in $Headers.GetEnumerator()) {
        [void]$request.Headers.TryAddWithoutValidation($header.Key, $header.Value)
    }
    if ($null -ne $Body) {
        $json = ConvertTo-Json -InputObject $Body -Depth 10 -Compress
        $request.Content = [System.Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json')
    }
    return $client.SendAsync($request)
}

function Read-Reply($Task) {
    $response = $Task.GetAwaiter().GetResult()
    try {
        $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        return @{Status = [int]$response.StatusCode; Body = ConvertFrom-Json -AsHashtable $raw}
    } finally { $response.Dispose() }
}

function Call([int]$Status, [string]$Method, [string]$Path, $Headers, $Body = $null) {
    $reply = Read-Reply (Send-Request $Method $Path $Headers $Body)
    Assert-That ($reply.Status -eq $Status) "$Method $Path esperava $Status, recebeu $($reply.Status) $($reply.Body | ConvertTo-Json -Compress)"
    return $reply.Body
}

function Create-Item([int]$Stock) {
    $item = Call 201 POST '/api/admin/itens' $admin @{
        codigo = "I1-$([guid]::NewGuid())"; nome = 'Peca I1'; descricao = 'Peca sintetica'
        tipoItem = 'PECA'; unidadeMedida = 'UNIDADE'; custoUnitario = '10.25'; precoVenda = '20.40'
        quantidadeEstoque = 0; estoqueMinimo = 0
    }
    if ($Stock -gt 0) {
        $entry = Call 201 POST '/api/admin/estoque/entrada' $admin @{
            idItemEstoque = $item.id; quantidade = $Stock; origemMovimentacao = 'COMPRA'
        }
        Assert-That ($entry.quantidadeDepois -eq $Stock) 'Entrada nao atualizou saldo'
    }
    return $item.id
}

function Balance([string]$Id) {
    (Call 200 GET "/api/admin/itens/$Id" $admin).quantidadeEstoque
}

function Movements([string]$Id) {
    Call 200 GET "/api/admin/estoque/itens/$Id/movimentacoes" $admin
}

function Operation($Items) {
    return @{
        operationId = [guid]::NewGuid().ToString(); serviceOrderId = [guid]::NewGuid().ToString()
        serviceOrderItemId = [guid]::NewGuid().ToString(); responsibleUserId = $subject
        items = @($Items)
    }
}

function Supply([string]$Id, $Quantity) { return @{inventoryItemId = $Id; quantity = $Quantity} }

function Consume-Headers($Operation) { return $os + @{'Idempotency-Key' = $Operation.operationId} }

function Consume([int]$Status, $Operation) {
    Call $Status POST '/internal/inventory/consumptions' (Consume-Headers $Operation) $Operation
}

function Lookup([int]$Status, [string]$Id) {
    Call $Status GET "/internal/inventory/operations/$Id" $os
}

try {
    $a = Create-Item 5
    $b = Create-Item 3
    $reference = Call 200 GET "/internal/inventory/items/$a" $os
    Assert-That ($reference.id -eq $a -and $reference.active -and $reference.description -eq 'Peca sintetica' -and $reference.referenceUnitPrice -ceq '20.40') 'Referencia invalida'
    $prices = Call 200 POST '/internal/inventory/consultar-precos' $billing @{inventoryItemIds = @($a)}
    Assert-That ($prices.items[0].salePrice -ceq '20.40' -and $prices.items[0].currency -eq 'BRL') 'Resposta de precos invalida'
    $null = Call 404 GET "/internal/inventory/items/$([guid]::NewGuid())" $os
    $null = Call 400 GET "/internal/inventory/items/$a" @{}
    $null = Call 200 GET "/api/admin/itens/$a" @{}
    'OK referencia decimal, precos sem credenciais e correlacao obrigatoria'

    $attempt = Operation @((Supply $b 1), (Supply $a 1), (Supply $a 2))
    $original = Consume 200 $attempt
    Assert-That ($original.result -eq 'CONSUMED' -and $original.operationId -eq $attempt.operationId -and
        @($original.movements | Sort-Object -Unique).Count -eq 2 -and (Balance $a) -eq 2 -and (Balance $b) -eq 2) 'Lote nao baixado atomicamente'
    $durable = Lookup 200 $attempt.operationId
    Assert-That (($durable.movements -join ',') -eq ($original.movements -join ',')) 'CT-08 nao devolveu movimentos originais'
    "OK lote $($attempt.operationId), movimentos $($original.movements -join ','), saldos 5/3 -> 2/2"

    $attempt.items = @((Supply $a 3), (Supply $b 1))
    $replay = Consume 200 $attempt
    Assert-That (($replay.movements -join ',') -eq ($original.movements -join ',')) 'Replay normalizado mudou movimentos'
    $attempt.items = @((Supply $a 4), (Supply $b 1))
    $conflict = Consume 409 $attempt
    Assert-That ($conflict.code -eq 'IDEMPOTENCY_CONFLICT' -and $conflict.operationId -eq $attempt.operationId -and
        $conflict.correlationId -eq $correlation -and (Balance $a) -eq 2 -and (Balance $b) -eq 2) 'Conflito mudou saldo ou identidade'
    Assert-That (((Lookup 200 $attempt.operationId).movements -join ',') -eq ($original.movements -join ',')) 'Conflito mudou resultado'
    'OK replay canonico e conflito com alteracao de corpo'

    $empty = Create-Item 0
    $before = @(Movements $a).Count
    $shortage = Operation @((Supply $a 1), (Supply $empty 1))
    $rejected = Consume 409 $shortage
    $known = Lookup 200 $shortage.operationId
    Assert-That ($rejected.result -eq 'REJECTED' -and $known.result -eq 'REJECTED' -and
        $known.code -eq 'INSUFFICIENT_STOCK' -and $known.movements.Count -eq 0 -and
        (Balance $a) -eq 2 -and (Balance $empty) -eq 0 -and @(Movements $a).Count -eq $before) 'Recusa nao duravel ou baixa parcial'
    $null = Call 201 POST '/api/admin/estoque/entrada' $admin @{
        idItemEstoque = $empty; quantidade = 2; origemMovimentacao = 'COMPRA'
    }
    Assert-That ((Consume 409 $shortage).result -eq 'REJECTED' -and (Balance $empty) -eq 2) 'Reposicao mudou recusa original'
    "OK recusa $($shortage.operationId), zero movimentos, saldo preservado apos reposicao e replay"

    foreach ($invalidItems in @(
        @{rows = @((Supply $a 0))},
        @{rows = @((Supply $a -1))},
        @{rows = @((Supply $a 1.5))},
        @{rows = @((Supply $a '1'))},
        @{rows = @((Supply $a 2147483648))},
        @{rows = @((Supply $a 2147483647), (Supply $a 1))}
    )) {
        $invalid = Operation $invalidItems.rows
        $null = Consume 400 $invalid
        Assert-That ((Lookup 404 $invalid.operationId).result -eq 'UNKNOWN') 'Pedido invalido deixou operacao'
    }
    $mismatch = Operation @((Supply $a 1))
    $null = Call 400 POST '/internal/inventory/consumptions' ($os + @{'Idempotency-Key' = [guid]::NewGuid().ToString()}) $mismatch
    'OK validacao de quantidade inteira, overflow e divergencia da chave'

    $last = Create-Item 1
    $first = Operation @((Supply $last 1))
    $second = Operation @((Supply $last 1))
    $one = Send-Request POST '/internal/inventory/consumptions' (Consume-Headers $first) $first
    $two = Send-Request POST '/internal/inventory/consumptions' (Consume-Headers $second) $second
    $races = @((Read-Reply $one), (Read-Reply $two))
    Assert-That ((($races.Status | Sort-Object) -join ',') -eq '200,409' -and (Balance $last) -eq 0) 'Disputa permitiu mais ou menos que uma baixa'
    Assert-That ((@(Movements $last | Where-Object tipoMovimentacao -eq 'BAIXA')).Count -eq 1) 'Disputa registrou movimentos indevidos'
    "OK disputa $($first.operationId) / $($second.operationId), HTTP $($races.Status -join '/'), saldo 1 -> 0"

    $same = Create-Item 2
    $sameAttempt = Operation @((Supply $same 1))
    $one = Send-Request POST '/internal/inventory/consumptions' (Consume-Headers $sameAttempt) $sameAttempt
    $two = Send-Request POST '/internal/inventory/consumptions' (Consume-Headers $sameAttempt) $sameAttempt
    $x = Read-Reply $one
    $y = Read-Reply $two
    Assert-That ($x.Status -eq 200 -and $y.Status -eq 200 -and
        $x.Body.movements[0] -eq $y.Body.movements[0] -and (Balance $same) -eq 1) 'Mesmo ID simultaneo efetuou mais de uma baixa'
    "OK replay simultaneo $($sameAttempt.operationId), um movimento e saldo 2 -> 1"
} finally { $client.Dispose() }
