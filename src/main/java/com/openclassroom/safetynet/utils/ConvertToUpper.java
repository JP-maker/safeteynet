package com.openclassroom.safetynet.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects; // Importer pour Objects.requireNonNullElseGet si utilisé
import java.util.stream.Collectors;

/**
 * Classe utilitaire fournissant des méthodes pour la conversion de chaînes de caractères.
 * Actuellement, elle contient une méthode pour convertir tous les éléments d'une liste
 * de chaînes en majuscules.
 * <p>
 * Cette classe contient uniquement des méthodes statiques et n'est pas destinée à être instanciée.
 * </p>
 */
public final class ConvertToUpper { // Marquer la classe comme final car elle n'a que des méthodes statiques

    private static final Logger logger = LoggerFactory.getLogger(ConvertToUpper.class);

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    private ConvertToUpper() {
        // Empêche l'instanciation
        throw new UnsupportedOperationException("Cette classe utilitaire ne doit pas être instanciée.");
    }

    /**
     * Convertit tous les éléments d'une liste de chaînes de caractères en leur équivalent en majuscules.
     * <p>
     * Crée et retourne une <b>nouvelle</b> liste contenant les résultats de la conversion.
     * La liste originale n'est pas modifiée.
     * Si un élément de la liste originale est {@code null}, il restera {@code null} dans la liste retournée.
     * </p>
     *
     * @param originalList La liste de chaînes de caractères à convertir. Peut être {@code null} ou vide.
     * @return Une nouvelle {@code List<String>} contenant les versions en majuscules des chaînes originales,
     *         ou une liste vide si la liste originale était {@code null} ou vide.
     *         Les éléments {@code null} de la liste originale sont préservés.
     */
    public static List<String> convertList(List<String> originalList) {
        // Gérer le cas où la liste d'entrée est nulle ou vide
        if (originalList == null || originalList.isEmpty()) {
            logger.debug("La liste originale est nulle ou vide, retour d'une liste vide immuable.");
            return Collections.emptyList(); // Retourne une liste vide immuable et sûre
        }

        logger.info("Conversion en majuscules de la liste originale : {}", originalList);

        // Utilisation de Stream API pour une conversion fonctionnelle et immuable par nature
        List<String> upperCaseList = originalList.stream()
                .map(s -> {
                    if (s == null) {
                        // Préserver les éléments nulls s'il y en a
                        return null;
                    }
                    // Convertir en majuscules (en utilisant la locale par défaut)
                    return s.toUpperCase();
                })
                // Collecter les résultats dans une nouvelle liste (modifiable par défaut avec toList())
                .collect(Collectors.toList());

        logger.info("Liste résultante après conversion en majuscules : {}", upperCaseList);
        // Retourner la nouvelle liste créée
        return upperCaseList;
    }
}