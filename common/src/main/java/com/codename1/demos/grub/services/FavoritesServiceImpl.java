package com.codename1.demos.grub.services;

import com.codename1.demos.grub.interfaces.CuddleProvider;
import com.codename1.demos.grub.models.Restaurant;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.codename1.io.Preferences;

/**
 * Implémentation du service de gestion des favoris
 */
public class FavoritesServiceImpl implements FavoritesService {
    private static final String FAVORITES_KEY = "favorites";
    private static FavoritesServiceImpl instance;
    private List<CuddleProvider> favorites;

    private FavoritesServiceImpl() {
        favorites = new ArrayList<>();
        loadFavorites();
    }

    public static FavoritesServiceImpl getInstance() {
        if (instance == null) {
            instance = new FavoritesServiceImpl();
        }
        return instance;
    }

    @Override
    public boolean isFavorite(CuddleProvider provider) {
        return favorites.stream()
                .anyMatch(p -> ((com.codename1.demos.grub.models.Restaurant) p).getId().equals(((com.codename1.demos.grub.models.Restaurant) provider).getId()));
    }

    @Override
    public void addFavorite(CuddleProvider provider) {
        if (!favorites.stream()
                .anyMatch(p -> ((com.codename1.demos.grub.models.Restaurant) p).getId().equals(((com.codename1.demos.grub.models.Restaurant) provider).getId()))) {
            favorites.add(provider);
            saveFavorites();
        }
    }

    @Override
    public void removeFavorite(CuddleProvider provider) {
        String idToRemove = ((com.codename1.demos.grub.models.Restaurant) provider).getId();
        favorites.removeIf(p -> ((com.codename1.demos.grub.models.Restaurant) p).getId().equals(idToRemove));
        saveFavorites();
    }

    @Override
    public List<CuddleProvider> getFavorites() {
        return Collections.unmodifiableList(favorites);
    }

    @Override
    public void toggleFavorite(CuddleProvider provider) {
        if (isFavorite(provider)) {
            removeFavorite(provider);
        } else {
            addFavorite(provider);
        }
    }

    private void loadFavorites() {
        String favoritesStr = Preferences.get(FAVORITES_KEY, "");
        if (!favoritesStr.isEmpty()) {
            String[] favoritesArray = favoritesStr.split(",");
            for (String id : favoritesArray) {
                if (!id.isEmpty()) {
                    Restaurant provider = (Restaurant) new com.codename1.demos.grub.models.Restaurant();
                    ((com.codename1.demos.grub.models.Restaurant) provider).setId(id);
                    favorites.add((CuddleProvider) provider);
                }
            }
        }
    }

    private void saveFavorites() {
        StringBuilder sb = new StringBuilder();
        for (CuddleProvider provider : favorites) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(((com.codename1.demos.grub.models.Restaurant) provider).getId());
        }
        Preferences.set(FAVORITES_KEY, sb.toString());
    }
            addFavorite(restaurant);
        }
    }
}
