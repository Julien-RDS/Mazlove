package com.codename1.demos.grub.views;

import com.codename1.demos.grub.interfaces.MainWindow;
import com.codename1.demos.grub.interfaces.CuddleProvider;
import com.codename1.demos.grub.models.Order;
import com.codename1.demos.grub.services.FavoritesService;
import com.codename1.demos.grub.services.FavoritesServiceImpl;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.Resources;
import com.codename1.ui.Tabs;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vue principale de l'application
 */
public class MainWindowView extends Container {
    private Resources globalResources;
    private Container completedOrdersContainer;
    private Container providerContainer;
    private Container serviceContainer;
    private Container editProfileContainer;
    private Container editAddressesContainer;
    private Container editCreditCardsContainer;
    private String userId;
    private CuddleProvider currentProvider;
    private FavoritesService favoritesService;
    private Consumer<CuddleProvider> providerAction;
    private Consumer<Order> addCompletedOrderAction;
    private Consumer<Order> orderAgainAction;

    public MainWindowView(Resources globalResources) {
        this.globalResources = globalResources;
        favoritesService = FavoritesServiceImpl.getInstance();
        setLayout(new BorderLayout());
        initTabs();
    }

    private void initTabs() {
        Tabs tabs = new Tabs();
        tabs.setUIID("Tabs");
        tabs.setTabUIID("Tab");

        // Tab Home
        Container homeTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        homeTab.setUIID("Container");
        tabs.addTab("Home", homeTab);

        // Tab Profile
        Container profileTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        profileTab.setUIID("Container");
        tabs.addTab("Profile", profileTab);

        // Tab Recommendations
        Container recommendationsTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        recommendationsTab.setUIID("Container");
        tabs.addTab("Recommendations", recommendationsTab);

        // Tab Orders
        Container ordersTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        ordersTab.setUIID("Container");
        tabs.addTab("Orders", ordersTab);

        // Tab Addresses
        Container addressesTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        addressesTab.setUIID("Container");
        tabs.addTab("Addresses", addressesTab);

        // Tab Credit Cards
        Container creditCardsTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        creditCardsTab.setUIID("Container");
        tabs.addTab("Credit Cards", creditCardsTab);

        // Tab Favorites
        Container favoritesTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        favoritesTab.setUIID("Container");
        tabs.addTab("Favorites", favoritesTab);

        // Tab Search
        Container searchTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        searchTab.setUIID("Container");
        tabs.addTab("Search", searchTab);

        // Tab Filters
        Container filtersTab = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        filtersTab.setUIID("Container");
        tabs.addTab("Filters", filtersTab);

        add(BorderLayout.CENTER, tabs);
    }

    public void setProviderAction(Consumer<CuddleProvider> action) {
        this.providerAction = action;
    }

    public void setAddCompletedOrderAction(Consumer<Order> action) {
        this.addCompletedOrderAction = action;
    }

    public void setFilterAction(Runnable action) {
        // TODO: Implémenter l'action de filtre
    }

    public void setSearchAction(Runnable action) {
        // TODO: Implémenter l'action de recherche
    }

    public void setAddFavoriteAction(Consumer<CuddleProvider> action) {
        // TODO: Implémenter l'action d'ajout de favoris
    }

    public void setEditProfileAction(Runnable action) {
        // TODO: Implémenter l'action d'édition du profil
    }

    public void setEditAddressesAction(Runnable action) {
        // TODO: Implémenter l'action d'édition des adresses
    }

    public void setEditCreditCardsAction(Runnable action) {
        // TODO: Implémenter l'action d'édition des cartes de crédit
    }

    public void setOrderAgainAction(Consumer<Order> action) {
        this.orderAgainAction = action;
    }

    public void toggleFavorite(CuddleProvider provider) {
        if (favoritesService.isFavorite(provider)) {
            favoritesService.removeFavorite(provider);
        } else {
            favoritesService.addFavorite(provider);
        }
    }

    public void showProviderTab() {
        // TODO: Implémenter l'affichage du tab Provider
    }

    public void updateDefaultAddressView() {
        // TODO: Implémenter la mise à jour de l'adresse par défaut
    }

    public void removeFavorite(CuddleProvider provider) {
        favoritesService.removeFavorite(provider);
    }

    public void setProvider(CuddleProvider provider) {
        currentProvider = provider;
        showProviderTab();
    }

    public void triggerOrderAgain(Order order) {
        if (orderAgainAction != null) {
            orderAgainAction.accept(order);
        }
    }

    public Container getComponent() {
        return this;
    }
}
