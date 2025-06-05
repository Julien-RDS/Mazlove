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

import com.codename1.demos.grub.views.EditProfileView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;

public class EditProfileController {
    private String userId;
    private Resources globalResources;

    public EditProfileController(String userId, Resources globalResources) {
        this.userId = userId;
        this.globalResources = globalResources;

        Form editProfileForm = new Form(new BorderLayout());
        editProfileForm.getToolbar().hideToolbar();

        EditProfileView editProfileView = new EditProfileView(userId, globalResources);
        editProfileForm.add(BorderLayout.CENTER, editProfileView);

        editProfileForm.show();
    }

    public void show() {
        // La méthode show() est déjà appelée dans le constructeur
    }
}
