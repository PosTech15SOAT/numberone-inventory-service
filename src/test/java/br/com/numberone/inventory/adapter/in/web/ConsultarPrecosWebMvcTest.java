
package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.ConsultarPrecosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConsultarPrecosController.class)
class ConsultarPrecosWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarPrecosService consultarPrecosService;

    @Test
    void deveRetornar200ComJacksonConfigurado() throws Exception {
        UUID id = UUID.randomUUID();

        when(consultarPrecosService.consultar(List.of(id)))
                .thenReturn(List.of(
                        new ConsultarPrecosService.PrecoItem(
                                id,
                                new BigDecimal("200.00"),
                                "BRL"
                        )
                ));

        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryItemIds\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].inventoryItemId")
                        .value(id.toString()))
                .andExpect(jsonPath("$.items[0].salePrice")
                        .value("200.00"))
                .andExpect(jsonPath("$.items[0].currency")
                        .value("BRL"));
    }

    @Test
    void deveRetornar400ParaListaVazia() throws Exception {
        mockMvc.perform(post("/internal/inventory/consultar-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryItemIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(consultarPrecosService);
    }
}
