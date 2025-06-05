package com.codename1.demos.grub.services;

import com.codename1.ui.util.Resources;

public interface ResourceService {
    Resources getApplicationResources();
    void setApplicationResources(Resources resources);
    Resources getGlobalResources();
}
