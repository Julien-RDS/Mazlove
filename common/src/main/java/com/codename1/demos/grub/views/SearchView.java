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


package com.codename1.demos.grub.views;

import com.codename1.demos.grub.interfaces.MainWindow;
import com.codename1.demos.grub.models.FilterModel;
import com.codename1.demos.grub.views.ModelPreviewNew;
import com.codename1.demos.grub.interfaces.ICuddleProvider;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.Resources;
import com.codename1.demos.grub.services.ResourceService;
import com.codename1.demos.grub.services.ResourceServiceImpl;
import java.util.ArrayList;
import java.util.List;

import static com.codename1.ui.CN.convertToPixels;
import static com.codename1.ui.util.Resources.getGlobalResources;

public class SearchView extends Container {
    private Resources globalResources;
    private Container rests;
    private List<ModelPreviewNew> modelPreviews;
    private String currentSearchText = "";
    private List<ICuddleProvider> allModels;
    private TextField searchField;

    public SearchView(Resources globalResources) {
        this.globalResources = globalResources;
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        setScrollableY(true);
        setScrollVisible(false);
        setUIID("SearchView");
        
        // Initialiser la liste des modèles
        allModels = new ArrayList<>();
        modelPreviews = new ArrayList<>();
        
        // Créer la barre de recherche
        searchField = new TextField() {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.setWidth(Display.getInstance().getDisplayWidth() / 4 * 3);
                return dim;
            }
        };
        searchField.setUIID("HomeSearchField");
        searchField.setHint("Search ...");
        searchField.getHintLabel().setUIID("HomeSearchFieldHint");
        searchField.addActionListener(e -> {
            String text = searchField.getText();
            if (!text.equals(currentSearchText)) {
                currentSearchText = text;
                filterModels(text);
            }
        });
        
        // Créer le bouton de retour
        Button backButton = new Button(FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
        backButton.setUIID("SearchBackButton");
        backButton.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter la navigation arrière
        });

        // Créer le label du titre
        Label headerLabel = new Label("MODELS", "SearchHeaderLabel");
        Container headerCnt = BorderLayout.centerCenter(headerLabel).add(BorderLayout.WEST, backButton);
        headerCnt.setUIID("SearchHeaderCnt");
        add(headerCnt);

        // Créer le bouton de filtre
        Image filterButtonImage = globalResources.getImage("filter-button.png").scaled(convertToPixels(5), convertToPixels(5));
        Button filterButton = new Button(filterButtonImage, "HomeFilterButton") {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(convertToPixels(8), convertToPixels(8));
            }
        };
        filterButton.addActionListener(evt -> {
            evt.consume();
            // TODO: Implémenter l'action du filtre
        });

        // Créer le conteneur de filtre
        Container filterCnt = new Container(new FlowLayout(Component.CENTER)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.setHeight(searchField.getPreferredH() + getAllStyles().getVerticalPadding());
                return dim;
            }
        };
        filterCnt.addAll(searchField, filterButton);
        filterCnt.setUIID("SearchFilterCnt");
        add(filterCnt);

        // Créer le label de catégorie
        Label categoryHeaderLabel = new Label("All Models", "CategoryHeader");
        add(categoryHeaderLabel);

        // Créer le conteneur pour les modèles
        rests = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        add(rests);
    public void updateModels(List<ICuddleProvider> models) {
        // Supprimer les previews existantes
        if (modelPreviews != null) {
            for (ModelPreviewNew preview : modelPreviews) {
                rests.remove(preview);
            }
            modelPreviews.clear();
        }

        // Créer les nouvelles previews
        modelPreviews = new ArrayList<>();
        for (ICuddleProvider model : models) {
            ModelPreviewNew preview = new ModelPreviewNew(model, globalResources);
            modelPreviews.add(preview);
            rests.add(preview);
        }

        // Revalider la vue
        revalidate();
    }

    private void filterModels(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            updateModels(allModels);
            return;
        }

        List<ICuddleProvider> filtered = new ArrayList<>();
        for (ICuddleProvider model : allModels) {
            if (model.getName().toLowerCase().contains(searchText.toLowerCase())) {
                filtered.add(model);
            }
        }
        updateModels(filtered);
    }

    public void setModels(List<ICuddleProvider> models) {
        allModels = models;
        updateModels(models);
    }

    public void setSearchField(TextField field) {
        searchField = field;
    }
}
