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

public class ProviderView extends AbstractView {
    private Resources theme;
    private Form current;

    public ProviderView(ViewNode node) {
        super(node);
        theme = UIManager.getInstance().getThemeProps().getLookup().createResources();
        current = new Form("Provider Details", new BorderLayout());
        current.setScrollable(false);
        current.setScrollableY(true);

        Provider provider = (Provider) node.getEntity().get("provider");

        // Header
        Container header = new Container(new BorderLayout());
        header.setUIID("ProviderHeader");

        Label name = new Label(provider.getName());
        name.setUIID("ProviderName");

        Label rating = new Label("★ " + provider.getRating());
        rating.setUIID("ProviderRating");

        header.add(BorderLayout.WEST, name);
        header.add(BorderLayout.EAST, rating);
        current.add(BorderLayout.NORTH, header);

        // Description
        Label description = new Label(provider.getDescription());
        description.setUIID("ProviderDescription");
        current.add(BorderLayout.CENTER, description);

        // Availability
        Container availability = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        availability.setUIID("Availability");

        Label availabilityTitle = new Label("Availability");
        availabilityTitle.setUIID("AvailabilityTitle");
        availability.add(availabilityTitle);

        for (TimeSlot slot : provider.getAvailability()) {
            Label slotLabel = new Label(slot.getStart() + " - " + slot.getEnd());
            slotLabel.setUIID("AvailabilitySlot");
            availability.add(slotLabel);
        }

        current.add(BorderLayout.SOUTH, availability);
    }

    @Override
    public Component getComponent() {
        return current;
    }
}
