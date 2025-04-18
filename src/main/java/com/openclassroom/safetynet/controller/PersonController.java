package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        Optional<ListOfPersonInfolastNameDTO> result = personService.getPersonInfoByLastName(lastName.toUpperCase());
        return result.map(dto -> {
                    logger.info("getPersonInfolastName - Réponse 200 OK pour le nom={}", lastName);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("getPersonInfolastName - Réponse 404 Not Found le nom={}", lastName);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Cette url doit retourner les adresses mail de tous les habitants de la ville.
     * URL: GET /communityEmail?city=<city>
     *
     * @param city Le nom de la ville.
     * @return ResponseEntity contenant le DTO CommunityEmailDTO ou une réponse 404.
     */
    @GetMapping("/communityEmail")
    public ResponseEntity<CommunityEmailDTO> getCommunityEmail(
            @RequestParam("city") String city) {
        logger.info("Requête reçue pour /communityEmail avec la ville={}", city);

        Optional<CommunityEmailDTO> result = personService.getCommunityEmailByCity(city.toUpperCase());

        return result.map(dto -> {
                    logger.info("getCommunityEmail - Réponse 200 OK pour la ville={}", city);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("getCommunityEmail - Réponse 404 Not Found la ville={}", city);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint pour ajouter une nouvelle personne.
     * HTTP POST /person
     * Corps de la requête: JSON représentant la personne à ajouter.
     */
    @PostMapping("/person")
    public ResponseEntity<Person> addPerson(@RequestBody Person person) {
        if (person.getFirstName() == null || person.getLastName() == null ||
                person.getFirstName().isBlank() || person.getLastName().isBlank()) {
            return ResponseEntity.badRequest().build(); // Mauvaise requête si nom/prénom manquant
        }
        try {
            Person createdPerson = personService.addPerson(person);
            // Retourne 201 Created avec la personne créée dans le corps
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPerson);
        } catch (IllegalArgumentException e) {
            // Gère le cas où la personne existe déjà (levé par le service)
            // Retourne 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            // Autre erreur potentielle
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint pour récupérer toutes les personnes.
     * HTTP GET /person
     */
    @GetMapping("/person")
    public ResponseEntity<List<Person>> getPersons() {
        List<Person> persons = personService.getAllPersons();
        if (persons.isEmpty()) {
            return ResponseEntity.noContent().build(); // Retourne 204 No Content si aucune personne trouvée
        }
        return ResponseEntity.ok(persons); // Retourne 200 OK avec la liste des personnes
    }

    /**
     * Endpoint pour mettre à jour une personne existante.
     * HTTP PUT /person
     * Corps de la requête: JSON représentant la personne avec les informations mises à jour.
     * Le prénom et le nom dans le corps identifient la personne à mettre à jour.
     */
    @PutMapping("/person")
    public ResponseEntity<Person> updatePerson(@RequestBody Person person) {
        if (person.getFirstName() == null || person.getLastName() == null ||
                person.getFirstName().isBlank() || person.getLastName().isBlank()) {
            return ResponseEntity.badRequest().build(); // Mauvaise requête si nom/prénom manquant
        }

        Optional<Person> updatedPersonOpt = personService.updatePerson(person);

        return updatedPersonOpt
                .map(ResponseEntity::ok) // Si présent, retourne 200 OK avec la personne mise à jour
                .orElseGet(() -> ResponseEntity.notFound().build()); // Sinon, retourne 404 Not Found
    }

    /**
     * Endpoint pour supprimer une personne.
     * HTTP DELETE /person?firstName=<prénom>&lastName=<nom>
     * Paramètres de requête: firstName et lastName pour identifier la personne.
     */
    @DeleteMapping("/person")
    public ResponseEntity<Void> deletePerson(@RequestParam String firstName, @RequestParam String lastName) {
        if (firstName == null || lastName == null || firstName.isBlank() || lastName.isBlank()) {
            return ResponseEntity.badRequest().build(); // Mauvaise requête si nom/prénom manquant
        }

        boolean deleted = personService.deletePerson(firstName, lastName);

        if (deleted) {
            // Retourne 204 No Content si la suppression a réussi
            // Ou 200 OK si vous préférez, 204 est plus standard pour DELETE réussi sans corps de retour.
            return ResponseEntity.noContent().build();
        } else {
            // Retourne 404 Not Found si la personne n'a pas été trouvée
            return ResponseEntity.notFound().build();
        }
    }
}
