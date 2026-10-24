package com.mazlove.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Journal dédié aux mesures de performance, écrit sous le logger "com.mazlove.performance"
 * (routé vers un fichier à part en production, cf. logback-prod.xml) pour pouvoir
 * l'analyser sans le bruit applicatif.
 */
@Component
public class PerformanceLogger {

    private static final Logger logger = LoggerFactory.getLogger("com.mazlove.performance");

    public void logRequest(String endpoint, String method, long durationMs) {
        logger.info("{} {} traité en {} ms", method, endpoint, durationMs);
    }

    public void logError(String endpoint, String method, Throwable error) {
        logger.error("Erreur sur {} {} : {}", method, endpoint, error.getMessage());
    }

    /** La requête SQL n'est volontairement pas loguée : elle peut contenir des données personnelles. */
    public void logDatabase(String query, long durationMs) {
        logger.info("Requête SQL exécutée en {} ms", durationMs);
    }

    public void logCacheHit(String key) {
        logger.info("Cache trouvé pour la clé {}", key);
    }

    public void logCacheMiss(String key) {
        logger.info("Cache absent pour la clé {}", key);
    }
}
