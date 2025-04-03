package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.service.FireStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}