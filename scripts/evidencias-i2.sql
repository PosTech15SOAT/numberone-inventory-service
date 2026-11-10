-- psql -v closed=<uuid> -v reversed=<uuid> -v rejected=<uuid>
-- Read-only evidence for the synthetic integrated run.
WITH selected AS (
    SELECT * FROM operacao_consumo_estoque
    WHERE operation_id IN (:'closed'::uuid, :'reversed'::uuid, :'rejected'::uuid)
)
SELECT jsonb_pretty(jsonb_build_object(
    'operations', (SELECT jsonb_agg(to_jsonb(o) ORDER BY operation_id) FROM selected o),
    'compensations', (SELECT jsonb_agg(to_jsonb(c) ORDER BY compensation_id) FROM compensacao_estoque c
        WHERE operation_id IN (SELECT operation_id FROM selected)),
    'movements', (SELECT jsonb_agg(to_jsonb(m) ORDER BY created_at,id) FROM movimentacao_estoque m
        WHERE operation_id IN (SELECT operation_id FROM selected)),
    'balances', (SELECT jsonb_agg(jsonb_build_object('itemId',i.id,'balance',i.quantidade_estoque) ORDER BY i.id)
        FROM item_estoque i WHERE id IN (
            SELECT item_estoque_id FROM operacao_consumo_item WHERE operation_id IN (SELECT operation_id FROM selected))),
    'migrations', (SELECT jsonb_agg(jsonb_build_object('version',version,'checksum',checksum,'success',success) ORDER BY installed_rank)
        FROM flyway_schema_history)
));
