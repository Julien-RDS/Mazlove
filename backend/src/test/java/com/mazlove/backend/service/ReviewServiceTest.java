package com.mazlove.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Vérifie l'anonymisation partielle des auteurs d'avis (affichés publiquement). */
class ReviewServiceTest {

    @Test
    void fullNameIsShortenedToFirstNameAndInitial() {
        assertEquals("Camille D.", ReviewService.shortName("Camille Dupont"));
        assertEquals("Jean P.", ReviewService.shortName("  Jean   Pierre  "));
    }

    @Test
    void singleNameIsKeptAsIs() {
        assertEquals("Camille", ReviewService.shortName("Camille"));
    }

    @Test
    void missingNameFallsBackToNeutralLabel() {
        assertEquals("Membre Mazlove", ReviewService.shortName(null));
        assertEquals("Membre Mazlove", ReviewService.shortName("   "));
    }
}
