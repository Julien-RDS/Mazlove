package com.mazlove.backend.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Accès aux libellés traduits de i18n/messages_{fr,en}.properties.
 * La langue vient de la requête en cours (cookie "mazlove_locale" ou paramètre ?lang=en,
 * cf. I18nConfig) ; le français est la langue par défaut.
 */
@Component
public class MessageUtil {

    private final MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /** @param args valeurs injectées dans les {0}, {1}… du libellé */
    public String getMessage(String key, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }
}
