/**
 * Licensed to Codename One LTD under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Codename One licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.codename1.demos.grub.controllers;

import com.codename1.ui.Form;
import com.codename1.demos.grub.services.NavigationService;
import com.codename1.demos.grub.services.NavigationServiceImpl;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;
import com.codename1.demos.grub.services.ResourceService;
import com.codename1.demos.grub.services.ResourceServiceImpl;
import com.codename1.demos.grub.views.MainWindowView;
import com.codename1.demos.grub.interfaces.CuddleProvider;
import com.codename1.demos.grub.models.Order;
import com.codename1.demos.grub.controllers.RestaurantControllerNew;
import com.codename1.demos.grub.controllers.FilterController;
import com.codename1.demos.grub.controllers.SearchController;
import com.codename1.demos.grub.controllers.EditProfileController;
import com.codename1.demos.grub.controllers.EditAddressesController;
import com.codename1.demos.grub.controllers.EditCreditCardsController;

public class MainWindowController {
    private final Form mainForm;
    private final ResourceService resourceService;
    private MainWindowView mainWindowView;
    private NavigationService navigationService;

    public MainWindowController(Form mainForm, ResourceService resourceService) {
        this.mainForm = mainForm;
        this.resourceService = resourceService;
        initialize();
    }

    public void initialize() {
        navigationService = new NavigationServiceImpl();
        navigationService.initialize(mainForm);

        mainWindowView = new MainWindowView(resourceService.getGlobalResources());

        mainWindowView.setProviderAction(provider -> {
            new RestaurantControllerNew(mainForm, provider, resourceService.getGlobalResources());
        });
        mainWindowView.setFilterAction(() -> {
            new FilterController(mainForm, resourceService.getGlobalResources());
        });
        mainWindowView.setSearchAction(() -> {
            new SearchController(mainForm, resourceService.getGlobalResources());
        });
        mainWindowView.setAddFavoriteAction(provider -> {
            mainWindowView.toggleFavorite(provider);
        });
        mainWindowView.setEditProfileAction(() -> {
            new EditProfileController("", resourceService.getGlobalResources());
        });
        mainWindowView.setEditAddressesAction(() -> {
            new EditAddressesController(mainForm, "", resourceService.getGlobalResources());
        });
        mainWindowView.setEditCreditCardsAction(() -> {
            new EditCreditCardsController(mainForm, "", resourceService.getGlobalResources());
        });
        mainWindowView.setAddCompletedOrderAction(order -> {
            // TODO: Implémenter AddCompletedOrderController
        });
        mainWindowView.setOrderAgainAction(order -> {
            // TODO: Implémenter OrderAgainController
        });

        // Ajout de la vue au formulaire parent
        mainForm.add(mainWindowView);
        mainForm.show();

        // Navigation initiale
        mainWindowView.showProviderTab();
    }

    public void updateDefaultAddressView() {
        // TODO: Implémenter la mise à jour de l'adresse par défaut
    }

    public void showProvider(String providerId) {
        // TODO: Implémenter l'affichage du prestataire
    }
}
