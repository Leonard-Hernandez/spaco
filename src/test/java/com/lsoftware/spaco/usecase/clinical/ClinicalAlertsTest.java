package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalAlertsTest {

    @Test
    void contraindicationWordsAreFoundInTheHistoryWithoutAccentsOrExactForms() {
        ClinicalRecord record = record("Alergia a la miel de abejas", "Anticoagulante (warfarina)", "Embarazada, 20 semanas");

        assertThat(ClinicalAlerts.matches(
                "Embarazo. Alergia a la miel o al polen. Tratamiento con anticoagulantes.", record))
                .containsExactly("embarazo", "alergia", "anticoagulantes");
    }

    @Test
    void negativeAnswersAreNotAlerts() {
        ClinicalRecord record = record("Ninguna", "No", "No");

        assertThat(ClinicalAlerts.of(record)).isEmpty();
        assertThat(ClinicalAlerts.matches("Embarazo, alergias", record)).isEmpty();
    }

    @Test
    void filledFieldsBecomeAlerts() {
        assertThat(ClinicalAlerts.of(record("Látex", null, "Lactancia")))
                .containsExactly("Alergias: Látex", "Embarazo o lactancia: Lactancia");
    }

    private static ClinicalRecord record(String allergies, String medications, String pregnancy) {
        return new ClinicalRecord(3L, allergies, medications, null, pregnancy, null, null, null, null, null, "Ana",
                OffsetDateTime.now());
    }
}
