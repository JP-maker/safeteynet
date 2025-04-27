package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FileIOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections; // Ajout import manquant
import java.util.List;
import java.util.Objects;
import java.util.Optional; // Import non utilisé mais peut être utile pour findByAddress
import java.util.stream.Collectors;

/**
 * Repository responsable de l'accès et de la manipulation des données des casernes de pompiers (FireStations).
 * <p>
 * Ce repository interagit avec le {@link FileIOService} pour obtenir la liste actuelle des casernes
 * et pour persister les modifications (via les méthodes 'set' du FileIOService).
 * Il fournit des méthodes pour rechercher des casernes ou des adresses par différents critères
 * et pour effectuer les opérations CRUD de base sur les mappings adresse/station.
 * </p>
 */
@Repository
public class FireStationRepository {

    private static final Logger logger = LoggerFactory.getLogger(FireStationRepository.class);
    private final FileIOService fileIOService;

    /**
     * Construit une nouvelle instance de FireStationRepository.
     *
     * @param fileIOService Le service injecté responsable de l'accès aux données brutes du fichier.
     */
    @Autowired // Optionnel ici
    public FireStationRepository(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * Trouve toutes les adresses distinctes couvertes par un numéro de station de pompiers donné.
     * La comparaison du numéro de station est exacte (sensible à la casse si les numéros de station dans
     * le fichier ne sont pas purement numériques, bien qu'ici la comparaison se fasse sur des String générés depuis un int).
     *
     * @param stationNumber Le numéro de la station (sous forme d'entier).
     * @return Une {@code List<String>} contenant les adresses uniques couvertes par cette station.
     *         Retourne une liste vide si aucune adresse n'est trouvée pour ce numéro ou si la source de données est vide.
     */
    public List<String> findAddressesByStationNumber(int stationNumber) {
        String stationNumberStr = String.valueOf(stationNumber);
        // Obtient la liste actuelle depuis le cache de FileIOService (copie immuable)
        List<FireStation> currentStations = fileIOService.getFireStations();

        if (currentStations.isEmpty()) {
            return Collections.emptyList();
        }

        // Filtre, mappe vers l'adresse, supprime les doublons et collecte
        return currentStations.stream()
                .filter(f -> Objects.equals(f.getStation(), stationNumberStr)) // Comparaison exacte
                .map(FireStation::getAddress)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Trouve le numéro de station associé à une adresse donnée.
     * La comparaison de l'adresse est exacte et sensible à la casse.
     * S'il existe plusieurs mappings pour la même adresse (ce qui ne devrait pas arriver),
     * seul le numéro de station du premier mapping trouvé est retourné.
     *
     * @param address L'adresse exacte (sensible à la casse) pour laquelle rechercher le numéro de station.
     * @return Le numéro de station ({@code String}) associé à l'adresse, ou {@code null} si aucune
     *         correspondance n'est trouvée ou si l'adresse fournie est {@code null}.
     */
    public String findStationNumberByAddress(String address) {
        // Obtient la liste actuelle
        List<FireStation> currentStations = fileIOService.getFireStations();

        if (address == null || currentStations.isEmpty()) {
            return null;
        }

        // Filtre sur l'adresse exacte, mappe vers le numéro de station, prend le premier trouvé
        return currentStations.stream()
                .filter(f -> Objects.equals(f.getAddress(), address)) // Comparaison exacte et sensible à la casse
                .map(FireStation::getStation)
                .findFirst()
                .orElse(null); // Retourne null si le stream est vide après filtrage
    }

    /**
     * Récupère la liste complète de tous les mappings caserne/adresse actuellement enregistrés.
     * Retourne une nouvelle copie mutable de la liste obtenue depuis {@link FileIOService}.
     *
     * @return Une {@code List<FireStation>} contenant tous les mappings.
     *         Cette liste est une copie mutable et peut être vide.
     */
    public List<FireStation> findAll() {
        // FileIOService retourne déjà une copie (potentiellement immuable), mais ici on crée explicitement une ArrayList mutable.
        // Cela peut être utile si l'appelant s'attend à pouvoir modifier la liste retournée (bien que ce ne soit pas idéal).
        // Si FileIOService retourne déjà une copie immuable via List.copyOf(), retourner directement cela serait plus sûr.
        return new ArrayList<>(this.fileIOService.getFireStations());
    }

    // --- Méthodes Write (Create/Update/Delete) ---

    /**
     * Sauvegarde (ajoute ou met à jour) un mapping adresse/station.
     * <p>
     * Si un mapping existe déjà pour l'adresse fournie (comparaison insensible à la casse),
     * il est remplacé par les nouvelles informations. Sinon, un nouveau mapping est ajouté.
     * Les valeurs d'adresse et de numéro de station sont nettoyées (trim) avant la sauvegarde.
     * </p><p>
     * Après modification de la liste en mémoire, la liste complète est passée à
     * {@link FileIOService#setFireStations(List)} pour persistance.
     * </p>
     *
     * @param fireStation L'objet {@link FireStation} à sauvegarder. L'adresse et le numéro de station
     *                    ne doivent pas être nuls ou vides.
     * @return L'objet {@link FireStation} tel qu'il a été sauvegardé (avec adresse et station trimées).
     * @throws IllegalArgumentException si {@code fireStation} ou ses champs adresse/station sont nuls ou vides.
     */
    public FireStation save(FireStation fireStation) {
        // Validation des entrées
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank() ||
                fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            throw new IllegalArgumentException("FireStation, son adresse et son numéro de station ne peuvent être nuls ou vides.");
        }

        // 1. Obtenir une copie mutable de la liste actuelle
        List<FireStation> currentStations = new ArrayList<>(fileIOService.getFireStations());

        // Nettoyer les données d'entrée
        String addressTrimmed = fireStation.getAddress().trim();
        String stationTrimmed = fireStation.getStation().trim();

        // 2. Enlever l'ancienne entrée pour cette adresse (insensible à la casse)
        boolean removed = currentStations.removeIf(fs -> fs.getAddress() != null
                && fs.getAddress().equalsIgnoreCase(addressTrimmed));
        if (removed) {
            logger.debug("Ancienne entrée pour l'adresse '{}' supprimée avant la sauvegarde.", addressTrimmed);
        }

        // 3. Créer et ajouter la nouvelle entrée (ou la version mise à jour) avec les données nettoyées
        FireStation stationToSave = new FireStation();
        stationToSave.setAddress(addressTrimmed);
        stationToSave.setStation(stationTrimmed);
        currentStations.add(stationToSave);
        logger.debug("Nouvelle entrée pour l'adresse '{}' avec station '{}' ajoutée à la liste.", addressTrimmed, stationTrimmed);

        // 4. Persister la liste complète modifiée via FileIOService
        fileIOService.setFireStations(currentStations);

        logger.info("FireStation sauvegardée: Adresse='{}', Station='{}'", addressTrimmed, stationTrimmed);
        // Retourner l'objet tel qu'il a été ajouté à la liste (avec trim)
        return stationToSave;
    }

    /**
     * Supprime le mapping adresse/station pour l'adresse spécifiée.
     * La recherche de l'adresse à supprimer est insensible à la casse et ignore les espaces
     * de début/fin (trim).
     * Si un mapping est trouvé et supprimé, la liste mise à jour est persistée via
     * {@link FileIOService#setFireStations(List)}.
     *
     * @param address L'adresse (insensible à la casse, trimée) dont le mapping doit être supprimé.
     *                Ne doit pas être nul ou vide.
     * @return {@code true} si un mapping a été trouvé et supprimé, {@code false} sinon (y compris
     *         si l'adresse fournie est nulle ou vide).
     */
    public boolean deleteByAddress(String address) {
        // Validation et nettoyage de l'adresse
        if (address == null || address.isBlank()) {
            logger.debug("Tentative de suppression avec adresse nulle ou vide.");
            return false;
        }
        String trimmedAddress = address.trim();

        // Obtenir une copie mutable
        List<FireStation> currentStations = new ArrayList<>(fileIOService.getFireStations());

        // Essayer de supprimer l'entrée (insensible à la casse)
        boolean removed = currentStations.removeIf(fs -> fs.getAddress() != null
                && fs.getAddress().equalsIgnoreCase(trimmedAddress));

        // Si suppression réussie, persister la nouvelle liste
        if (removed) {
            fileIOService.setFireStations(currentStations);
            logger.info("Mapping FireStation pour l'adresse '{}' supprimé.", trimmedAddress);
        } else {
            // Logguer si non trouvé, mais ne rien faire d'autre
            logger.warn("Aucun mapping FireStation trouvé pour l'adresse '{}' lors de la tentative de suppression.", trimmedAddress);
        }
        // Retourner le résultat de l'opération de suppression
        return removed;
    }

    /**
     * Vérifie si un mapping adresse/station existe pour une adresse donnée.
     * La vérification est insensible à la casse et ignore les espaces de début/fin (trim)
     * pour l'adresse fournie.
     *
     * @param address L'adresse (insensible à la casse, trimée) à vérifier.
     * @return {@code true} si un mapping existe pour cette adresse, {@code false} sinon (y compris
     *         si l'adresse fournie est nulle ou vide).
     */
    public boolean existsByAddress(String address) {
        // Validation et nettoyage
        if (address == null || address.isBlank()) {
            logger.trace("Vérification d'existence pour une adresse nulle ou vide -> false");
            return false;
        }
        String trimmedAddress = address.trim();

        // Obtient la liste actuelle
        List<FireStation> currentStations = fileIOService.getFireStations();

        if (currentStations.isEmpty()) {
            logger.trace("Vérification d'existence dans une liste vide -> false");
            return false;
        }

        // Vérifie si au moins un élément correspond (insensible à la casse)
        return currentStations.stream()
                .anyMatch(f -> f.getAddress() != null && f.getAddress().equalsIgnoreCase(trimmedAddress));
    }
}