package com.mazlove.app.views;

import com.codename1.rad.nodes.ViewNode;
import com.codename1.rad.ui.AbstractView;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.mazlove.app.models.Provider;
import com.mazlove.app.models.ServiceItem;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.List;

public class MainWindowView extends AbstractView {
    private Resources theme;
    private Form current;

    public MainWindowView(ViewNode node) {
        super(node);
        theme = UIManager.getInstance().getThemeProps().getLookup().createResources();
        current = new Form("Mazlove", new BorderLayout());
        current.setScrollable(false);
        current.setScrollableY(true);

        // Header
        Container header = new Container(new BorderLayout());
        header.setUIID("Header");
        
        Label logo = new Label(theme.getImage("mazlove_logo.png"));
        logo.setUIID("Logo");
        
        Label location = new Label("Current Location");
        location.setUIID("Location");
        
        header.add(BorderLayout.WEST, logo);
        header.add(BorderLayout.CENTER, location);
        current.add(BorderLayout.NORTH, header);

        // Services Grid
        Container servicesGrid = new Container(new GridLayout(3, 2));
        servicesGrid.setUIID("ServicesGrid");
        
        List<ServiceItem> services = (List<ServiceItem>) node.getEntity().get("services");
        for (ServiceItem service : services) {
            Container serviceCard = createServiceCard(service);
            servicesGrid.add(serviceCard);
        }
        
        current.add(BorderLayout.CENTER, servicesGrid);
    }

    private Container createServiceCard(ServiceItem service) {
        Container card = new Container(new BorderLayout());
        card.setUIID("ServiceCard");

        Label icon = new Label(theme.getImage("service_icons/" + service.getName().toLowerCase() + ".png"));
        icon.setUIID("ServiceIcon");

        Container info = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        info.setUIID("ServiceInfo");

        Label name = new Label(service.getName());
        name.setUIID("ServiceName");

        Label price = new Label("$" + String.format("%.2f", service.getPrice()));
        price.setUIID("ServicePrice");

        info.addAll(name, price);

        card.add(BorderLayout.NORTH, icon);
        card.add(BorderLayout.CENTER, info);

        return card;
    }

    @Override
    public Component getComponent() {
        return current;
    }
}
