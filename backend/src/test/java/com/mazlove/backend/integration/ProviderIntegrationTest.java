package com.mazlove.backend.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de bout en bout sur l'application complète (sécurité, contrôleurs, base, cache).
 *
 * Désactivés par défaut car ils exigent PostgreSQL et Redis démarrés : lancer avec
 *   MAZLOVE_IT=true mvn test
 * Sans cette variable, "mvn test" reste rapide et passe sur n'importe quelle machine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MAZLOVE_IT", matches = "true")
class ProviderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listingNearbyProvidersIsPublic() throws Exception {
        mockMvc.perform(get("/api/providers").param("lat", "48.87").param("lng", "2.35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void creatingAProviderRequiresAnAccount() throws Exception {
        mockMvc.perform(post("/api/providers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unknownProviderReturns404WithAReadableMessage() throws Exception {
        mockMvc.perform(get("/api/providers/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingCoordinatesReturn400() throws Exception {
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
