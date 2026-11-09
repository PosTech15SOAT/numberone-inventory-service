
package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.ConsultarPrecosService;
import br.com.numberone.inventory.application.service.ConsultarPrecosService.ConsultaPrecosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConsultarPrecosControllerTest {

    private MockMvc mockMvc;
    private ConsultarPrecosService service;

    @BeforeEach
    void configurar() {
        service = mock(ConsultarPrecosService.class);
        
        var controller = new ConsultarPrecosController(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ConsultarPrecosExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar200ComPrecoDoItem() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.consultar(List.of(id)))
                .thenReturn(List.of(
                        new ConsultarPrecosService.PrecoItem(
                                id,
                                new BigDecimal("200.00"),
                                "BRL"
                        )
                ));

        String request = "{\"inventoryItemIds\":[\"" + id + "\"]}";

        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].inventoryItemId")
                        .value(id.toString()))
                .andExpect(jsonPath("$.items[0].salePrice")
                        .value("200.00"))
                .andExpect(jsonPath("$.items[0].currency")
                        .value("BRL"));

        verify(service).consultar(List.of(id));
    }

    @Test
    void deveRetornar400ParaListaVazia() throws Exception {
        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryItemIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar400ParaJsonInvalido() throws Exception {
        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar422QuandoItemNaoExiste() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.consultar(anyList()))
                .thenThrow(new ConsultaPrecosException(
                        "INVENTORY_ITEMS_NOT_FOUND",
                        List.of(id)
                ));

        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryItemIds\":[\"" + id + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("INVENTORY_ITEMS_NOT_FOUND"))
                .andExpect(jsonPath("$.inventoryItemIds[0]")
                        .value(id.toString()));
    }

    @Test
    void deveRetornar422QuandoItemNaoTemPreco() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.consultar(anyList()))
                .thenThrow(new ConsultaPrecosException(
                        "INVENTORY_ITEM_WITHOUT_PRICE",
                        List.of(id)
                ));

        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryItemIds\":[\"" + id + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("INVENTORY_ITEM_WITHOUT_PRICE"))
                .andExpect(jsonPath("$.inventoryItemIds[0]")
                        .value(id.toString()));
    }
}
