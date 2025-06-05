package com.codename1.demos.grub.controllers;

import com.codename1.demos.grub.views.ProfileRecommendationView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.mazlove.recommendation.ProfileRecommendation;
import com.codename1.ui.util.Resources;

public class ProfileRecommendationsController {
    private String userId;
    private ProfileRecommendation recommendationSystem;
    private ProfileRecommendationView recommendationsView;

    public ProfileRecommendationsController(String userId, Resources globalResources) {
        this.userId = userId;
        this.recommendationSystem = new ProfileRecommendation(globalResources);

        Form recommendationsForm = new Form(new BorderLayout());
        recommendationsForm.getToolbar().hideToolbar();
        recommendationsForm.setFormBottomPaddingEditingMode(true);
        
        recommendationsView = new ProfileRecommendationView(userId, globalResources);
        recommendationsForm.add(BorderLayout.CENTER, recommendationsView);
        
        // Configurer les actions de la vue
        recommendationsView.setBackAction(() -> recommendationsForm.showBack());
        recommendationsView.setRefreshAction(() -> recommendationsView.updateRecommendations());
        
        recommendationsForm.show();
    }

    public void show() {
        // La méthode show() est déjà appelée dans le constructeur
    }

    public void openProfile(String profileId) {
        // TODO: Implémenter l'action d'ouverture d'un profil
    }

    public void rateProfile(String profileId, int rating) {
        // TODO: Implémenter la notation d'un profil
    }

    public void updateRecommendations() {
        recommendationsView.updateRecommendations();
    }

    public ProfileRecommendationView getRecommendationsView() {
        return recommendationsView;
    }
}
