package com.mazlove.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PerformanceLogger {
    private static final Logger logger = LoggerFactory.getLogger("com.mazlove.performance");

    public void logRequest(String endpoint, String method, long durationMs) {
        logger.info("{} {} completed in {}ms", method, endpoint, durationMs);
    }

    public void logError(String endpoint, String method, Throwable error) {
        logger.error("Error in {} {}: {}", method, endpoint, error.getMessage());
    }

    public void logDatabase(String query, long durationMs) {
        logger.info("Database query completed in {}ms", durationMs);
    }

    public void logCacheHit(String key) {
        logger.info("Cache hit for key: {}", key);
    }

    public void logCacheMiss(String key) {
        logger.info("Cache miss for key: {}", key);
    }
}
