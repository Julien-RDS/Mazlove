package com.mazlove.backend.integration;

import com.mazlove.backend.dto.ProviderCreateDTO;
import com.mazlove.backend.dto.ProviderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProviderIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateProvider() {
        ProviderCreateDTO dto = new ProviderCreateDTO(
                "Test Provider",
                "Test description",
                48.8566, 2.3522,
                "https://example.com/image.jpg",
                "+33123456789"
        );

        webTestClient.post()
                .uri("/api/providers")
                .contentType(APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProviderDTO.class)
                .value(provider -> {
                    assertNotNull(provider.getId());
                    assertEquals(dto.name(), provider.getName());
                    assertEquals(dto.description(), provider.getDescription());
                });
    }

    @Test
    void shouldGetProvidersNearby() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/providers")
                        .queryParam("lat", "48.8566")
                        .queryParam("lng", "2.3522")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProviderDTO.class)
                .hasSizeGreaterThan(0);
    }

    @Test
    void shouldGetProviderById() {
        webTestClient.get()
                .uri("/api/providers/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProviderDTO.class)
                .value(provider -> {
                    assertNotNull(provider.getId());
                    assertNotNull(provider.getName());
                });
    }
}
