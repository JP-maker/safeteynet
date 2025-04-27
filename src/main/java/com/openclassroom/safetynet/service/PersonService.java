package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.FireStation; // Import non utilisé directement mais via repo
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.repository.FireStationRepository;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import com.openclassroom.safetynet.repository.PersonRepository;
import com.openclassroom.safetynet.utils.AgeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections; // Ajout import manquant
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Ajout import manquant

/**
 * Service gérant la logique métier pour les entités Person et les requêtes associées.
 * <p>
 * Ce service interagit avec les repositories {@link PersonRepository}, {@link MedicalRecordRepository},
 * et {@link FireStationRepository} pour effectuer des opérations CRUD sur les personnes et
 * pour agréger des informations complexes demandées par certains endpoints (ex: enfants par adresse,
 * informations par nom, e-mails par ville, personnes par adresse avec détails médicaux et caserne).
 * Il utilise {@link AgeCalculator} pour déterminer l'âge des personnes à partir de leur dossier médical.
 * </p>
 */
@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final FireStationRepository fireStationRepository;

    /**
     * Construit une nouvelle instance de PersonService avec les dépendances des repositories.
     *
     * @param personRepository        Le repository pour accéder aux données des personnes.
     * @param medicalRecordRepository Le repository pour accéder aux données des dossiers médicaux.
     * @param fireStationRepository   Le repository pour accéder aux données des casernes.
     */
    @Autowired // Optionnel ici mais bonne pratique
    public PersonService(PersonRepository personRepository,
                         MedicalRecordRepository medicalRecordRepository,
                         FireStationRepository fireStationRepository) {
        this.personRepository = personRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.fireStationRepository = fireStationRepository;
    }

    /**
     * Récupère une liste des enfants (âge <= {@link ConfigData#CHILD_AGE_THRESHOLD})
     * et des autres membres de la famille (adultes) résidant à une adresse spécifique.
     * <p>
     * L'âge est déterminé via le {@link MedicalRecordRepository} et {@link AgeCalculator}.
     * Seules les personnes avec un dossier médical valide permettant de calculer l'âge
     * sont classées comme enfant ou adulte. Les autres sont ignorées pour la classification
     * mais peuvent apparaître dans la liste générale des personnes si la logique du DTO le permettait
     * (actuellement, le DTO ne contient que les enfants et les adultes classifiés).
     * </p>
     *
     * @param address L'adresse pour laquelle rechercher les enfants et la famille.
     * @return Un {@link Optional<ChildWithFamilyDTO>} contenant la liste des enfants et des adultes
     *         si au moins un enfant est trouvé à cette adresse.
     *         Retourne {@link Optional#empty()} si aucune personne n'est trouvée à l'adresse
     *         ou si aucune des personnes trouvées (avec dossier médical) n'est un enfant.
     */
    public Optional<ChildWithFamilyDTO> getChildAndFamilyByAddress(String address) {
        logger.debug("Recherche des enfants et de la famille pour l'adresse: {}", address);

        // Vérification initiale de l'adresse (optionnel mais recommandé)
        if (address == null || address.isBlank()) {
            logger.warn("Adresse fournie invalide (null ou vide).");
            return Optional.empty();
        }

        List<String> addresses = Collections.singletonList(address); // Utiliser une liste immuable
        List<ChildInfoDTO> childInfos = new ArrayList<>();
        List<PersonInfoDTO> familyInfos = new ArrayList<>(); // Renommé pour clarté

        // 1. Trouver toutes les personnes vivant à cette adresse
        List<Person> peopleAtAddress = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées à l'adresse '{}'", peopleAtAddress.size(), address);

        if (peopleAtAddress.isEmpty()) {
            logger.warn("Aucune personne trouvée à l'adresse {}", address);
            return Optional.empty(); // Pas la peine de continuer si personne n'habite là
        }

        // 2. Pour chaque personne, essayer de calculer l'âge et classer
        for (Person person : peopleAtAddress) {
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());
                    if (age <= ConfigData.CHILD_AGE_THRESHOLD) {
                        // C'est un enfant
                        childInfos.add(new ChildInfoDTO(
                                person.getFirstName(),
                                person.getLastName(),
                                age
                        ));
                    } else {
                        // C'est un adulte (ou autre membre de la famille)
                        familyInfos.add(new PersonInfoDTO(
                                person.getFirstName(),
                                person.getLastName(),
                                person.getAddress(), // L'adresse est la même pour tous ici
                                person.getPhone()
                        ));
                    }
                } catch (Exception e) {
                    logger.error("Impossible de calculer l'âge pour {} {} (date: {}): {}",
                            person.getFirstName(), person.getLastName(), medicalRecord.getBirthdate(), e.getMessage());
                    // On ne classe pas cette personne si l'âge ne peut être calculé
                }
            } else {
                logger.warn("Aucun dossier médical trouvé pour {} {}, impossible de déterminer l'âge et de classer.",
                        person.getFirstName(), person.getLastName());
                // Ajouter cette personne à la liste des membres de famille si nécessaire?
                // familyInfos.add(new PersonInfoDTO(...)); // Décision métier à prendre
            }
        }

        // 3. Condition de retour: uniquement s'il y a des enfants
        if (childInfos.isEmpty()) {
            logger.warn("Aucun enfant (avec âge calculable) trouvé pour l'adresse {}", address);
            return Optional.empty();
        } else {
            ChildWithFamilyDTO result = new ChildWithFamilyDTO(childInfos, familyInfos);
            logger.info("Résultat pour l'adresse {}: {} enfants, {} autres membres trouvés",
                    address, childInfos.size(), familyInfos.size());
            return Optional.of(result);
        }
    }

    /**
     * Récupère une liste des personnes résidant à une adresse donnée, avec pour chacune
     * son nom, téléphone, numéro de caserne, âge, médications et allergies.
     * <p>
     * Nécessite des informations des trois repositories (Person, FireStation, MedicalRecord).
     * L'âge est calculé via {@link AgeCalculator}. Si le dossier médical manque ou si l'âge
     * ne peut être calculé, la personne n'est pas incluse dans le résultat final.
     * </p>
     *
     * @param address L'adresse pour laquelle rechercher les informations.
     * @return Un {@link Optional<FirePersonDTO>} contenant la liste des personnes avec leurs informations
     *         si au moins une personne (avec dossier médical valide) est trouvée à cette adresse.
     *         Retourne {@link Optional#empty()} si aucune personne (avec dossier médical valide)
     *         n'est trouvée à l'adresse.
     */
    public Optional<FirePersonDTO> getPersonFireStationAndMedicalReportByAddress(String address) {
        logger.debug("Recherche des personnes, caserne et infos médicales pour l'adresse: {}", address);

        // Vérification initiale de l'adresse
        if (address == null || address.isBlank()) {
            logger.warn("Adresse fournie invalide (null ou vide).");
            return Optional.empty();
        }

        List<String> addresses = Collections.singletonList(address);
        List<PersonWithMedicalRecordDTO> personInfosComplete = new ArrayList<>(); // Renommé pour clarté

        // 1. Trouver le numéro de la caserne pour cette adresse (une seule fois)
        String fireStationNumber = fireStationRepository.findStationNumberByAddress(address);
        if (fireStationNumber == null) {
            logger.warn("Aucune caserne trouvée pour l'adresse '{}'. Le numéro de caserne sera null dans le résultat.", address);
        } else {
            logger.debug("Caserne trouvée pour l'adresse '{}': {}", address, fireStationNumber);
        }


        // 2. Trouver toutes les personnes vivant à cette adresse
        List<Person> peopleAtAddress = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées à l'adresse '{}'", peopleAtAddress.size(), address);

        if (peopleAtAddress.isEmpty()) {
            logger.warn("Aucune personne trouvée à l'adresse {}", address);
            return Optional.empty(); // Retourner vide si personne n'habite là
        }

        // 3. Pour chaque personne, trouver le dossier médical, calculer l'âge et créer le DTO
        for (Person person : peopleAtAddress) {
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );
            // logger.debug("Dossier médical pour {} {}: {}", person.getFirstName(), person.getLastName(), medicalRecordOpt.isPresent() ? "Trouvé" : "Non trouvé");

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());
                    // Créer le DTO uniquement si l'âge a pu être calculé
                    personInfosComplete.add(new PersonWithMedicalRecordDTO(
                            // Note: Le DTO attend lastName, phone, fireStationNumber, age, medications, allergies
                            person.getLastName(), // Utiliser lastName comme demandé par le DTO
                            person.getPhone(),
                            fireStationNumber, // Numéro trouvé précédemment
                            age,
                            medicalRecord.getMedications(),
                            medicalRecord.getAllergies()
                    ));
                } catch (Exception e) {
                    logger.error("getPersonFireStationAndMedicalReportByAddress - Impossible de calculer l'âge pour {} {} (date: {}): {}",
                            person.getFirstName(), person.getLastName(), medicalRecord.getBirthdate(), e.getMessage());
                    // Ne pas ajouter la personne au résultat si l'âge est invalide
                }
            } else {
                logger.warn("getPersonFireStationAndMedicalReportByAddress - Aucun dossier médical trouvé pour {} {}, personne non incluse dans le résultat.",
                        person.getFirstName(), person.getLastName());
                // Ne pas ajouter la personne si pas de dossier médical
            }
        } // Fin boucle sur les personnes

        // 4. Condition de retour
        if (personInfosComplete.isEmpty()) {
            logger.warn("Aucun résident avec dossier médical valide trouvé pour l'adresse {}", address);
            return Optional.empty();
        } else {
            FirePersonDTO result = new FirePersonDTO(personInfosComplete); // Supposant que FirePersonDTO prend List<PersonWithMedicalRecordDTO>
            logger.info("Résultat pour l'adresse {}: {} résidents avec détails médicaux trouvés.",
                    address, personInfosComplete.size());
            return Optional.of(result);
        }
    }

    /**
     * Recherche toutes les personnes portant un nom de famille donné et retourne leurs
     * informations détaillées, y compris adresse, âge, e-mail, médications et allergies.
     * <p>
     * L'âge est calculé à partir du dossier médical. Les personnes sans dossier médical
     * ou pour lesquelles l'âge ne peut être calculé ne sont pas incluses dans la liste résultante.
     * La recherche du nom de famille est insensible à la casse (géré par le repository).
     * </p>
     *
     * @param lastName Le nom de famille à rechercher.
     * @return Un {@link Optional<ListOfPersonInfolastNameDTO>} contenant le nom de famille recherché
     *         et la liste des personnes correspondantes avec leurs informations, si au moins une
     *         personne (avec dossier médical valide) est trouvée.
     *         Retourne {@link Optional#empty()} si aucune personne (avec dossier médical valide)
     *         n'est trouvée pour ce nom de famille.
     */
    public Optional<ListOfPersonInfolastNameDTO> getPersonInfoByLastName(String lastName) {
        logger.debug("Recherche des informations pour le nom de famille: {}", lastName);

        // Vérification initiale
        if (lastName == null || lastName.isBlank()) {
            logger.warn("Nom de famille fourni invalide (null ou vide).");
            return Optional.empty();
        }

        // 1. Trouver toutes les personnes avec ce nom (insensible à la casse via repo)
        List<Person> peopleWithLastName = personRepository.findByLastName(lastName);
        logger.debug("{} personnes trouvées pour le nom {}", peopleWithLastName.size(), lastName);

        if (peopleWithLastName.isEmpty()) {
            logger.warn("Aucune personne trouvée pour le nom {}", lastName);
            return Optional.empty();
        }

        List<PersonInfolastNameDTO> personsInfoComplete = new ArrayList<>(); // Renommé

        // 2. Pour chaque personne, trouver dossier médical, calculer âge et créer DTO
        for (Person person : peopleWithLastName) {
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );
            // logger.debug("Dossier médical pour {} {}: {}", person.getFirstName(), person.getLastName(), medicalRecordOpt.isPresent() ? "Trouvé" : "Non trouvé");

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());
                    // Créer le DTO uniquement si l'âge a pu être calculé
                    personsInfoComplete.add(new PersonInfolastNameDTO(
                            // Note: Le DTO attend address, age, email, medications, allergies
                            person.getAddress(),
                            age,
                            person.getEmail(),
                            medicalRecord.getMedications(),
                            medicalRecord.getAllergies()
                    ));
                } catch (Exception e) {
                    logger.error("getPersonInfoByLastName - Impossible de calculer l'âge pour {} {} (date: {}): {}",
                            person.getFirstName(), person.getLastName(), medicalRecord.getBirthdate(), e.getMessage());
                    // Ne pas ajouter si âge invalide
                }
            } else {
                logger.warn("getPersonInfoByLastName - Aucun dossier médical trouvé pour {} {}, personne non incluse dans le résultat.",
                        person.getFirstName(), person.getLastName());
                // Ne pas ajouter si pas de dossier médical
            }
        } // Fin boucle personnes

        // 3. Condition de retour
        if (personsInfoComplete.isEmpty()) {
            logger.warn("Aucun résident avec dossier médical valide trouvé pour le nom {}", lastName);
            return Optional.empty();
        } else {
            ListOfPersonInfolastNameDTO result = new ListOfPersonInfolastNameDTO(lastName, personsInfoComplete);
            logger.info("Résultat pour le nom {}: {} résidents avec détails trouvés.", lastName, personsInfoComplete.size());
            return Optional.of(result);
        }
    }

    /**
     * Récupère une liste des adresses e-mail de tous les résidents d'une ville donnée.
     * La recherche de la ville est insensible à la casse (géré par le repository).
     *
     * @param city La ville pour laquelle récupérer les adresses e-mail.
     * @return Un {@link Optional<CommunityEmailDTO>} contenant la ville et la liste des e-mails
     *         si au moins une personne est trouvée dans cette ville.
     *         Retourne {@link Optional#empty()} si aucune personne n'est trouvée pour cette ville.
     */
    public Optional<CommunityEmailDTO> getCommunityEmailByCity(String city) {
        logger.debug("Recherche des adresses e-mail pour la ville: {}", city);

        // Vérification initiale
        if (city == null || city.isBlank()) {
            logger.warn("Ville fournie invalide (null ou vide).");
            return Optional.empty();
        }

        // 1. Trouver toutes les personnes dans cette ville (insensible à la casse via repo)
        List<Person> peopleInCity = personRepository.findByCity(city);
        logger.debug("{} personnes trouvées dans la ville {}", peopleInCity.size(), city);

        if (peopleInCity.isEmpty()) {
            logger.warn("Aucune personne trouvée pour la ville {}", city);
            return Optional.empty();
        }

        // 2. Extraire les e-mails
        List<String> emails = peopleInCity.stream()
                .map(Person::getEmail)
                // .distinct() // Optionnel: si on veut des emails uniques
                .collect(Collectors.toList());

        // 3. Créer le DTO résultat
        CommunityEmailDTO result = new CommunityEmailDTO(city, emails);
        logger.info("Résultat pour la ville {}: {} adresses e-mail trouvées.", city, emails.size());

        return Optional.of(result);
    }

    /**
     * Récupère la liste complète de toutes les personnes enregistrées.
     * Simple délégation à la méthode findAll du repository.
     *
     * @return Une {@code List<Person>} contenant toutes les personnes. Peut être vide.
     */
    public List<Person> getAllPersons() {
        logger.debug("Récupération de toutes les personnes");
        return personRepository.findAll();
    }

    /**
     * Ajoute une nouvelle personne au système après avoir vérifié qu'aucune personne
     * avec le même prénom et nom n'existe déjà (vérification insensible à la casse).
     *
     * @param person L'objet {@link Person} à ajouter. Les champs firstName et lastName
     *               ne doivent pas être nuls ou vides.
     * @return L'objet {@link Person} tel qu'il a été sauvegardé par le repository.
     * @throws IllegalArgumentException Si une personne avec le même prénom et nom existe déjà,
     *                                  ou si l'objet Person ou ses identifiants sont invalides.
     */
    public Person addPerson(Person person) {
        logger.debug("Tentative d'ajout d'une personne: {}", person);
        // Validation basique (le repository peut avoir des validations plus fines)
        if (person == null || person.getFirstName() == null || person.getFirstName().isBlank()
                || person.getLastName() == null || person.getLastName().isBlank()) {
            throw new IllegalArgumentException("Les informations de la personne (prénom, nom) sont invalides.");
        }

        // Vérification de l'existence (insensible à la casse via repo)
        if (personRepository.existsById(person.getFirstName(), person.getLastName())) {
            String errorMsg = "Une personne nommée " + person.getFirstName() + " " + person.getLastName() + " existe déjà.";
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg); // Sera traité comme 409 Conflict
        } else {
            logger.info("Ajout de la personne : {}", person);
            return personRepository.save(person); // Le repo gère la sauvegarde
        }
    }

    /**
     * Met à jour les informations modifiables d'une personne existante.
     * <p>
     * La personne est identifiée par son prénom et son nom (insensible à la casse via le repository).
     * Seuls les champs suivants sont mis à jour : adresse, ville, code postal, téléphone, e-mail.
     * Le prénom et le nom ne sont pas modifiés par cette méthode.
     * </p>
     *
     * @param personToUpdate Un objet {@link Person} contenant le prénom et le nom pour identifier
     *                       l'enregistrement existant, ainsi que les nouvelles valeurs pour
     *                       l'adresse, la ville, le code postal, le téléphone et l'e-mail.
     * @return Un {@link Optional<Person>} contenant la personne mise à jour si elle a été trouvée,
     *         sinon {@link Optional#empty()}.
     */
    public Optional<Person> updatePerson(Person personToUpdate) {
        logger.debug("Tentative de mise à jour pour la personne: {} {}",
                personToUpdate != null ? personToUpdate.getFirstName() : "null",
                personToUpdate != null ? personToUpdate.getLastName() : "null");

        // Validation de l'entrée
        if (personToUpdate == null || personToUpdate.getFirstName() == null || personToUpdate.getFirstName().isBlank()
                || personToUpdate.getLastName() == null || personToUpdate.getLastName().isBlank()) {
            logger.error("Données invalides fournies pour la mise à jour.");
            // On pourrait lancer une IllegalArgumentException ici, mais retourner Optional.empty est aussi une option
            // si l'on considère qu'une requête invalide ne peut pas trouver de personne.
            // Pour être cohérent avec le contrôleur qui attend une exception pour 400:
            throw new IllegalArgumentException("Prénom et nom sont requis pour identifier la personne à mettre à jour.");
        }


        // Recherche insensible à la casse via repo
        Optional<Person> existingPersonOpt = personRepository.findByFirstNameAndLastName(
                personToUpdate.getFirstName(),
                personToUpdate.getLastName()
        );

        if (existingPersonOpt.isPresent()) {
            Person existingPerson = existingPersonOpt.get();
            logger.info("Personne trouvée. Mise à jour des champs pour: {} {}", existingPerson.getFirstName(), existingPerson.getLastName());

            // Appliquer les mises à jour
            existingPerson.setAddress(personToUpdate.getAddress());
            existingPerson.setCity(personToUpdate.getCity());
            existingPerson.setZip(personToUpdate.getZip());
            existingPerson.setPhone(personToUpdate.getPhone());
            existingPerson.setEmail(personToUpdate.getEmail());

            // Sauvegarder l'entité mise à jour
            Person savedPerson = personRepository.save(existingPerson);
            return Optional.of(savedPerson);
        } else {
            logger.warn("Personne non trouvée pour la mise à jour: {} {}",
                    personToUpdate.getFirstName(), personToUpdate.getLastName());
            return Optional.empty();
        }
    }

    /**
     * Supprime une personne du système, identifiée par son prénom et son nom.
     * La suppression est effectuée de manière insensible à la casse pour le prénom et le nom.
     *
     * @param firstName Le prénom de la personne à supprimer.
     * @param lastName Le nom de famille de la personne à supprimer.
     * @return {@code true} si la personne a été trouvée et supprimée avec succès,
     *         {@code false} sinon (y compris si les noms fournis sont nuls ou vides).
     */
    public boolean deletePerson(String firstName, String lastName) {
        logger.debug("Tentative de suppression de la personne: {} {}", firstName, lastName);
        // La validation null/blank est déjà dans le repo, mais on peut la dupliquer pour la clarté
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            logger.warn("Tentative de suppression avec prénom ou nom invalide.");
            return false;
        }
        // Délégation au repository qui gère la logique (insensible à la casse) et la sauvegarde
        boolean deleted = personRepository.deleteByFirstNameAndLastName(firstName, lastName);
        if(deleted) {
            logger.info("Personne {} {} supprimée avec succès.", firstName, lastName);
        } else {
            logger.warn("Personne {} {} non trouvée pour suppression.", firstName, lastName);
        }
        return deleted;
    }
}