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


package com.mazlove.app;

import com.codename1.demos.grub.controllers.AccountController;
import com.codename1.demos.grub.controllers.IntroductionController;
import com.codename1.demos.grub.controllers.MainWindowController;
import com.codename1.demos.grub.controllers.SetFirstLocationController;
import com.codename1.demos.grub.interfaces.Account;
import com.mazlove.app.models.*;
import com.codename1.demos.grub.views.*;
import com.codename1.io.Log;
import com.codename1.rad.controllers.ApplicationController;
import com.codename1.rad.controllers.ControllerEvent;
import com.codename1.rad.models.Entity;
import com.codename1.rad.nodes.ActionNode;
import com.codename1.rad.nodes.ViewNode;
import com.codename1.rad.ui.UI;
import com.codename1.ui.*;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.codename1.ui.CN.*;
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
public class Mazlove extends ApplicationController {
    Entity account;
    private static boolean darkMode = false;
    private Resources theme;

    public static final ActionNode enterMainWindow = UI.action();
    public static final ActionNode enterIntroduction = UI.action();
    public static final ActionNode enterSetLocation = UI.action();
    public static final ActionNode logout = UI.action();
    public static final ActionNode darkModeActionNode = UI.action();

    public static final ActionNode.Category SKIP_TO_MAIN_WINDOW = new ActionNode.Category();

    public static final int DEBUG_MODE_WITH_SIGNING = 0;
    public static final int DEBUG_MODE_WITHOUT_SIGNING = 1;
    public static final int DEBUG_MODE_WITH_SIGNING_DARK_MODE = 2;
    public static final int DEBUG_MODE_WITHOUT_SIGNING_DARK_MODE = 3;

    public static final int mode = DEBUG_MODE_WITH_SIGNING;

    @Override
    public void actionPerformed(ControllerEvent evt) {
        if (evt instanceof StartEvent) {
            evt.consume();

            ViewNode viewNode = new ViewNode(
                    UI.actions(SignInView.COMPLETE_SIGNING_IN, enterIntroduction),
                    UI.actions(ThirdIntroductionView.FINISHED_THIRD_INTRO, enterSetLocation),
                    UI.actions(SetFirstLocationView.COMPLETE_SETTING_ADDRESS, enterMainWindow),
                    UI.actions(ProfileView.LOG_OUT, logout),
                    UI.actions(AccountView.DARK_MODE, darkModeActionNode),
                    UI.actions(SKIP_TO_MAIN_WINDOW, enterMainWindow)
            );

            if (mode == DEBUG_MODE_WITH_SIGNING || mode == DEBUG_MODE_WITH_SIGNING_DARK_MODE){

                if (mode == DEBUG_MODE_WITH_SIGNING_DARK_MODE){
                    darkMode = true;
                    initTheme();
                }

                account = new AccountModel();
                new AccountController(this, account, viewNode).getView().show();

                addActionListener(logout, event-> {
                    account = new AccountModel();
                    new AccountController(this, account, viewNode).getView().showBack();
                });

                addActionListener(enterIntroduction, event->{
                    event.consume();
                    new IntroductionController(this, account, viewNode).getView().show();
                });


                addActionListener(enterSetLocation, event -> {
                    event.consume();
                    new SetFirstLocationController(this, account, viewNode).getView().show();
                });

                addActionListener(enterMainWindow, event->{
                    event.consume();
                    new MainWindowController(this, createDemoMainWindowEntity(account), viewNode).getView().show();
                });

                addActionListener(darkModeActionNode, event->{
                    event.consume();
                    darkMode = !darkMode;
                    initTheme();
                });
            }else if (mode == DEBUG_MODE_WITHOUT_SIGNING){
                new MainWindowController(this, createDemoMainWindowEntity(null), viewNode).getView().show();

            }else if(mode == DEBUG_MODE_WITHOUT_SIGNING_DARK_MODE){
                darkMode = true;
                initTheme();
                new MainWindowController(this, createDemoMainWindowEntity(null), viewNode).getView().show();
            }
        }
    }

    private Entity createDemoMainWindowEntity(Entity accountEntity){
        Entity account;
        if (accountEntity == null){
             account = new AccountModel();
            account.set(Account.firstName, "Codename");
            account.set(Account.lastName, "One");
            account.set(Account.emailAddress, "sergey@gmail.com");
            account.set(Account.password, "sd12eqwf134qsd");
            account.set(Account.mobileNumber, "0544123415");
        }else{
            account = accountEntity;
        }
        List providersList = new ArrayList<>();
        for (int i = 0; i < 10; i++){
            providersList.add(createProviderDemoModel());
        }
        return new MainWindowModel(account, providersList);
    }

    private Entity createProviderDemoModel(){
        Provider provider = new Provider();
        provider.setId("1");
        provider.setName("Sergio's Services");
        provider.setDescription("Professional services provider");
        provider.setRating(4.7f);
        provider.setVerified(true);
        List<TimeSlot> availability = new ArrayList<>();
        availability.add(new TimeSlot("09:00", "12:00"));
        availability.add(new TimeSlot("14:00", "18:00"));
        provider.setAvailability(availability);
        return provider;
    }

