package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.dto.ListOfAddressWithListOfPersonWithMedicalRecordDTO;
import com.openclassroom.safetynet.dto.PhoneAlertDTO;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FireStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST gérant les requêtes HTTP liées aux casernes de pompiers (FireStations).
 * <p>
 * Expose des endpoints pour les opérations CRUD de base sur les mappings adresse/station,
 * ainsi que des endpoints spécifiques pour obtenir des informations agrégées basées sur
 * les numéros de station (personnes couvertes, alertes téléphoniques, données pour inondations).
 * </p><p>
 * Utilise {@link FireStationService} pour déléguer la logique métier.
 * </p>
 */
@RestController
public class FireStationController {

    private static final Logger logger = LoggerFactory.getLogger(FireStationController.class);

    private final FireStationService fireStationService;

    /**
     * Construit une nouvelle instance de FireStationController.
     *
     * @param fireStationService Le service injecté pour gérer la logique métier des casernes.
     */
    @Autowired // Optionnel ici
    public FireStationController(FireStationService fireStationService) {
        this.fireStationService = fireStationService;
    }

    /**
     * Endpoint GET pour obtenir la liste des personnes couvertes par une station de pompiers donnée,
     * ainsi qu'un décompte des adultes et des enfants.
     * <p>
     * URL: {@code GET /firestation?stationNumber=<station_number>}
     * </p>
     *
     * @param stationNumber Le numéro de la station de pompiers (entier positif).
     *                      Requis comme paramètre de requête URL.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link FireStationCoverageDTO} dans le corps si la station est trouvée.</li>
     *             <li>Code 404 (Not Found) si aucune adresse n'est associée à ce numéro de station.</li>
     *             <li>Code 400 (Bad Request) si le {@code stationNumber} est invalide (<= 0 ou non fourni).</li>
     *         </ul>
     */
    @GetMapping("/firestation")
    public ResponseEntity<FireStationCoverageDTO> getPeopleCoveredByFireStation(
            @RequestParam("stationNumber") int stationNumber) { // @RequestParam rend le paramètre requis par défaut

        logger.info("Requête GET /firestation reçue avec stationNumber={}", stationNumber);

        // Validation de base du paramètre
        if (stationNumber <= 0) {
            logger.warn("Numéro de station invalide reçu: {}", stationNumber);
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service
        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        // Construction de la réponse basée sur le résultat du service
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /firestation?stationNumber={}", stationNumber);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /firestation?stationNumber={}", stationNumber);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint GET pour obtenir les numéros de téléphone de tous les résidents
     * couverts par une station de pompiers donnée.
     * <p>
     * URL: {@code GET /phoneAlert?firestation=<firestation_number>}
     * </p>
     *
     * @param fireStationNumber Le numéro de la station de pompiers (entier positif).
     *                          Requis comme paramètre de requête URL.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link PhoneAlertDTO} dans le corps si la station est trouvée (la liste de téléphones peut être vide).</li>
     *             <li>Code 404 (Not Found) si aucune adresse n'est associée à ce numéro de station.</li>
     *             <li>Code 400 (Bad Request) si le {@code firestation} est invalide (<= 0 ou non fourni).</li>
     *         </ul>
     */
    @GetMapping("/phoneAlert")
    public ResponseEntity<PhoneAlertDTO> getPhoneNumberByFireStation(
            @RequestParam("firestation") int fireStationNumber) { // Nom du paramètre URL est "firestation"

        logger.info("Requête GET /phoneAlert reçue avec firestation={}", fireStationNumber);

        // Validation de base
        if (fireStationNumber <= 0) {
            logger.warn("Numéro de station invalide reçu: {}", fireStationNumber);
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service
        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(fireStationNumber);

        // Construction de la réponse
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /phoneAlert?firestation={}", fireStationNumber);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /phoneAlert?firestation={}", fireStationNumber);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint GET pour obtenir une liste de tous les foyers (regroupés par adresse) desservis
     * par une liste donnée de numéros de station. Inclut les informations des personnes
     * (nom, téléphone, âge) et leurs antécédents médicaux (médicaments, allergies).
     * <p>
     * URL: {@code GET /flood/stations?stations=<station_number1>&stations=<station_number2>...}
     * </p>
     *
     * @param fireStationsNumber Une liste de numéros de station (chaînes de caractères représentant des entiers positifs).
     *                           Requise comme paramètre de requête URL (peut être répétée).
     *                           Exemple: {@code /flood/stations?stations=1&stations=2}
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link ListOfAddressWithListOfPersonWithMedicalRecordDTO} dans le corps.
     *                 La liste peut être vide si aucune des stations ne dessert d'adresses ou si les personnes n'ont pas d'infos.</li>
     *             <li>Code 404 (Not Found) si le service retourne Optional.empty() (comportement actuel peu probable).</li>
     *             <li>Code 400 (Bad Request) si la liste {@code stations} est vide ou contient des valeurs non numériques.</li>
     *         </ul>
     */
    @GetMapping("/flood/stations")
    public ResponseEntity<ListOfAddressWithListOfPersonWithMedicalRecordDTO> getListOfPersonsWithMedicalRecordsByListOfFireStation(
            @RequestParam("stations") List<String> fireStationsNumber) { // @RequestParam pour lire les paramètres répétes dans une liste

        logger.info("Requête GET /flood/stations reçue avec stations={}", fireStationsNumber);

        // Validation des paramètres
        if (fireStationsNumber == null || fireStationsNumber.isEmpty()) {
            logger.warn("La liste des numéros de station est nulle ou vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        for (String stationNumber : fireStationsNumber) {
            // Valider que chaque élément est bien un nombre entier positif
            if (stationNumber == null || !stationNumber.matches("^\\d+$") || Integer.parseInt(stationNumber) <= 0) {
                logger.warn("Liste de stations contient une valeur invalide: '{}'", stationNumber);
                return ResponseEntity.badRequest().build(); // 400
            }
        }

        // Appel au service
        Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> result = fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(fireStationsNumber);

        // Construction de la réponse (le service retourne toujours Optional.of(...) actuellement)
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /flood/stations?stations={}", fireStationsNumber);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    // Ce cas ne devrait pas se produire avec la logique actuelle du service, mais on le garde par sécurité
                    logger.warn("Réponse 404 Not Found inattendue pour /flood/stations?stations={}", fireStationsNumber);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint POST pour ajouter un nouveau mapping entre une adresse et un numéro de station.
     * <p>
     * URL: {@code POST /firestation}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link FireStation}.
     * Exemple: {@code {"address": "123 Main St", "station": "1"}}
     * </p>
     *
     * @param fireStation L'objet {@link FireStation} à créer, désérialisé depuis le corps JSON de la requête.
     *                    L'adresse et le numéro de station doivent être valides.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 201 (Created) avec l'objet {@link FireStation} créé dans le corps en cas de succès.</li>
     *             <li>Code 409 (Conflict) si un mapping existe déjà pour cette adresse.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (adresse/station manquantes).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PostMapping("/firestation")
    public ResponseEntity<FireStation> addFireStation(@RequestBody FireStation fireStation) {
        try {
            FireStation createdStation = fireStationService.addFireStation(fireStation);
            logger.info("Endpoint POST /firestation: Mapping créé avec succès pour l'adresse '{}'", createdStation.getAddress());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStation); // 201
        } catch (IllegalArgumentException e) {
            logger.warn("Endpoint POST /firestation: Échec de création - {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("existe déjà")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
            } else {
                // Considérer toute autre IllegalArgumentException comme une mauvaise requête
                return ResponseEntity.badRequest().build(); // 400
            }
        } catch (Exception e) {
            logger.error("Endpoint POST /firestation: Erreur interne inattendue lors de la création", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint PUT pour mettre à jour le numéro de station associé à une adresse existante.
     * <p>
     * URL: {@code PUT /firestation}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link FireStation}
     * avec l'adresse à identifier et le nouveau numéro de station.
     * Exemple: {@code {"address": "123 Main St", "station": "2"}}
     * </p>
     *
     * @param fireStation L'objet {@link FireStation} contenant les informations de mise à jour,
     *                    désérialisé depuis le corps JSON de la requête.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec l'objet {@link FireStation} mis à jour dans le corps si l'adresse est trouvée.</li>
     *             <li>Code 404 (Not Found) si aucune station n'est associée à l'adresse fournie.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (adresse/station manquantes).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PutMapping("/firestation")
    public ResponseEntity<FireStation> updateFireStation(@RequestBody FireStation fireStation) {
        try {
            Optional<FireStation> updatedStationOpt = fireStationService.updateFireStation(fireStation);

            return updatedStationOpt
                    .map(station -> {
                        logger.info("Endpoint PUT /firestation: Mapping mis à jour avec succès pour l'adresse '{}'", station.getAddress());
                        return ResponseEntity.ok(station); // 200
                    })
                    .orElseGet(() -> {
                        logger.warn("Endpoint PUT /firestation: Mapping non trouvé pour l'adresse '{}'",
                                (fireStation != null ? fireStation.getAddress() : "null"));
                        return ResponseEntity.notFound().build(); // 404
                    });
        } catch (IllegalArgumentException e) {
            logger.warn("Endpoint PUT /firestation: Échec de mise à jour - Données invalides : {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // 400
        } catch (Exception e) {
            logger.error("Endpoint PUT /firestation: Erreur interne inattendue lors de la mise à jour", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint DELETE pour supprimer le mapping caserne/adresse pour une adresse donnée.
     * <p>
     * URL: {@code DELETE /firestation?address=<adresse_a_supprimer>}
     * </p>
     *
     * @param address L'adresse (chaîne non vide) dont le mapping doit être supprimé.
     *                Requise comme paramètre de requête URL.
     * @return Une {@link ResponseEntity} avec:
     *         <ul>
     *             <li>Code 204 (No Content) si la suppression a réussi.</li>
     *             <li>Code 404 (Not Found) si aucun mapping n'a été trouvé pour cette adresse.</li>
     *             <li>Code 400 (Bad Request) si le paramètre {@code address} est manquant ou invalide.</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @DeleteMapping("/firestation")
    public ResponseEntity<Void> deleteFireStation(@RequestParam String address) { // Validation @RequestParam NotEmpty/NotBlank possible
        // Une validation supplémentaire ici est possible, mais le service la fait déjà
        // if (address == null || address.isBlank()) {
        //     logger.warn("Endpoint DELETE /firestation: Paramètre 'address' manquant ou vide.");
        //     return ResponseEntity.badRequest().build(); // 400
        // }
        try {
            boolean deleted = fireStationService.deleteFireStationMapping(address);

            if (deleted) {
                logger.info("Endpoint DELETE /firestation: Mapping supprimé avec succès pour l'adresse '{}'", address);
                return ResponseEntity.noContent().build(); // 204
            } else {
                // Le service retourne false si l'adresse est invalide OU si non trouvée.
                // On pourrait distinguer les deux cas si le service lançait une exception pour adresse invalide.
                logger.warn("Endpoint DELETE /firestation: Mapping non trouvé pour l'adresse '{}'", address);
                return ResponseEntity.notFound().build(); // 404
            }
        } catch (Exception e) {
            logger.error("Endpoint DELETE /firestation: Erreur interne lors de la suppression pour l'adresse '{}'", address, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint GET pour récupérer la liste complète de tous les mappings caserne/adresse.
     * <p>
     * URL: {@code GET /firestation/all}
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec la {@code List<FireStation>} dans le corps (peut être vide).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @GetMapping("/firestation/all")
    public ResponseEntity<List<FireStation>> getAllFireStations() {
        try {
            List<FireStation> stations = fireStationService.getAllFireStations();
            logger.debug("Endpoint GET /firestation/all: Récupération de {} mappings.", stations.size());
            return ResponseEntity.ok(stations); // 200
        } catch (Exception e) {
            logger.error("Endpoint GET /firestation/all: Erreur interne lors de la récupération", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}