package com.codename1.demos.grub.services;

import com.codename1.ui.util.Resources;

/**
 * Classe utilitaire pour gérer les ressources de l'application
 */
public class ResourceServiceImpl implements ResourceService {
    private Resources resources;

    public ResourceServiceImpl() {
        this.resources = null;
    }

    public ResourceServiceImpl(Resources resources) {
        this.resources = resources;
    }

    /**
     * Obtient les ressources de l'application
     * @return les ressources de l'application
     */
    @Override
    public Resources getApplicationResources() {
        return resources;
    }

    /**
     * Définit les ressources de l'application
     * @param resources les ressources à définir
     */
    @Override
    public void setApplicationResources(Resources resources) {
        this.resources = resources;
    }

    /**
     * Obtient les ressources globales de l'application
     * @return les ressources globales
     */
    @Override
    public Resources getGlobalResources() {
        return resources;
    }
}
