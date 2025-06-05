package com.codename1.demos.grub.services;

import com.codename1.ui.Form;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

public class NavigationServiceImpl implements NavigationService {
    private Form currentForm;
    private Form homeForm;
    private Form previousForm;

    public void initialize(Form homeForm) {
        this.homeForm = homeForm;
        this.currentForm = homeForm;
    }

    @Override
    public void showForm(Form form) {
        previousForm = currentForm;
        currentForm = form;
        form.show();
    }

    @Override
    public void showBack() {
        if (previousForm != null) {
            Form temp = previousForm;
            previousForm = currentForm;
            currentForm = temp;
            currentForm.showBack();
        }
    }

    @Override
    public void showContainer(Container container) {
        if (currentForm != null) {
            currentForm.removeAll();
            currentForm.add(container);
            currentForm.revalidate();
        }
    }

    @Override
    public void showContainerBack(Container container) {
        if (currentForm != null) {
            currentForm.removeAll();
            currentForm.add(container);
            currentForm.revalidate();
            currentForm.showBack();
        }
    }

    @Override
    public void showHome() {
        if (homeForm != null) {
            currentForm = homeForm;
            homeForm.show();
        }
    }

    @Override
    public Form getCurrentForm() {
        return currentForm;
    }

    public void addBackListener(Form form, ActionListener<ActionEvent> listener) {
        if (form != null) {
            form.getToolbar().addCommandToSideMenu("", null, listener);
        }
    }
}
