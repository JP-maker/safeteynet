package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.AddressWithListOfPersonWithMedicalRecordDTO;
import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.dto.ListOfAddressWithListOfPersonWithMedicalRecordDTO;
import com.openclassroom.safetynet.dto.PhoneAlertDTO;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FireStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class FireStationController {

    private static final Logger logger = LoggerFactory.getLogger(FireStationController.class);

    private final FireStationService fireStationService;

    public FireStationController(FireStationService fireStationService) {
        this.fireStationService = fireStationService;
    }

    /**
     * Endpoint pour obtenir les personnes couvertes par une station et le décompte enfants/adultes.
     * URL: GET /firestation?stationNumber=<station_number>
     *
     * @param stationNumber Le numéro de la station de pompier.
     * @return ResponseEntity contenant le DTO FirestationCoverageDTO ou une réponse 404.
     */
    @GetMapping("/firestation")
    public ResponseEntity<FireStationCoverageDTO> getPeopleCoveredByFireStation(
            @RequestParam("stationNumber") int stationNumber) {

        logger.info("Requête reçue pour /firestation avec stationNumber={}", stationNumber);

        if (stationNumber <= 0) {
            logger.warn("Numéro de station invalide reçu: {}", stationNumber);
            // Retourner Bad Request si le numéro n'est pas valide logiquement
            return ResponseEntity.badRequest().build();
        }

        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        // Si le service retourne un résultat, renvoyer 200 OK avec les données
        // Si le service retourne Optional.empty (station non trouvée), renvoyer 404 Not Found
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour stationNumber={}", stationNumber);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour stationNumber={}", stationNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint pour obtenir les numéros de téléphone en donnant un numéro de station.
     * URL: GET /phoneAlert?firestation=<firestation_number>
     *
     * @param fireStationNumber Le numéro de la station de pompier.
     * @return ResponseEntity contenant le DTO PHoneAlertDTO ou une réponse 404.
     */
    @GetMapping("/phoneAlert")
    public ResponseEntity<PhoneAlertDTO> getPhoneNumberByFireStation(
            @RequestParam("firestation") int fireStationNumber) {

        logger.info("Requête reçue pour /phoneAlert avec firestation={}", fireStationNumber);

        if (fireStationNumber <= 0) {
            logger.warn("Numéro de station invalide reçu: {}", fireStationNumber);
            // Retourner Bad Request si le numéro n'est pas valide logiquement
            return ResponseEntity.badRequest().build();
        }

        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(fireStationNumber);

        // Si le service retourne un résultat, renvoyer 200 OK avec les données
        // Si le service retourne Optional.empty (station non trouvée), renvoyer 404 Not Found
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour firestation={}", fireStationNumber);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour firestation={}", fireStationNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint pour retourner une liste de tous les foyers desservis par la caserne
     * URL: GET /flood/stations?stations=<a list of station_numbers>
     *
     * @param stations Une liste de numéro de station de pompier.
     * @return ResponseEntity contenant le DTO floodDTO ou une réponse 404.
     */
    @GetMapping("/flood/stations")
    public ResponseEntity<ListOfAddressWithListOfPersonWithMedicalRecordDTO> getListOfPersonsWithMedicalRecordsByListOfFireStation(
            @RequestParam("stations") List<String> fireStationsNumber) {

        logger.info("Requête reçue pour /flood/stations avec station_numbers={}", fireStationsNumber);

        if (fireStationsNumber.isEmpty()) {
            logger.warn("La liste des numéros de station est vide: {}", fireStationsNumber);
            return ResponseEntity.badRequest().build();
        }

        for (String stationNumber : fireStationsNumber) {
            if (!stationNumber.matches("^\\d+$")) {
                logger.warn("Numéro de station invalide reçu: {}", stationNumber);
                return ResponseEntity.badRequest().build();
            }
        }

        Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> result = fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(fireStationsNumber);

        // Si le service retourne un résultat, renvoyer 200 OK avec les données
        // Si le service retourne Optional.empty (station non trouvée), renvoyer 404 Not Found
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour firestation={}", fireStationsNumber);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour firestation={}", fireStationsNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint pour ajouter un nouveau mapping caserne/adresse.
     * POST /firestation
     * Corps: {"address": "...", "station": "..."}
     */
    @PostMapping("/firestation")
    public ResponseEntity<FireStation> addFireStation(@RequestBody FireStation fireStation) {
        try {
            FireStation createdStation = fireStationService.addFireStation(fireStation);
            logger.info("Endpoint POST /firestation: Mapping créé avec succès pour {}", fireStation.getAddress());
            // Retourne 201 Created avec l'objet créé
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStation);
        } catch (IllegalArgumentException e) {
            // Gère le cas où l'adresse existe déjà (conflit) ou données invalides
            logger.warn("Endpoint POST /firestation: Échec de création - {}", e.getMessage());
            if (e.getMessage().contains("existe déjà")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
            } else {
                return ResponseEntity.badRequest().build(); // 400 Bad Request pour données invalides
            }
        } catch (Exception e) {
            logger.error("Endpoint POST /firestation: Erreur interne lors de la création", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint pour mettre à jour le numéro de station d'un mapping existant.
     * PUT /firestation
     * Corps: {"address": "...", "station": "..."} (l'adresse identifie le mapping à maj)
     */
    @PutMapping("/firestation")
    public ResponseEntity<FireStation> updateFireStation(@RequestBody FireStation fireStation) {
        try {
            Optional<FireStation> updatedStationOpt = fireStationService.updateFireStation(fireStation);

            return updatedStationOpt
                    .map(station -> {
                        logger.info("Endpoint PUT /firestation: Mapping mis à jour avec succès pour {}", station.getAddress());
                        return ResponseEntity.ok(station); // 200 OK avec l'objet mis à jour
                    })
                    .orElseGet(() -> {
                        logger.warn("Endpoint PUT /firestation: Mapping non trouvé pour l'adresse {}", fireStation.getAddress());
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    });
        } catch (IllegalArgumentException e) {
            logger.warn("Endpoint PUT /firestation: Échec de mise à jour - Données invalides : {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        } catch (Exception e) {
            logger.error("Endpoint PUT /firestation: Erreur interne lors de la mise à jour", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint pour supprimer un mapping caserne/adresse.
     * DELETE /firestation?address=<adresse_a_supprimer>
     */
    @DeleteMapping("/firestation")
    public ResponseEntity<Void> deleteFireStation(@RequestParam String address) {
        try {
            boolean deleted = fireStationService.deleteFireStationMapping(address);

            if (deleted) {
                logger.info("Endpoint DELETE /firestation: Mapping supprimé avec succès pour l'adresse {}", address);
                return ResponseEntity.noContent().build(); // 204 No Content (succès sans corps)
            } else {
                logger.warn("Endpoint DELETE /firestation: Mapping non trouvé pour l'adresse {}", address);
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (Exception e) {
            logger.error("Endpoint DELETE /firestation: Erreur interne lors de la suppression pour l'adresse {}", address, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint pour récupérer tous les mappings caserne/adresse.
     * GET /firestation
     */
    @GetMapping("/firestation/all")
    public ResponseEntity<List<FireStation>> getAllFireStations() {
        try {
            List<FireStation> stations = fireStationService.getAllFireStations();
            logger.debug("Endpoint GET /firestation/all: Récupération de {} mappings.", stations.size());
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            logger.error("Endpoint GET /firestation/all: Erreur interne lors de la récupération", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}