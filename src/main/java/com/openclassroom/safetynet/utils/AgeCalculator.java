package com.openclassroom.safetynet.utils;

import com.openclassroom.safetynet.constants.ConfigData; // Importer si DATE_FORMATTER utilise une constante d'ici
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Classe utilitaire pour calculer l'âge d'une personne à partir de sa date de naissance.
 * <p>
 * Cette classe utilise le format de date défini (actuellement "MM/dd/yyyy") pour parser
 * la date de naissance fournie sous forme de chaîne de caractères.
 * Elle contient uniquement une méthode statique et n'est pas destinée à être instanciée.
 * </p>
 * @see ConfigData#DATE_FORMAT Format de date attendu.
 */
public final class AgeCalculator { // Marquer final car classe utilitaire statique

    private static final Logger logger = LoggerFactory.getLogger(AgeCalculator.class);

    /**
     * Formatteur de date utilisé pour parser les chaînes de date de naissance.
     * Le format attendu est "MM/dd/yyyy".
     * @see ConfigData#DATE_FORMAT Constante définissant le format.
     */
    // Utiliser la constante si elle existe et est publique, sinon définir ici.
    // private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(ConfigData.DATE_FORMAT);
    // Si ConfigData.DATE_FORMAT n'est pas accessible/souhaité, définir ici :
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    private AgeCalculator() {
        // Empêche l'instanciation
        throw new UnsupportedOperationException("Cette classe utilitaire ne doit pas être instanciée.");
    }

    /**
     * Calcule l'âge en années complètes d'une personne à partir de sa date de naissance fournie
     * sous forme de chaîne de caractères.
     * <p>
     * La méthode utilise le format {@value #DATE_FORMATTER} (MM/dd/yyyy) pour interpréter la chaîne de date.
     * Elle calcule la différence en années entre la date de naissance et la date actuelle.
     * </p>
     *
     * @param birthdateString La date de naissance sous forme de chaîne, attendue au format "MM/dd/yyyy".
     *                        Ne doit pas être {@code null} ou vide.
     * @return L'âge calculé en années entières.
     * @throws DateTimeParseException Si la chaîne {@code birthdateString} ne peut pas être parsée selon le format attendu.
     *                                L'erreur est logguée avant que l'exception ne soit relancée.
     * @throws IllegalArgumentException Si la chaîne {@code birthdateString} est {@code null}, vide, ou représente
     *                                  une date dans le futur par rapport à la date actuelle. L'erreur est logguée.
     */
    public static int calculateAge(String birthdateString) {
        // Validation de l'entrée
        if (birthdateString == null || birthdateString.trim().isEmpty()) {
            // Log spécifique pour ce cas
            logger.error("Tentative de calcul d'âge avec une date de naissance nulle ou vide.");
            throw new IllegalArgumentException("La date de naissance ne peut pas être nulle ou vide.");
        }

        try {
            // Parser la date de naissance
            LocalDate birthDate = LocalDate.parse(birthdateString.trim(), DATE_FORMATTER); // Ajouter trim() par sécurité
            // Obtenir la date actuelle
            LocalDate currentDate = LocalDate.now();

            // Vérifier si la date de naissance n'est pas dans le futur
            if (birthDate.isAfter(currentDate)) {
                logger.warn("La date de naissance fournie ({}) est dans le futur.", birthdateString);
                throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur: " + birthdateString);
            }

            // Calculer la période entre les deux dates et retourner les années complètes
            int age = Period.between(birthDate, currentDate).getYears();
            logger.trace("Calcul de l'âge réussi pour date '{}': {} ans.", birthdateString, age);
            return age;

        } catch (DateTimeParseException e) {
            // Logguer l'erreur de parsing avant de relancer l'exception
            logger.error("Format de date invalide fourni : '{}'. Attendu: MM/dd/yyyy", birthdateString, e);
            // Relancer l'exception pour que l'appelant (le service) puisse la gérer
            throw e;
        } catch (IllegalArgumentException iae) {
            // Relancer l'exception pour la date future (déjà logguée)
            throw iae;
        } catch (Exception ex) {
            // Capturer toute autre exception inattendue
            logger.error("Erreur inattendue lors du calcul de l'âge pour la date '{}'", birthdateString, ex);
            // Relancer comme une RuntimeException pour signaler un problème potentiellement grave
            throw new RuntimeException("Erreur inattendue lors du calcul de l'âge pour la date: " + birthdateString, ex);
        }
    }
}