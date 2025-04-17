package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FileIOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class FireStationRepository {

    private static final Logger logger = LoggerFactory.getLogger(FireStationRepository.class);
    private final FileIOService fileIOService;

    public FireStationRepository(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * Trouve toutes les adresses couvertes par un numéro de station donné.
     * @param stationNumber Le numéro de la station (sous forme d'entier).
     * @return Une liste distincte des adresses couvertes, ou une liste vide si non trouvée.
     */
    public List<String> findAddressesByStationNumber(int stationNumber) {
        String stationNumberStr = String.valueOf(stationNumber);
        return fileIOService.getFireStations().stream()
                .filter(f -> Objects.equals(f.getStation(), stationNumberStr))
                .map(FireStation::getAddress) // Extrait l'adresse
                .distinct() // Évite les doublons d'adresses si une station couvre plusieurs fois la même
                .collect(Collectors.toList());
    }

    /**
     * Trouve le numéro de station pour une adresse donnée.
     * @param address L'adresse recherchée.
     * @return Le numéro de station (en String), ou null si l'adresse n'est pas trouvée.
     */
    public String findStationNumberByAddress(String address) {
        return fileIOService.getFireStations().stream()
                .filter(f -> Objects.equals(f.getAddress(), address))
                .map(FireStation::getStation)
                .findFirst() // Prend la première correspondance s'il y en a plusieurs (devrait être unique)
                .orElse(null); // Retourne null si aucune correspondance
    }

    /**
     * Récupère toutes les casernes (utile pour d'autres opérations).
     * @return Une copie de la liste des casernes.
     */
    public List<FireStation> findAll() {
        return new ArrayList<>(this.fileIOService.getFireStations()); // Retourne une copie
    }

    // --- Méthodes Write (Create/Update/Delete) ---

    /**
     * Sauvegarde une caserne (ajoute si nouvelle adresse, met à jour si adresse existante).
     * @param fireStation L'objet FireStation à sauvegarder.
     * @return L'objet FireStation sauvegardé.
     * @throws IllegalArgumentException si fireStation ou son adresse/station est null/vide.
     */
    public FireStation save(FireStation fireStation) {
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank() ||
                fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            throw new IllegalArgumentException("FireStation, son adresse et son numéro de station ne peuvent être nuls ou vides.");
        }

        // 1. Obtenir la liste actuelle (qui est une copie)
        List<FireStation> currentStations = new ArrayList<>(fileIOService.getFireStations());

        // 2. Enlever l'ancienne entrée pour cette adresse, si elle existe (comparaison insensible à la casse)
        boolean removed = currentStations.removeIf(fs -> fs.getAddress() != null
                && fs.getAddress().equalsIgnoreCase(fireStation.getAddress().trim()));
        if (removed) {
            logger.debug("Ancienne entrée pour l'adresse '{}' supprimée avant la sauvegarde.", fireStation.getAddress());
        }

        // 3. Ajouter la nouvelle entrée (ou la version mise à jour)
        FireStation stationToSave = new FireStation();
        stationToSave.setAddress(fireStation.getAddress().trim());
        stationToSave.setStation(fireStation.getStation().trim());
        currentStations.add(stationToSave);
        logger.debug("Nouvelle entrée pour l'adresse '{}' avec station '{}' ajoutée à la liste.", stationToSave.getAddress(), stationToSave.getStation());

        // 4. Passer la liste modifiée complète à FileIOService pour sauvegarde
        fileIOService.setFireStations(currentStations);

        logger.info("FireStation sauvegardée: Adresse='{}', Station='{}'", stationToSave.getAddress(), stationToSave.getStation());
        return stationToSave; // Retourner l'objet sauvegardé (avec trim)
    }

    /**
     * Supprime le mapping d'une caserne par son adresse.
     * @param address L'adresse dont le mapping doit être supprimé.
     * @return true si un mapping a été trouvé et supprimé, false sinon.
     */
    public boolean deleteByAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String trimmedAddress = address.trim();

        List<FireStation> currentStations = new ArrayList<>(fileIOService.getFireStations());

        // Essayer de supprimer l'entrée correspondante (insensible à la casse)
        boolean removed = currentStations.removeIf(fs -> fs.getAddress() != null
                && fs.getAddress().equalsIgnoreCase(trimmedAddress));

        // Si quelque chose a été supprimé, sauvegarder la nouvelle liste
        if (removed) {
            fileIOService.setFireStations(currentStations);
            logger.info("Mapping FireStation pour l'adresse '{}' supprimé.", trimmedAddress);
        } else {
            logger.warn("Aucun mapping FireStation trouvé pour l'adresse '{}' lors de la tentative de suppression.", trimmedAddress);
        }
        return removed;
    }

    /**
     * Vérifie si un mapping existe pour une adresse donnée.
     * @param address L'adresse à vérifier.
     * @return true si un mapping existe, false sinon.
     */
    public boolean existsByAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        // Utiliser equalsIgnoreCase pour être plus flexible sur la casse de l'adresse
        return fileIOService.getFireStations().stream()
                .anyMatch(f -> f.getAddress() != null && f.getAddress().equalsIgnoreCase(address.trim()));
    }
}