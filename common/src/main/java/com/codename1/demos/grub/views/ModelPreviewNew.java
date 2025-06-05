package com.codename1.demos.grub.views;

import com.codename1.components.ScaleImageButton;
import com.codename1.demos.grub.interfaces.ICuddleProvider;
import com.codename1.ui.*;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;

import static com.codename1.ui.CN.convertToPixels;
import static com.codename1.ui.util.Resources.getGlobalResources;

public class ModelPreviewNew extends Container {
    private static EncodedImage placeHolder = EncodedImage.createFromImage(getGlobalResources().getImage("placeholder.png"), false);
    private Image restImage;
    private Label restNameLabel;
    private Label restCategoryLabel;
    private Label estimatedDeliveryTimeLabel;
    private Label ratingLabel;
    private Label distanceLabel;

    public ModelPreviewNew(ICuddleProvider model, Resources globalResources) {
        setLayout(new BorderLayout());
        setUIID("ModelPreview");

        // Charger l'image du modèle
        restImage = restaurant.getPicture();
        if (restImage == null) {
            restImage = placeHolder;
        }

        // Créer le bouton image
        ScaleImageButton restImageButton = new ScaleImageButton(restImage);
        restImageButton.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED);
        restImageButton.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter la navigation vers la page du modèle
        });

        setLeadComponent(restImageButton);

        // Créer les labels pour le nom et la catégorie
        modelNameLabel = new Label(model.getName(), "ModelPreviewNameLabel");
        restCategoryLabel = new Label(restaurant.getCategory(), "RestPreviewCategoryLabel");

        // Créer le conteneur principal avec l'image et les informations
        Container restTopView = new Container(new LayeredLayout());
        restTopView.add(restImageButton);

        Container nameAndCategoryCnt = new Container(new BorderLayout());
        nameAndCategoryCnt.setUIID("RestaurantPreviewDetailsCnt");
        Container restDetails = BoxLayout.encloseY(restNameLabel, restCategoryLabel);
        nameAndCategoryCnt.add(BorderLayout.SOUTH, restDetails);
        nameAndCategoryCnt.setUIID("BottomShadowContainer");
        restTopView.add(nameAndCategoryCnt);

        // Créer les labels pour le temps de livraison, la note et la distance
        estimatedDeliveryTimeLabel = new Label(" " + restaurant.getEstimatedDeliveryTime() + " mins", "RestPreviewDeliveryTime");
        estimatedDeliveryTimeLabel.setIcon(globalResources.getImage("delivery-time-icon.png").scaled(convertToPixels(4), convertToPixels(4)));

        ratingLabel = new Label(" " + restaurant.getRating() + "/5", "RestPreviewRating");
        ratingLabel.setIcon(globalResources.getImage("rating-icon.png").scaled(convertToPixels(4), convertToPixels(4)));

        distanceLabel = new Label(" " + restaurant.getDistance() + "km", "RestPreviewDistance");
        distanceLabel.setIcon(globalResources.getImage("distance-icon.png").scaled(convertToPixels(4), convertToPixels(4)));

        // Ajouter les composants au conteneur principal
        add(BorderLayout.SOUTH, FlowLayout.encloseCenter(estimatedDeliveryTimeLabel, ratingLabel, distanceLabel));
        add(BorderLayout.CENTER, restTopView);
    }

    public void update(Restaurant restaurant) {
        // Mettre à jour les informations du restaurant
        if (restaurant.getPicture() != null) {
            restImage = restaurant.getPicture();
        }
        restNameLabel.setText(restaurant.getName());
        restCategoryLabel.setText(restaurant.getCategory());
        estimatedDeliveryTimeLabel.setText(" " + restaurant.getEstimatedDeliveryTime() + " mins");
        ratingLabel.setText(" " + restaurant.getRating() + "/5");
        distanceLabel.setText(" " + restaurant.getDistance() + "km");
        revalidate();
    }
}
