package com.codename1.demos.grub.services;

import com.codename1.demos.grub.interfaces.CuddleProvider;
import java.util.List;

/**
 * Interface pour gérer les prestataires de calins favoris de l'utilisateur
 */
public interface FavoritesService {
    /**
     * Ajoute un prestataire de calins aux favoris
     * @param provider le prestataire à ajouter
     */
    void addFavorite(CuddleProvider provider);

    /**
     * Supprime un prestataire de calins des favoris
     * @param provider le prestataire à supprimer
     */
    void removeFavorite(CuddleProvider provider);

    /**
     * Vérifie si un prestataire de calins est dans les favoris
     * @param provider le prestataire à vérifier
     * @return vrai si le prestataire est en favori, faux sinon
     */
    boolean isFavorite(CuddleProvider provider);

    /**
     * Obtient la liste des prestataires de calins favoris
     * @return la liste des prestataires favoris
     */
    List<CuddleProvider> getFavorites();

    /**
     * Toggle le statut favori d'un prestataire de calins
     * @param provider le prestataire à modifier
     */
    void toggleFavorite(CuddleProvider provider);
}
