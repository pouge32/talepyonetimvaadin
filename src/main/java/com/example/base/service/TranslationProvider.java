package com.example.base.service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.vaadin.flow.i18n.I18NProvider;

@Component
public class TranslationProvider implements I18NProvider {

    public static final String BUNDLE_PREFIX = "messages";
    public static final Locale LOCALE_TR = new Locale("tr", "TR");
    public static final Locale LOCALE_EN = new Locale("en", "US");

    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(LOCALE_TR, LOCALE_EN);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            return "";
        }
        
        Locale targetLocale = locale != null ? locale : LOCALE_TR;

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, targetLocale);
            String value = bundle.getString(key);
            
            if (params.length > 0) {
                return MessageFormat.format(value, params);
            }
            return value;
        } catch (MissingResourceException e) {
            return "!" + key + "!"; 
        }
    }
}