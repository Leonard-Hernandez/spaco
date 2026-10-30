package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Plain rules over the anamnesis. They remind; the professional decides. */
final class ClinicalAlerts {

    private static final Set<String> NEGATIVE = Set.of("no", "ninguno", "ninguna", "niega", "n/a", "na", "-", "ningun");
    private static final Set<String> STOPWORDS = Set.of("antes", "durante", "despues", "sobre", "entre", "desde",
            "hasta", "puede", "pueden", "deben", "evitar", "personas", "pacientes", "casos", "tratamiento", "zona",
            "zonas", "cualquier", "otros", "otras", "tiene", "tengan", "consultar", "medico", "reciente", "recientes");

    private ClinicalAlerts() {}

    static List<String> of(ClinicalRecord r) {
        List<String> alerts = new ArrayList<>();
        if (meaningful(r.allergies())) alerts.add("Alergias: " + r.allergies());
        if (meaningful(r.pregnancy())) alerts.add("Embarazo o lactancia: " + r.pregnancy());
        if (meaningful(r.medications())) alerts.add("Medicamentos: " + r.medications());
        if (meaningful(r.conditions())) alerts.add("Condiciones: " + r.conditions());
        return alerts;
    }

    /**
     * Words of the contraindications that also appear in the record, compared without accents
     * and by their first 6 letters (embarazo ~ embarazada, anticoagulantes ~ anticoagulante).
     */
    static List<String> matches(String contraindications, ClinicalRecord r) {
        if (contraindications == null || contraindications.isBlank()) return List.of();
        String history = normalize(Stream.of(r.allergies(), r.medications(), r.conditions(), r.pregnancy(),
                        r.surgeries(), r.habits(), r.observations(), r.skinType())
                .filter(ClinicalAlerts::meaningful)
                .collect(Collectors.joining(" ")));
        if (history.isBlank()) return List.of();
        Set<String> historyStems = Stream.of(history.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 5).map(ClinicalAlerts::stem).collect(Collectors.toSet());
        Set<String> found = new LinkedHashSet<>();
        for (String word : normalize(contraindications).split("[^a-z0-9]+")) {
            if (word.length() < 5 || STOPWORDS.contains(word)) continue;
            if (historyStems.contains(stem(word))) found.add(word);
        }
        return List.copyOf(found);
    }

    static boolean meaningful(String value) {
        return value != null && !value.isBlank() && !NEGATIVE.contains(normalize(value).strip());
    }

    private static String stem(String word) {
        return word.length() > 6 ? word.substring(0, 6) : word;
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