    private List<Entity> createServiceItems(){
        List<Entity> services = new ArrayList<>();

        services.add(new ServiceItem("Cleaning", "Deep cleaning service", 50.0f));
        services.add(new ServiceItem("Gardening", "Garden maintenance", 30.0f));
        services.add(new ServiceItem("Handyman", "General repairs", 45.0f));
        services.add(new ServiceItem("Moving", "Furniture moving assistance", 60.0f));

        return services;
    }

    private List<Entity> createPastaMenu(){
        List<Entity> dishes = new ArrayList<>();
        dishes.add(new DishModel("Ravioli", "Ravioli pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 4, createDemoAddOns()));
        dishes.add(new DishModel("Fettuccine", "Fettuccine pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 5, createDemoAddOns()));
        dishes.add(new DishModel("Linguini", "Linguini pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 2, createDemoAddOns()));
        dishes.add(new DishModel("Rotelle", "Rotelle pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 3, createDemoAddOns()));
        dishes.add(new DishModel("Ditalini", "Ditalini pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 4.5, createDemoAddOns()));
        dishes.add(new DishModel("Tortellini", "Tortellini pasta", "https://ju-rule.github.io/mazlove-images/pasta.jpg", 6.4, createDemoAddOns()));

        return dishes;
    }

    private List<Entity> createDrinksMenu(){
        List<Entity> dishes = new ArrayList<>();
        dishes.add(new DishModel("Cola", "Cola with ice", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Soda", "Soda with ice", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Orange Juice", "Orange Juice with ice", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Apple juice", "Apple juice with ice", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Vanilla Milkshake", "Vanilla Milkshake with cream", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Chocolate Milkshake", "Chocolate Milkshake with cream", "https://ju-rule.github.io/mazlove-images/drink.jpg", 4.60, createDemoAddOns()));

        return dishes;
    }

    private List<Entity> createDessertsMenu(){
        List<Entity> dishes = new ArrayList<>();
        dishes.add(new DishModel("Apple Pie", "Apple Pie with ice-cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Chocolate Cake", "Chocolate Cake with ice-cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Pancakes", "Pancakes with ice-cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Cupcakes", "Cupcakes with ice-cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Vanilla Ice Cream", "Vanilla Ice Cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Chocolate Ice Cream", "Chocolate Ice Cream", "https://ju-rule.github.io/mazlove-images/dessert.jpg", 4.60, createDemoAddOns()));

        return dishes;
    }

    private List<Entity> createPizzaMenu(){
        List<Entity> dishes = new ArrayList<>();
        dishes.add(new DishModel("Neapolitan Pizza", "Large pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Chicago Pizza", "Medium pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("New York-Style Pizza", "Small pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Sicilian Pizza", "Medium pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("Greek Pizza", "Small pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));
        dishes.add(new DishModel("California Pizza", "Medium pizza", "https://ju-rule.github.io/mazlove-images/pizza.jpg", 4.60, createDemoAddOns()));

        return dishes;
    }


    private List<Entity> createDemoAddOns(){
        List addOns = new ArrayList();

        addOns.add(new DishAddOnModel("mushrooms", "https://ju-rule.github.io/mazlove-images/mushrooms.jpg", 4.0));
        addOns.add(new DishAddOnModel("tomato", "https://ju-rule.github.io/mazlove-images/tomato.jpg", 2.0));
        addOns.add(new DishAddOnModel("cheese", "https://ju-rule.github.io/mazlove-images/cheese.jpg", 3.0));
        addOns.add(new DishAddOnModel("onion", "https://ju-rule.github.io/mazlove-images/onion.jpg", 1.0));

        return addOns;
    }

    private void initTheme() {
        String themeFileName = darkMode ? "/dark-theme" : "/theme";
        try {
            Resources theme = Resources.openLayered(themeFileName);
            UIManager.getInstance().addThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        }catch(IOException e){
            Log.e(e);
        }
        Form currForm = Display.getInstance().getCurrent();
        if (currForm != null) {
            currForm.refreshTheme();
        }
    }

    @Override
    public void init(Object context) {
        CN.setProperty("Component.revalidateOnStyleChange", "false");
        updateNetworkThreadCount(2);

        try {
            theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
            Resources.setGlobalResources(theme);
        }catch(IOException e){
            Log.e(e);
        }

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });
        dispatchEvent(new InitEvent(context));
    }

    public static boolean isDarkMode(){
        return darkMode;
    }

    @Override
    public void stop() {
        current = getCurrentForm();
        dispatchEvent(new StopEvent());
        System.out.println(current);
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }

    @Override
    public void start() {
        showCurrentForm();
        if (current == null){
            dispatchEvent(new StartEvent());
        }
    }

    @Override
    protected void showCurrentForm() {
        if (current != null) {
            current.show();
        }
    }
    
}

