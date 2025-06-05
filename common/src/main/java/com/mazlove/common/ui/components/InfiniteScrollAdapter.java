package com.mazlove.common.ui.components;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.InfiniteContainer;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIID;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;

public class InfiniteScrollAdapter extends Container {
    private final InfiniteContainer infiniteContainer;
    private final EventDispatcher dataLoaded = new EventDispatcher();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private final Resources theme;

    public InfiniteScrollAdapter(Resources theme) {
        super(BoxLayout.y());
        this.theme = theme;
        infiniteContainer = new InfiniteContainer(() -> {
            if (isLoading || !hasMoreData) {
                return null;
            }
            isLoading = true;
            
            // Simuler le chargement des données
            try {
                Thread.sleep(1000); // Simuler le délai de chargement
                // Ici, vous devriez appeler votre API avec currentPage
                // Pour le moment, on retourne des données de test
                Component[] items = new Component[5];
                for (int i = 0; i < items.length; i++) {
                    Container card = new Container(BoxLayout.y());
                    card.setUIID("ProviderCard");
                    card.getAllStyles().setMargin(5);
                    card.getAllStyles().setPadding(10);
                    
                    // Ajouter une image de placeholder
                    card.add(theme.getImage("placeholder.png"));
                    
                    // Ajouter le nom et la description
                    Container info = new Container(BoxLayout.y());
                    info.setUIID("ProviderInfo");
                    info.add(theme.getImage("star.png").scaled(20, 20));
                    info.add(theme.getImage("location.png").scaled(20, 20));
                    card.add(info);
                    
                    items[i] = card;
                }
                
                currentPage++;
                isLoading = false;
                return items;
            } catch (Exception e) {
                isLoading = false;
                return null;
            }
        });
        
        // Ajouter une barre de chargement
        Container loadingBar = new Container(new FlowLayout(Component.CENTER));
        loadingBar.setUIID("LoadingBar");
        loadingBar.add(theme.getImage("loading.gif").scaled(30, 30));
        
        add(infiniteContainer);
        add(loadingBar);
    }

    public void reset() {
        currentPage = 0;
        hasMoreData = true;
        infiniteContainer.reset();
    }

    public void setHasMoreData(boolean hasMoreData) {
        this.hasMoreData = hasMoreData;
    }

    public void addDataLoadedListener(com.codename1.ui.events.ActionListener<ActionEvent> listener) {
        dataLoaded.addListener(listener);
    }

    public void removeDataLoadedListener(com.codename1.ui.events.ActionListener<ActionEvent> listener) {
        dataLoaded.removeListener(listener);
    }
}
