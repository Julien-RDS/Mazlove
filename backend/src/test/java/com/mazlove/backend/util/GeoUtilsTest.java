package com.mazlove.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

    @Test
    void parisToLyonIsAbout392Km() {
        double km = GeoUtils.distanceKm(48.8566, 2.3522, 45.7640, 4.8357);
        assertEquals(391.5, km, 2.0);
    }

    @Test
    void boundingBoxContainsPointsInsideTheRadius() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(48.87, 2.35, 10);
        // Un point à ~2,4 km (Paris 11e) doit être dans la boîte de 10 km.
        assertTrue(48.8592 > box.minLat() && 48.8592 < box.maxLat());
        assertTrue(2.3791 > box.minLng() && 2.3791 < box.maxLng());
        // Un degré de latitude ≈ 111 km : la demi-hauteur d'une boîte de 10 km ≈ 0,09°.
        assertEquals(0.09, box.maxLat() - 48.87, 0.001);
    }

    @Test
    void boundingBoxNearThePoleOpensAllLongitudes() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(89.9999999, 0, 10);
        assertEquals(-180.0, box.minLng());
        assertEquals(180.0, box.maxLng());
        assertEquals(90.0, box.maxLat());
    }

    @Test
    void roundKeepsTheRequestedDecimals() {
        assertEquals(48.87, GeoUtils.round(48.8712, 2));
        assertEquals(2.352, GeoUtils.round(2.35218, 3));
    }
}
