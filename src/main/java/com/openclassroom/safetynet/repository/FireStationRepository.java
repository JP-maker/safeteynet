package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.FireStation;
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
    private List<FireStation> fireStations = new ArrayList<>(); // Stockage en mémoire

    /**
     * Méthode pour initialiser ou mettre à jour les données des casernes.
     * Appelée par DataLoadingService.
     * @param fireStationList La liste complète des casernes chargées.
     */
    public void setData(List<FireStation> fireStationList) {
        if (fireStationList != null) {
            this.fireStations = new ArrayList<>(fireStationList); // Copie pour éviter modif externe
            logger.info("{} enregistrements de casernes chargés.", this.fireStations.size());
        } else {
            this.fireStations = new ArrayList<>();
            logger.warn("La liste des casernes fournie pour l'initialisation est nulle.");
        }
    }

    /**
     * Trouve toutes les adresses couvertes par un numéro de station donné.
     * @param stationNumber Le numéro de la station (sous forme d'entier).
     * @return Une liste distincte des adresses couvertes, ou une liste vide si non trouvée.
     */
    public List<String> findAddressesByStationNumber(int stationNumber) {
        String stationNumberStr = String.valueOf(stationNumber); // Convertir pour comparaison (si station est String dans le modèle)
        return fireStations.stream()
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
        return fireStations.stream()
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
        return new ArrayList<>(fireStations); // Retourne une copie
    }

    // Ajoutez d'autres méthodes si nécessaire (ex: save, delete si vous gérez des modifs)
}