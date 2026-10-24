package com.mazlove.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Cache applicatif dans Redis (le même serveur que les codes OTP).
 * Connexion configurée par spring.data.redis.* dans application.properties.
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /** Durée de vie d'une entrée ; accepte "600000" (ms) comme "10m". */
    @Value("${spring.cache.redis.time-to-live:10m}")
    private Duration cacheTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTtl)
                .disableCachingNullValues()
                .prefixCacheNameWith("mazlove:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // Les évictions attendent le commit de la transaction en cours (cf. CacheService).
                .transactionAware()
                .build();
    }

    /**
     * JSON lisible dans redis-cli, avec le nom de classe pour pouvoir relire les listes.
     * - JavaTimeModule : sans lui, les LocalDateTime des avis font échouer la sérialisation.
     * - Validateur de types restreint à nos DTO et aux types JDK courants : on ne laisse jamais
     *   Jackson instancier une classe arbitraire lue dans Redis (faille de désérialisation connue).
     */
    private static GenericJackson2JsonRedisSerializer jsonSerializer() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.mazlove.backend.dto.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Un champ ajouté/supprimé dans un DTO ne doit pas casser la lecture d'un cache existant.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Si Redis tombe ou qu'une entrée est illisible, on journalise et on interroge la base
     * comme s'il n'y avait pas de cache : la liste des praticiens reste disponible.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Lecture du cache {} impossible (clé {}) : {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Écriture du cache {} impossible (clé {}) : {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Éviction du cache {} impossible (clé {}) : {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Vidage du cache {} impossible : {}", cache.getName(), e.getMessage());
            }
        };
    }
}
