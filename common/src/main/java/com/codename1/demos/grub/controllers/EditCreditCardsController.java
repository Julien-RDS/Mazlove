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
import com.codename1.demos.grub.models.CreditCardModel;
import com.codename1.demos.grub.views.AddCreditCardView;
import com.codename1.demos.grub.views.EditCreditCardsView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;

public class EditCreditCardsController {
    private String userId;
    private Resources globalResources;

    public EditCreditCardsController(Form parentForm, String userId, Resources globalResources) {
        this.userId = userId;
        this.globalResources = globalResources;

        Form editCreditCardsForm = new Form(new BorderLayout());
        editCreditCardsForm.getToolbar().hideToolbar();

        EditCreditCardsView editCreditCardsView = new EditCreditCardsView(userId, globalResources);
        editCreditCardsForm.add(BorderLayout.CENTER, editCreditCardsView);

        editCreditCardsForm.show();
            CreditCardModel card = (CreditCardModel)evt.getEntity();
            account.addCreditCard(card);
            view.update();
            showBack();
        });
    }
}
