package com.codename1.demos.grub.services;

import com.codename1.ui.Form;
import com.codename1.ui.Container;

public interface NavigationService {
    void initialize(Form homeForm);
    void showForm(Form form);
    void showBack();
    void showContainer(Container container);
    void showContainerBack(Container container);
    void showHome();
    Form getCurrentForm();
}
