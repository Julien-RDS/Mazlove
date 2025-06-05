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
package com.codename1.demos.grub.views;

import com.codename1.demos.grub.models.AddressModel;
import com.codename1.demos.grub.models.AccountModel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.util.Resources;

public class EditAddressesView {
    public static final String ADD_NEW_ADDRESS = "addNewAddress";
    public static final String EDIT_ADDRESS = "editAddress";

    private AccountModel account;
    private Container addressesContainer;
    private Container buttonsContainer;
    private Button addAddressButton;
    private Button backButton;
    private Resources globalResources;
    private Component component;

    public EditAddressesView(String userId, Resources globalResources) {
        this.globalResources = globalResources;

        Container root = new Container(new BorderLayout());
        root.setUIID("EditAddressesRoot");

        Container headerContainer = new Container(new BorderLayout());
        headerContainer.setUIID("EditAddressesHeader");

        backButton = new Button("Back");
        backButton.setUIID("EditAddressesBackButton");
        headerContainer.add(BorderLayout.WEST, backButton);

        Label headerLabel = new Label("Addresses");
        headerLabel.setUIID("EditAddressesHeaderLabel");
        headerContainer.add(BorderLayout.CENTER, headerLabel);

        root.add(BorderLayout.NORTH, headerContainer);

        addressesContainer = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        addressesContainer.setUIID("EditAddressesAddressesContainer");
        root.add(BorderLayout.CENTER, addressesContainer);

        buttonsContainer = new Container(new FlowLayout());
        buttonsContainer.setUIID("EditAddressesButtonsContainer");

        addAddressButton = new Button("Add New Address");
        addAddressButton.setUIID("EditAddressesAddAddressButton");
        buttonsContainer.add(addAddressButton);

        root.add(BorderLayout.SOUTH, buttonsContainer);

        setComponent(root);
        update();
    }

    public void update() {
        addressesContainer.removeAll();
        // TODO: Implémenter la logique pour récupérer les adresses de l'utilisateur
        addressesContainer.revalidate();
    }

    public Button getAddAddressButton() {
        return addAddressButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void setAddAddressButton(Button addAddressButton) {
        this.addAddressButton = addAddressButton;
    }

    public void setBackButton(Button backButton) {
        this.backButton = backButton;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}
