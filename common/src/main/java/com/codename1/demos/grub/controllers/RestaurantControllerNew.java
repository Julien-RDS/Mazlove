package com.codename1.demos.grub.controllers;

import com.codename1.demos.grub.interfaces.CuddleProvider;
import com.codename1.demos.grub.views.DishPreview;
import com.codename1.demos.grub.views.DishView;
import com.codename1.demos.grub.views.RestaurantViewNew;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;

public class RestaurantControllerNew {
    private Form currentForm;
    private CuddleProvider provider;
    private Resources globalResources;

    public RestaurantControllerNew(Form parentForm, CuddleProvider provider, Resources globalResources) {
        this.provider = provider;
        this.globalResources = globalResources;

        // Création du formulaire
        currentForm = new Form(new BorderLayout());
        currentForm.getToolbar().hideToolbar();

        // Ajout de la vue du restaurant
        RestaurantViewNew view = new RestaurantViewNew(provider, globalResources);
        currentForm.add(BorderLayout.CENTER, view);
        currentForm.show();

        // Configuration des actions
        view.setDishAction(dish -> {
            // TODO: Implémenter l'action pour afficher le plat
            Form dishForm = new Form(new BorderLayout());
            dishForm.getToolbar().hideToolbar();
            DishView dishView = new DishView(dish, globalResources);
            dishForm.add(BorderLayout.CENTER, dishView);
            dishForm.show();
        });

        view.setOrderAction(() -> {
            // TODO: Implémenter l'action pour afficher la commande
            Form orderForm = new Form(new BorderLayout());
            orderForm.getToolbar().hideToolbar();
            // TODO: Ajouter la vue de la commande
            orderForm.show();
        });

        view.setAddToCartAction((dish, quantity) -> {
            // TODO: Implémenter l'ajout au panier
            System.out.println("Ajout au panier: " + dish.getName() + " x" + quantity);
        });
    }

    public Form getCurrentForm() {
        return currentForm;
    }
}
