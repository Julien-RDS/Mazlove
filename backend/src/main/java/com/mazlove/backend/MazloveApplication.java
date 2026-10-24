package com.mazlove.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Mazlove (port 3001 par défaut, cf. application.properties).
 * Prérequis au démarrage : PostgreSQL (le schéma est créé automatiquement) et Redis.
 */
@SpringBootApplication
public class MazloveApplication {
    public static void main(String[] args) {
        SpringApplication.run(MazloveApplication.class, args);
    }
}
