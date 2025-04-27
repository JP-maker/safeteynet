package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST gérant les requêtes HTTP liées aux personnes (Person).
 * <p>
 * Expose des endpoints pour les opérations CRUD de base sur les personnes,
 * ainsi que des endpoints spécifiques pour obtenir des informations agrégées basées sur
 * l'adresse, le nom de famille ou la ville.
 * </p><p>
 * Utilise {@link PersonService} pour déléguer la logique métier.
 * </p>
 */
@RestController
public class PersonController {
    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    private final PersonService personService;

    /**
     * Construit une nouvelle instance de PersonController.
     *
     * @param personService Le service injecté pour gérer la logique métier des personnes.
     */
    @Autowired // Optionnel ici
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * Endpoint GET pour obtenir une liste des enfants et des autres membres du foyer (adultes)
     * résidant à une adresse donnée.
     * <p>
     * URL: {@code GET /childAlert?address=<address>}
     * </p>
     *
     * @param address L'adresse du foyer (chaîne non vide). Requis comme paramètre de requête URL.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link ChildWithFamilyDTO} dans le corps si au moins un enfant est trouvé.</li>
     *             <li>Code 404 (Not Found) si aucune personne n'est trouvée à l'adresse ou si aucun enfant n'y réside.</li>
     *             <li>Code 400 (Bad Request) si le paramètre {@code address} est vide ou manquant.</li>
     *         </ul>
     */
    @GetMapping("/childAlert")
    public ResponseEntity<ChildWithFamilyDTO> getChildrenAndFamilyByAddress(
            @RequestParam("address") String address) {

        logger.info("Requête GET /childAlert reçue avec address={}", address);

        // Validation simple de l'adresse (non vide)
        if (address == null || address.isBlank()) { // Vérifier isBlank() plutôt que isEmpty()
            logger.warn("Paramètre 'address' manquant ou vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service (note: le service fait déjà toUpperCase, pas besoin ici)
        Optional<ChildWithFamilyDTO> result = personService.getChildAndFamilyByAddress(address);

        // Construction de la réponse
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /childAlert?address={}", address);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /childAlert?address={}", address);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint GET pour obtenir la liste des habitants d'une adresse donnée, le numéro de la
     * caserne de pompiers qui la dessert, et leurs informations médicales (âge, médications, allergies).
     * <p>
     * URL: {@code GET /fire?address=<address>}
     * </p>
     *
     * @param address L'adresse recherchée (chaîne non vide). Requise comme paramètre de requête URL.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link FirePersonDTO} dans le corps si au moins une personne (avec dossier médical) est trouvée.</li>
     *             <li>Code 404 (Not Found) si aucune personne (avec dossier médical) n'est trouvée à cette adresse.</li>
     *             <li>Code 400 (Bad Request) si le paramètre {@code address} est vide ou manquant.</li>
     *         </ul>
     */
    @GetMapping("/fire")
    public ResponseEntity<FirePersonDTO> getPersonWithMedicalReportByAddress(
            @RequestParam("address") String address) {

        logger.info("Requête GET /fire reçue avec address={}", address);

        // Validation
        if (address == null || address.isBlank()) {
            logger.warn("Paramètre 'address' manquant ou vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service (le service gère toUpperCase si nécessaire)
        Optional<FirePersonDTO> result = personService.getPersonFireStationAndMedicalReportByAddress(address);

        // Construction de la réponse
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /fire?address={}", address);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /fire?address={}", address);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint GET pour obtenir les informations (adresse, âge, e-mail, antécédents médicaux)
     * de toutes les personnes portant un nom de famille donné.
     * <p>
     * URL: {@code GET /personInfolastName?lastName=<lastName>}
     * </p>
     *
     * @param lastName Le nom de famille recherché (chaîne non vide). Requis comme paramètre de requête URL.
     *                 La recherche est insensible à la casse.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link ListOfPersonInfolastNameDTO} dans le corps si au moins une personne (avec dossier médical) est trouvée.</li>
     *             <li>Code 404 (Not Found) si aucune personne (avec dossier médical) n'est trouvée pour ce nom.</li>
     *             <li>Code 400 (Bad Request) si le paramètre {@code lastName} est manquant ou vide.</li>
     *         </ul>
     */
    @GetMapping("/personInfolastName") // Renommé pour être plus standard
    public ResponseEntity<ListOfPersonInfolastNameDTO> getPersonInfoByLastName( // Renommé aussi pour cohérence
                                                                                @RequestParam("lastName") String lastName) { // @RequestParam "lastName" est plus standard

        logger.info("Requête GET /personInfo reçue avec lastName={}", lastName);

        // Validation
        if (lastName == null || lastName.isBlank()) {
            logger.warn("Paramètre 'lastName' manquant ou vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service (le service gère toUpperCase si nécessaire)
        Optional<ListOfPersonInfolastNameDTO> result = personService.getPersonInfoByLastName(lastName);

        // Construction de la réponse
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /personInfo?lastName={}", lastName);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /personInfo?lastName={}", lastName);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    /**
     * Endpoint GET pour obtenir les adresses e-mail de tous les habitants d'une ville donnée.
     * <p>
     * URL: {@code GET /communityEmail?city=<city>}
     * </p>
     *
     * @param city Le nom de la ville (chaîne non vide). Requis comme paramètre de requête URL.
     *             La recherche est insensible à la casse.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec un {@link CommunityEmailDTO} dans le corps si au moins une personne est trouvée dans la ville.</li>
     *             <li>Code 404 (Not Found) si aucune personne n'est trouvée pour cette ville.</li>
     *             <li>Code 400 (Bad Request) si le paramètre {@code city} est manquant ou vide.</li>
     *         </ul>
     */
    @GetMapping("/communityEmail")
    public ResponseEntity<CommunityEmailDTO> getCommunityEmail(
            @RequestParam("city") String city) {

        logger.info("Requête GET /communityEmail reçue avec city={}", city);

        // Validation
        if (city == null || city.isBlank()) {
            logger.warn("Paramètre 'city' manquant ou vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        // Appel au service (le service gère toUpperCase si nécessaire)
        Optional<CommunityEmailDTO> result = personService.getCommunityEmailByCity(city);

        // Construction de la réponse
        return result.map(dto -> {
                    logger.info("Réponse 200 OK pour /communityEmail?city={}", city);
                    return ResponseEntity.ok(dto); // 200
                })
                .orElseGet(() -> {
                    logger.warn("Réponse 404 Not Found pour /communityEmail?city={}", city);
                    return ResponseEntity.notFound().build(); // 404
                });
    }

    // --- Endpoints CRUD pour /person ---

    /**
     * Endpoint POST pour ajouter une nouvelle personne.
     * <p>
     * URL: {@code POST /person}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link Person}.
     * Exemple: {@code { "firstName":"John", "lastName":"Doe", "address":"123 Main St", ...}}
     * </p>
     *
     * @param person L'objet {@link Person} à créer, désérialisé depuis le corps JSON.
     *               Le prénom et le nom ne doivent pas être nuls ou vides.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 201 (Created) avec l'objet {@link Person} créé dans le corps en cas de succès.</li>
     *             <li>Code 409 (Conflict) si une personne avec le même prénom et nom existe déjà.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (prénom/nom manquants).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PostMapping("/person")
    public ResponseEntity<Person> addPerson(@RequestBody Person person) {
        // Validation basique dans le contrôleur
        if (person == null || person.getFirstName() == null || person.getLastName() == null ||
                person.getFirstName().isBlank() || person.getLastName().isBlank()) {
            logger.warn("Requête POST /person reçue avec prénom ou nom manquant/vide.");
            return ResponseEntity.badRequest().build(); // 400
        }
        try {
            Person createdPerson = personService.addPerson(person);
            logger.info("Endpoint POST /person: Personne créée avec succès: {} {}", createdPerson.getFirstName(), createdPerson.getLastName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPerson); // 201
        } catch (IllegalArgumentException e) {
            // Le service lance cette exception si la personne existe déjà
            logger.warn("Endpoint POST /person: Échec de création - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
        } catch (Exception e) {
            logger.error("Endpoint POST /person: Erreur interne inattendue lors de la création", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint GET pour récupérer la liste de toutes les personnes enregistrées.
     * <p>
     * URL: {@code GET /person}
     * </p>
     *
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec la {@code List<Person>} dans le corps.</li>
     *             <li>Code 204 (No Content) si aucune personne n'est enregistrée.</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @GetMapping("/person")
    public ResponseEntity<List<Person>> getPersons() {
        try {
            List<Person> persons = personService.getAllPersons();
            if (persons.isEmpty()) {
                logger.info("Endpoint GET /person: Aucune personne trouvée.");
                return ResponseEntity.noContent().build(); // 204
            }
            logger.debug("Endpoint GET /person: Récupération de {} personnes.", persons.size());
            return ResponseEntity.ok(persons); // 200
        } catch (Exception e) {
            logger.error("Endpoint GET /person: Erreur interne inattendue lors de la récupération", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint PUT pour mettre à jour les informations d'une personne existante.
     * Ne modifie pas le prénom ni le nom.
     * <p>
     * URL: {@code PUT /person}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link Person}.
     * Le prénom et le nom sont utilisés pour identifier la personne. Les autres champs
     * (adresse, ville, zip, téléphone, email) contiennent les nouvelles valeurs.
     * Exemple: {@code { "firstName":"John", "lastName":"Doe", "address":"456 New Ave", ...}}
     * </p>
     *
     * @param person L'objet {@link Person} contenant les informations de mise à jour.
     *               Prénom et nom sont requis pour l'identification.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec l'objet {@link Person} mis à jour dans le corps si trouvé et mis à jour.</li>
     *             <li>Code 404 (Not Found) si aucune personne n'est trouvée pour ce prénom/nom.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (prénom/nom manquants).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PutMapping("/person")
    public ResponseEntity<Person> updatePerson(@RequestBody Person person) {
        // Validation basique
        if (person == null || person.getFirstName() == null || person.getLastName() == null ||
                person.getFirstName().isBlank() || person.getLastName().isBlank()) {
            logger.warn("Requête PUT /person reçue avec prénom ou nom manquant/vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        try {
            Optional<Person> updatedPersonOpt = personService.updatePerson(person);

            return updatedPersonOpt
                    .map(updatedPerson -> {
                        logger.info("Endpoint PUT /person: Personne mise à jour: {} {}", updatedPerson.getFirstName(), updatedPerson.getLastName());
                        return ResponseEntity.ok(updatedPerson); // 200
                    })
                    .orElseGet(() -> {
                        logger.warn("Endpoint PUT /person: Personne non trouvée pour mise à jour: {} {}", person.getFirstName(), person.getLastName());
                        return ResponseEntity.notFound().build(); // 404
                    });
        } catch (IllegalArgumentException e) {
            // Si le service lançait une exception pour données invalides (autre que prénom/nom null)
            logger.warn("Endpoint PUT /person: Échec de mise à jour - Données invalides : {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // 400
        } catch (Exception e) {
            logger.error("Endpoint PUT /person: Erreur interne inattendue lors de la mise à jour", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint DELETE pour supprimer une personne.
     * <p>
     * URL: {@code DELETE /person?firstName=<prénom>&lastName=<nom>}
     * </p>
     *
     * @param firstName Le prénom de la personne à supprimer (paramètre de requête URL, requis).
     * @param lastName Le nom de famille de la personne à supprimer (paramètre de requête URL, requis).
     * @return Une {@link ResponseEntity<Void>} avec:
     *         <ul>
     *             <li>Code 204 (No Content) si la suppression a réussi.</li>
     *             <li>Code 404 (Not Found) si aucune personne n'est trouvée pour ce prénom/nom.</li>
     *             <li>Code 400 (Bad Request) si les paramètres {@code firstName} ou {@code lastName} sont manquants ou vides.</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @DeleteMapping("/person")
    public ResponseEntity<Void> deletePerson(@RequestParam String firstName, @RequestParam String lastName) {
        // Validation des paramètres
        if (firstName == null || lastName == null || firstName.isBlank() || lastName.isBlank()) {
            logger.warn("Requête DELETE /person reçue avec prénom ou nom manquant/vide.");
            return ResponseEntity.badRequest().build(); // 400
        }

        try {
            boolean deleted = personService.deletePerson(firstName, lastName);

            if (deleted) {
                logger.info("Endpoint DELETE /person: Personne supprimée: {} {}", firstName, lastName);
                return ResponseEntity.noContent().build(); // 204
            } else {
                logger.warn("Endpoint DELETE /person: Personne non trouvée pour suppression: {} {}", firstName, lastName);
                return ResponseEntity.notFound().build(); // 404
            }
        } catch (Exception e) {
            logger.error("Endpoint DELETE /person: Erreur interne lors de la suppression pour {} {}", firstName, lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}