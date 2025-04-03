package com.openclassroom.safetynet.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AgeCalculator {

    private static final Logger logger = LoggerFactory.getLogger(AgeCalculator.class);
    // Ajustez le format si nécessaire (ex: "MM/dd/yyyy")
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Calcule l'âge en années complètes à partir d'une date de naissance.
     *
     * @param birthdateString La date de naissance au format "MM/dd/yyyy".
     * @return L'âge en années.
     * @throws DateTimeParseException Si le format de la date est invalide.
     * @throws IllegalArgumentException Si la date de naissance est dans le futur.
     */
    public static int calculateAge(String birthdateString) {
        if (birthdateString == null || birthdateString.trim().isEmpty()) {
            throw new IllegalArgumentException("La date de naissance ne peut pas être nulle ou vide.");
        }

        try {
            LocalDate birthDate = LocalDate.parse(birthdateString, DATE_FORMATTER);
            LocalDate currentDate = LocalDate.now();

            if (birthDate.isAfter(currentDate)) {
                logger.warn("La date de naissance {} est dans le futur.", birthdateString);
                throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
            }

            return Period.between(birthDate, currentDate).getYears();

        } catch (DateTimeParseException e) {
            logger.error("Format de date invalide pour : {}", birthdateString, e);
            throw e; // Relancer pour que le service puisse la gérer
        }
    }
}