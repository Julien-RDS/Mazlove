package com.mazlove.app.views;

import com.codename1.rad.nodes.ViewNode;
import com.codename1.rad.ui.AbstractView;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.mazlove.app.models.Provider;
import com.mazlove.app.models.Session;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

public class SessionView extends AbstractView {
    private Resources theme;
    private Form current;

    public SessionView(ViewNode node) {
        super(node);
        theme = UIManager.getInstance().getThemeProps().getLookup().createResources();
        current = new Form("Schedule Service", new BorderLayout());
        current.setScrollable(false);
        current.setScrollableY(true);

        Provider provider = (Provider) node.getEntity().get("provider");
        Session session = (Session) node.getEntity().get("session");

        // Header
        Container header = new Container(new BorderLayout());
        header.setUIID("SessionHeader");

        Label providerName = new Label(provider.getName());
        providerName.setUIID("ProviderName");

        Label startTime = new Label("Start: " + session.getStartTime());
        startTime.setUIID("SessionTime");

        header.add(BorderLayout.WEST, providerName);
        header.add(BorderLayout.EAST, startTime);
        current.add(BorderLayout.NORTH, header);

        // Duration
        Container duration = new Container(new BoxLayout(BoxLayout.X_AXIS));
        duration.setUIID("Duration");

        Label durationLabel = new Label("Duration: " + session.getDuration() + " hours");
        durationLabel.setUIID("DurationLabel");

        duration.add(durationLabel);
        current.add(BorderLayout.CENTER, duration);

        // Confirm Button
        Button confirm = new Button("Confirm Appointment");
        confirm.setUIID("ConfirmButton");
        confirm.addActionListener(e -> {
            // TODO: Implement confirmation logic
        });

        current.add(BorderLayout.SOUTH, confirm);
    }

    @Override
    public Component getComponent() {
        return current;
    }
}
