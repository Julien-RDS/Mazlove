package com.codename1.demos.grub.views;

import com.codename1.components.ScaleImageLabel;
import com.codename1.demos.grub.interfaces.ICuddleProvider;

import com.codename1.ui.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.*;
import com.codename1.ui.util.Resources;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import static com.codename1.ui.CN.*;
import static com.codename1.ui.util.Resources.getGlobalResources;

public class ModelViewNew extends Container {
    private Image modelImage;
    private Container restInfo;
    private static EncodedImage placeHolder;
    private final ICuddleProvider provider;
    private final Resources globalResources;
    private Label imageLabel;

    public ModelViewNew(ICuddleProvider provider, Resources globalResources) {
        this.provider = provider;
        this.globalResources = globalResources;
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        setScrollableY(true);
        setScrollVisible(false);

        if (placeHolder == null) {
            placeHolder = EncodedImage.createFromImage(globalResources.getImage("placeholder.png").scaled(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayWidth() / 2), false);
        }

        modelImage = provider.getPicture() != null ? provider.getPicture() : placeHolder;

        restInfo = new Container(new LayeredLayout());

        // Bouton retour
        Button backButton = new Button(FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
        backButton.setUIID("RestBackButton");
        backButton.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter la navigation arrière
        });

        // Bouton panier
        Button cart = new Button(FontImage.MATERIAL_SHOPPING_CART, "ModelActionButton");
        cart.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter l'action du panier
        });

        // Bouton favori
        Style likeStyle = UIManager.getInstance().getComponentStyle("ModelActionButton");
        CheckBox like = CheckBox.createToggle(FontImage.createMaterial(FontImage.MATERIAL_FAVORITE_BORDER, likeStyle));
        like.setUIID("ModelActionButton");
        like.setPressedIcon(FontImage.createMaterial(FontImage.MATERIAL_FAVORITE, likeStyle));
        like.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter l'action favori
        });

        // Bouton commander
        Button order = new Button("Réserver", "ProviderActionButton");
        order.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter l'action de commande
        });

        // Configuration de l'interface du prestataire
        restInfo = new Container(new LayeredLayout());
        restInfo.add(FlowLayout.encloseIn(backButton, cart, like, order));

        // Gestion de l'image du prestataire
        String imagePath = provider.getPicture();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                modelImage = Image.createImage(imagePath);
            } catch (Exception e) {
                modelImage = Image.createImage("theme://images/provider_placeholder.png");
            }
        } else {
            modelImage = Image.createImage("theme://images/provider_placeholder.png");
        }
        
        imageLabel = new ScaleImageLabel(modelImage);
        imageLabel.setUIID("ProviderImage");
        imageLabel.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        add(imageLabel);

        // Conteneur d'informations
        Container infoContainer = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Label nameLabel = new Label(provider.getName(), "ProviderName");
        Label categoryLabel = new Label(provider.getCategory(), "ProviderCategory");
        Label ratingLabel = new Label("★ " + provider.getRating(), "ProviderRating");
        Label distanceLabel = new Label(provider.getDistance() + " km", "ProviderDistance");

        infoContainer.addAll(nameLabel, categoryLabel, ratingLabel, distanceLabel);

        // Ajout des composants
        addAll(restInfo, infoContainer);
    }
}
