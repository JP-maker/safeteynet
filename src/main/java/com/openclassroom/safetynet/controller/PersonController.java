package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class PersonController {
    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * Endpoint pour obtenir une liste d'enfants avec leurs ages et les autres membres de la famille pour une adresse.
     * URL: GET /childAlert?address=<address>
     *
     * @param address L'adresse de la maison.
     * @return ResponseEntity contenant le DTO ChildWithFamilyDTO ou une réponse 404.
     */
    @GetMapping("/childAlert")
    public ResponseEntity<ChildWithFamilyDTO> getChildrenAndFamilyByAddress(
            @RequestParam("address") String address) {

        logger.info("Requête reçue pour /childAlert avec adresse={}", address);

        if (address.isEmpty()) {
            logger.warn("L'adresse recue est vide: {}", address);
            // Retourner Bad Request si le numéro n'est pas valide logiquement
            return ResponseEntity.badRequest().build();
        }

        Optional<ChildWithFamilyDTO> result = personService.getChildAndFamilyByAddress(address.toUpperCase());

        // Si le service retourne un résultat, renvoyer 200 OK avec les données
        // Si le service retourne Optional.empty (station non trouvée), renvoyer 404 Not Found
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour adresse={}", address);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour adresse={}", address);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint pour obtenir la liste des habitants vivant à l’adresse donnée ainsi que le numéro de la caserne de pompiers la desservant.
     * URL: GET /fire?address=<address>
     *
     * @param address L'adresse de la maison.
     * @return ResponseEntity contenant le DTO PersonWithMedicalReportDTO ou une réponse 404.
     */
    @GetMapping("/fire")
    public ResponseEntity<FirePersonDTO> getPersonWithMedicalReportByAddress(
            @RequestParam("address") String address) {

        logger.info("Requête reçue pour /fire avec adresse={}", address);

        if (address.isEmpty()) {
            logger.warn("L'adresse recue est vide: {}", address);
            // Retourner Bad Request si le numéro n'est pas valide logiquement
            return ResponseEntity.badRequest().build();
        }

        Optional<FirePersonDTO> result = personService.getPersonFireStationAndMedicalReportByAddress(address.toUpperCase());

        // Si le service retourne un résultat, renvoyer 200 OK avec les données
        // Si le service retourne Optional.empty (station non trouvée), renvoyer 404 Not Found
        return result.map(dto -> {
                    logger.info("getPersonWithMedicalReportByAddress - Réponse 200 OK pour adresse={}", address);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("getPersonWithMedicalReportByAddress - Réponse 404 Not Found pour adresse={}", address);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Cette url doit retourner le nom, l'adresse, l'âge, l'adresse mail et les antécédents médicaux (médicaments, posologie et allergies) de chaque habitant. Si plusieurs personnes portent le même nom, elles doivent toutes apparaître
     * URL: GET /personInfolastName?lastname=<lastName>
     *
     * @param lastname Le nom de la personne.
     * @return ResponseEntity contenant le DTO PersonsWithMedicalReportDTO ou une réponse 404.
     */
    @GetMapping("/personInfolastName")
    public ResponseEntity<ListOfPersonInfolastNameDTO> getPersonInfolastName(
            @RequestParam("lastname") String lastName) {
        logger.info("Requête reçue pour /personInfolastName avec lastname={}", lastName);


        Optional<ListOfPersonInfolastNameDTO> result = null;
        return result.map(dto -> {
                    logger.info("getPersonWithMedicalReportByAddress - Réponse 200 OK pour le nom={}", lastName);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("getPersonWithMedicalReportByAddress - Réponse 404 Not Found le nom={}", lastName);
                    return ResponseEntity.notFound().build();
                });
    }
}
