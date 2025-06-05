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

import com.codename1.demos.grub.models.OrderDishModel;
import com.codename1.demos.grub.models.RestaurantModel;
import com.codename1.demos.grub.views.DishPreview;
import com.codename1.demos.grub.views.DishView;
import com.codename1.demos.grub.views.RestaurantView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;

public class RestaurantController {

    public static final ActionNode enterDish = UI.action();
    public static final ActionNode showOrder = UI.action();
    public static final ActionNode addToCart = UI.action();


    public RestaurantController(Form parentForm, RestaurantModel restaurant, AccountModel account, Resources globalResources) {
        Form form = new Form(new BorderLayout());
        form.getToolbar().hideToolbar();

        RestaurantView view = new RestaurantView(account, restaurant, globalResources);
        form.add(BorderLayout.CENTER, view);
        form.show();

        addActionListener(enterDish, evt->{
            evt.consume();
            Entity dish = evt.getEntity();
            new DishController(this, dish).getView().show();
        });

        addActionListener(showOrder, evt->{
            evt.consume();
            new OrderController(this, restaurant, account).getView().show();
        });

        addActionListener(addToCart, evt -> {
            evt.consume();
            if (restaurant instanceof RestaurantModel){
                ((RestaurantModel)(restaurant)).addToOrder(new OrderDishModel(evt.getEntity(), (int)(evt.getContext().getExtra("quantity"))));
            }
        });
    }
}
