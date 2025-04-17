package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FileIOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
}