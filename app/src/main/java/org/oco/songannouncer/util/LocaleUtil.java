package org.oco.songannouncer.util;

import java.util.Locale;

public class LocaleUtil {

    public static String getLocaleCode(Locale l) {
        String code = l.getLanguage();
        String country = l.getCountry();
        return code + (country.isEmpty() ? "" : "-" + country);
    }

    public static String getLocaleName(Locale l) {
        String name = l.getDisplayName().substring(0, 1).toUpperCase() + l.getDisplayName().substring(1);
        return name + " (" + getLocaleCode(l) + ")";
    }

}
