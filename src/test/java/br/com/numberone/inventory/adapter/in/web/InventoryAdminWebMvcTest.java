package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.port.in.GerenciarCatalogoUseCase;
import br.com.numberone.inventory.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryAdminController.class)
class InventoryAdminWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean GerenciarCatalogoUseCase catalogo;

    private static final String CADASTRO = """
            {"codigo":"A","nome":"Peca","tipoItem":"PECA","unidadeMedida":"UNIDADE",
             "custoUnitario":10,"precoVenda":20,"quantidadeEstoque":0,"estoqueMinimo":0}
            """;

    @Test void leituraAdministrativaNaoExigeCredenciais() throws Exception {
        mvc.perform(get("/api/admin/itens")).andExpect(status().isOk());
        verify(catalogo).listar();
    }

    @Test void cadastroUsaAutorDeclaradoETrataConflito() throws Exception {
        UUID autor = UUID.randomUUID();
        when(catalogo.cadastrar(any(), eq(0), eq(autor))).thenThrow(new DataIntegrityViolationException("duplicado"));
        mvc.perform(post("/api/admin/itens").header("X-Authenticated-Subject", autor)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PERSISTENCE_CONFLICT"));
        verify(catalogo).cadastrar(any(), eq(0), eq(autor));
    }

    @Test void entradaUsaAutorDeclarado() throws Exception {
        UUID autor = UUID.randomUUID(), item = UUID.randomUUID();
        when(catalogo.entrada(eq(item), eq(2), eq(OrigemMovimentacao.COMPRA), isNull(), isNull(), eq(autor)))
                .thenReturn(new MovimentacaoEstoque(UUID.randomUUID(), item, TipoMovimentacao.ENTRADA,
                        OrigemMovimentacao.COMPRA, null, 0, 2, null, autor, LocalDateTime.now()));
        mvc.perform(post("/api/admin/estoque/entrada").header("X-Authenticated-Subject", autor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idItemEstoque\":\"" + item + "\",\"quantidade\":2,\"origemMovimentacao\":\"COMPRA\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioResponsavelId").value(autor.toString()));
    }

    @Test void autorAusenteOuInvalidoERejeitadoNasDuasOperacoes() throws Exception {
        String entrada = "{\"idItemEstoque\":\"" + UUID.randomUUID() + "\",\"quantidade\":2,\"origemMovimentacao\":\"COMPRA\"}";
        for (String path : new String[]{"/api/admin/itens", "/api/admin/estoque/entrada"}) {
            String body = path.endsWith("itens") ? CADASTRO : entrada;
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            mvc.perform(post(path).header("X-Authenticated-Subject", "invalido")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(catalogo);
    }
}
