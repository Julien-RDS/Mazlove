package com.mazlove.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Fiche praticien telle qu'affichée publiquement (liste et page de détail).
 *
 * Classe mutable (et non record) : elle est mise en cache dans Redis, sérialisée en JSON
 * avec informations de type, ce qui demande un constructeur vide et des setters.
 * Les collections doivent donc rester des ArrayList/HashSet "classiques" (pas de List.of()).
 */
@Data
public class ProviderDTO {
    private Long id;

    /** Nom du compte praticien (pas de champ "name" sur Provider). */
    private String userName;
    private String bio;
    private String category;

    /** Moyenne des avis sur 5, ou null si aucun avis : l'interface affiche alors « Nouveau ». */
    private Double rating;
    private long reviewCount;

    /** Identité vérifiée par l'équipe Mazlove. */
    private boolean verified;

    /** Distance depuis la position recherchée, en km arrondie à 0,1 ; null hors recherche géographique. */
    private Double distanceKm;

    /** Position volontairement arrondie à 2 décimales (~1 km) pour protéger le domicile du praticien. */
    private LocationDTO location;

    private List<TimeSlotDTO> availability;
    private Set<String> images;
}
