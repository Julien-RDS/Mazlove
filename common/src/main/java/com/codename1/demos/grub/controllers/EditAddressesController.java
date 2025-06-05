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

import com.codename1.demos.grub.models.AccountModel;
import com.codename1.demos.grub.models.AddressModel;
import com.codename1.demos.grub.views.EditAddressView;
import com.codename1.demos.grub.views.EditAddressesView;
import com.codename1.demos.grub.views.NewAddressView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;

public class EditAddressesController {

    public static final ActionNode addAddress = UI.action();
    public static final ActionNode editAddress = UI.action();
    public static final ActionNode finishedEditing = UI.action();
    public static final ActionNode finishedNewAddress = UI.action();

    private String userId;
    private Resources globalResources;

    public EditAddressesController(Form parentForm, String userId, Resources globalResources) {
        this.userId = userId;
        this.globalResources = globalResources;

        Form addressesForm = new Form(new BorderLayout());
        addressesForm.getToolbar().hideToolbar();

        EditAddressesView editAddressesView = new EditAddressesView(userId, globalResources);
        addressesForm.add(BorderLayout.CENTER, editAddressesView);

        addressesForm.show();

        addActionListener(addAddress, evt -> {
            evt.consume();
            new NewAddressController(this).getView().show();
        });

        addActionListener(finishedNewAddress, evt -> {
            evt.consume();
            Entity address = evt.getEntity();
            account.addAddress(address);
            view.update();
            view.revalidateWithAnimationSafety();
            if(parent instanceof OrderController){
                ((OrderController) parent).updateOrderView();
            } else if(parent instanceof MainWindowController){
                ((MainWindowController) parent).updateDefaultAddressView();
            }
            showBack();
        });

        addActionListener(editAddress, evt -> {
            evt.consume();
            AddressModel address = (AddressModel)evt.getEntity();
            new EditAddressController(this, address, viewNode).getView().show();
        });

        addActionListener(finishedEditing, evt -> {
            evt.consume();
            account.checkValidation(evt.getEntity());
            view.update();
            view.revalidateWithAnimationSafety();
            if(parent instanceof OrderController){
                ((OrderController) parent).updateOrderView();
            }else if(parent instanceof MainWindowController){
                ((MainWindowController) parent).updateDefaultAddressView();
            }
            showBack();
        });
    }
}
