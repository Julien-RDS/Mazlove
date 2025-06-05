package com.codename1.demos.grub.views;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileRecommendationView extends Container {
    private final String userId;
    private final Object appNode;
    private final List<Button> recommendationButtons;
    private final Container recommendationsContainer;
    private final Button backButton;
    private final Button refreshButton;

    public ProfileRecommendationView(String userId, Object appNode) {
        super(new BorderLayout());
        this.userId = userId;
        this.appNode = appNode;
        this.recommendationButtons = new ArrayList<>();

        recommendationsContainer = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        recommendationsContainer.setScrollableY(true);
        recommendationsContainer.setScrollVisible(false);

        backButton = new Button("Back");
        refreshButton = new Button("Refresh");

        add(BorderLayout.NORTH, createHeader());
        add(BorderLayout.CENTER, recommendationsContainer);
        add(BorderLayout.SOUTH, createFooter());

        updateRecommendations();
    }

    private Component createHeader() {
        Container header = new Container(new BorderLayout());
        header.setUIID("ProfileRecommendationHeader");

        Container titleContainer = new Container(new BorderLayout());
        titleContainer.setUIID("ProfileRecommendationTitle");
        titleContainer.add(BorderLayout.CENTER, "Profile Recommendations");

        header.add(BorderLayout.CENTER, titleContainer);
        header.add(BorderLayout.EAST, refreshButton);
        return header;
    }

    private Component createFooter() {
        Container footer = new Container(new BorderLayout());
        footer.setUIID("ProfileRecommendationFooter");
        footer.add(BorderLayout.CENTER, backButton);
        return footer;
    }

    public void show() {
        Form form = new Form(new BorderLayout()) {
            {
                getToolbar().hideToolbar();
                add(BorderLayout.CENTER, ProfileRecommendationView.this);
            }
        };
        form.show();
    }

    public void updateRecommendations() {
        recommendationsContainer.removeAll();
        recommendationButtons.clear();

        // TODO: Implémenter la logique de recommandation
        // Pour l'instant, on simule quelques profils
        for (int i = 1; i <= 5; i++) {
            final int profileIndex = i;
            Button recommendationButton = new Button("Profile " + profileIndex);
            recommendationButton.setUIID("ProfileRecommendationButton");
            recommendationButton.addActionListener(e -> {
                String profileId = "profile_" + profileIndex;
                // TODO: Implémenter l'action de sélection d'un profil
            });
            recommendationsContainer.add(recommendationButton);
            recommendationButtons.add(recommendationButton);
        }

        recommendationsContainer.revalidate();
    }

    public void recordProfileInteraction(String profileId, int rating) {
        // TODO: Implémenter la logique d'enregistrement de l'interaction
    }

    public void setBackAction(Runnable action) {
        backButton.addActionListener(e -> action.run());
    }

    public void setRefreshAction(Runnable action) {
        refreshButton.addActionListener(e -> action.run());
    }
}
