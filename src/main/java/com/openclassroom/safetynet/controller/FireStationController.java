package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.AddressWithListOfPersonWithMedicalRecordDTO;
import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.dto.ListOfAddressWithListOfPersonWithMedicalRecordDTO;
import com.openclassroom.safetynet.dto.PhoneAlertDTO;
import com.openclassroom.safetynet.service.FireStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * @param station_numbers Une liste de numéro de station de pompier.
     * @return ResponseEntity contenant le DTO floodDTO ou une réponse 404.
     */
    @GetMapping("/flood/stations")
    public ResponseEntity<ListOfAddressWithListOfPersonWithMedicalRecordDTO> getListOfPersonsWithMedicalRecordsByListOfFireStation(
            @RequestParam("station_numbers") List<String> fireStationsNumber) {

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
}