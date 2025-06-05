package com.mazlove.recommendation;

import com.codename1.rad.models.Entity;
import com.codename1.rad.models.Property;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.HashMap;
import java.util.Map;

public class ProfileRecommendation {
    private final RecommendationSystem recommender;
    private final Map<String, Map<String, Double>> userProfiles;
    private final Map<String, double[]> profileFeatures;
    private final Resources globalResources;

    public ProfileRecommendation(Resources globalResources) {
        this.recommender = new RecommendationSystem();
        this.userProfiles = new HashMap<>();
        this.profileFeatures = new HashMap<>();
        this.globalResources = globalResources;
    }

    /**
     * Crée un profil utilisateur à partir d'une entité Codename One
     * @param userEntity L'entité utilisateur
     * @param interests Les intérêts de l'utilisateur (ex: cuisine, sport, musique)
     */
    public void createUserProfile(Entity userEntity, Map<String, Double> interests) {
        String userId = userEntity.getText(userEntity.findProperty("id"));
        userProfiles.put(userId, interests);
        
        // Ajouter l'utilisateur au système de recommandation
        recommender.addUser(userId, interests);
    }

    /**
     * Crée un profil à recommander avec ses caractéristiques
     * @param profileEntity L'entité du profil
     * @param features Les caractéristiques du profil (ex: cuisine=0.8, sport=0.5)
     */
    public void createProfile(Entity profileEntity, double[] features) {
        String profileId = profileEntity.getText(profileEntity.findProperty("id"));
        profileFeatures.put(profileId, features);
        
        // Ajouter le profil au système de recommandation
        recommender.addItem(profileId, features, 1200);
    }

    /**
     * Enregistre une interaction utilisateur-profil
     * @param userId ID de l'utilisateur
     * @param profileId ID du profil
     * @param rating Notation de 0.0 à 1.0
     */
    public void recordProfileInteraction(String userId, String profileId, double rating) {
        recommender.recordInteraction(userId, profileId, rating);
    }

    /**
     * Génère des recommandations de profils pour un utilisateur
     * @param userId ID de l'utilisateur
     * @param count Nombre de profils à recommander
     * @return Liste des profils recommandés avec leurs scores
     */
    public Map<String, Double> getProfileRecommendations(String userId, int count) {
        List<Recommendation> recommendations = recommender.recommend(userId, count);
        Map<String, Double> result = new HashMap<>();
        
        for (Recommendation rec : recommendations) {
            result.put(rec.getItemId(), rec.getScore());
        }
        
        return result;
    }

    /**
     * Crée une vue pour afficher les profils recommandés
     * @param userId ID de l'utilisateur
     * @param count Nombre de profils à afficher
     * @return Container avec les profils recommandés
     */
    public Component createRecommendedProfilesView(String userId, int count) {
        Container profilesContainer = new Container(BoxLayout.y());
        Map<String, Double> recommendations = getProfileRecommendations(userId, count);
        
        for (Map.Entry<String, Double> entry : recommendations.entrySet()) {
            String profileId = entry.getKey();
            double score = entry.getValue();
            
            Container profileCnt = new Container(BoxLayout.x());
            
            // Icône du profil
            Style buttonEmblemStyle = UIManager.getInstance().getComponentStyle("ProfileButtonEmblemStyle");
            Image profileImage = globalResources.getImage("account-place-holder.png").fill(80, 80);
            ScaleImageLabel profileImageLabel = new ScaleImageLabel(profileImage);
            profileImageLabel.setUIID("ProfileImageLabel");
            
            // Informations du profil
            Container infoCnt = new Container(BoxLayout.y());
            Label profileName = new Label("Profile " + profileId, "ProfileNameLabel");
            Label scoreLabel = new Label("Score: " + String.format("%.2f", score), "ProfileScoreLabel");
            
            infoCnt.addAll(profileName, scoreLabel);
            
            // Ajouter au conteneur
            profileCnt.addAll(profileImageLabel, infoCnt);
            profilesContainer.add(profileCnt);
        }
        
        return profilesContainer;
    }

    /**
     * Obtient les statistiques du système de recommandation
     * @return Map des statistiques
     */
    public Map<String, Object> getSystemStats() {
        return recommender.getSystemStats();
    }
}
