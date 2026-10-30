package com.lsoftware.spaco.usecase.catalog;

import java.text.Normalizer;
import java.util.Locale;

/** URL names for treatments and categories: "Exfoliación de miel" -> "exfoliacion-de-miel". */
public final class Slugs {

    private Slugs() {}

    public static String of(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-$", "") : slug;
    }
}
