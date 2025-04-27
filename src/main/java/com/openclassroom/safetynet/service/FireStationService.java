package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.FireStation;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Ajout import manquant

import static java.lang.Integer.parseInt; // Utilisation de parseInt

/**
 * Service gérant la logique métier liée aux casernes de pompiers (FireStations).
 * <p>
 * Ce service interagit avec les repositories {@link FireStationRepository},
 * {@link PersonRepository}, et {@link MedicalRecordRepository} pour agréger des données
 * nécessaires à certains endpoints spécifiques (couverture de station, alertes téléphoniques,
 * informations pour inondations) et pour gérer les opérations CRUD de base sur les mappings
 * adresse/station. Il utilise également {@link PersonService} pour certaines opérations déléguées.
 * </p>
 */
@Service
public class FireStationService {

    private static final Logger logger = LoggerFactory.getLogger(FireStationService.class);

    private final FireStationRepository fireStationRepository;
    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PersonService personService; // Service utilisé pour certaines logiques Personne

    /**
     * Construit une nouvelle instance de FireStationService avec les dépendances nécessaires.
     *
     * @param fireStationRepository   Le repository pour accéder aux données des casernes.
     * @param personRepository        Le repository pour accéder aux données des personnes.
     * @param medicalRecordRepository Le repository pour accéder aux données des dossiers médicaux.
     * @param personService           Le service pour accéder à des logiques métier liées aux personnes.
     */
    @Autowired // @Autowired est optionnel sur les constructeurs uniques mais bonne pratique de le laisser
    public FireStationService(FireStationRepository fireStationRepository,
                              PersonRepository personRepository,
                              MedicalRecordRepository medicalRecordRepository,
                              PersonService personService) {
        this.fireStationRepository = fireStationRepository;
        this.personRepository = personRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.personService = personService;
    }

    /**
     * Récupère une liste de personnes couvertes par le numéro de station spécifié,
     * ainsi qu'un décompte des adultes et des enfants parmi elles.
     * L'âge est déterminé à partir des dossiers médicaux associés.
     *
     * @param stationNumber Le numéro de la station de pompiers à rechercher.
     * @return Un {@link Optional} contenant un {@link FireStationCoverageDTO} si la station
     *         couvre au moins une adresse, même si aucune personne n'y réside.
     *         Retourne {@link Optional#empty()} si le numéro de station ne correspond à aucune adresse.
     */
    public Optional<FireStationCoverageDTO> getPeopleCoveredByStation(int stationNumber) {
        logger.debug("Recherche des adresses pour la station numéro: {}", stationNumber);

        // 1. Trouver les adresses couvertes par la station
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            return Optional.empty(); // Si la station ne couvre aucune adresse, retourner vide
        }
        logger.debug("Adresses trouvées pour la station {}: {}", stationNumber, addresses);

        // 2. Trouver toutes les personnes vivant à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);

        // 3. Préparer les listes et compteurs
        List<PersonInfoDTO> personInfos = new ArrayList<>(); // Liste pour le DTO final
        int adultCount = 0;
        int childCount = 0;

        // 4. Itérer sur les personnes trouvées pour collecter infos et calculer âges/compteurs
        for (Person person : peopleAtAddresses) {
            // Ajouter les informations de base de la personne au DTO
            personInfos.add(new PersonInfoDTO(
                    person.getFirstName(),
                    person.getLastName(),
                    person.getAddress(),
                    person.getPhone()
            ));

            // Trouver le dossier médical pour calculer l'âge
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );

            if (medicalRecordOpt.isPresent()) {
                try {
                    // Calculer l'âge et incrémenter les compteurs
                    int age = AgeCalculator.calculateAge(medicalRecordOpt.get().getBirthdate());
                    if (age <= ConfigData.CHILD_AGE_THRESHOLD) {
                        childCount++;
                    } else {
                        adultCount++;
                    }
                } catch (Exception e) {
                    // Logguer l'erreur si le calcul d'âge échoue mais continuer
                    logger.error("Erreur de calcul d'âge pour {} {}: {}", person.getFirstName(), person.getLastName(), e.getMessage());
                    // Considérer cette personne comme un adulte par défaut? Ou ignorer le comptage? Ici, on l'ignore.
                }
            } else {
                // Logguer si pas de dossier médical, la personne ne sera pas comptée comme enfant/adulte.
                logger.warn("Pas de dossier médical trouvé pour {} {}, impossible de déterminer l'âge pour le comptage.",
                        person.getFirstName(), person.getLastName());
            }
        }

        // 5. Créer le DTO de résultat
        FireStationCoverageDTO result = new FireStationCoverageDTO(personInfos, adultCount, childCount);
        logger.info("Résultat pour la station {}: {} personnes listées, {} adultes comptés, {} enfants comptés",
                stationNumber, personInfos.size(), adultCount, childCount);

        // Retourner le résultat encapsulé dans un Optional (toujours présent si des adresses ont été trouvées)
        return Optional.of(result);
    }

    /**
     * Récupère une liste des numéros de téléphone de toutes les personnes résidant
     * aux adresses couvertes par le numéro de station spécifié.
     *
     * @param stationNumber Le numéro de la station de pompiers.
     * @return Un {@link Optional} contenant un {@link PhoneAlertDTO} avec la liste des numéros
     *         (peut être vide si personne ne réside aux adresses couvertes).
     *         Retourne {@link Optional#empty()} si le numéro de station ne correspond à aucune adresse.
     */
    public Optional<PhoneAlertDTO> getPhoneNumberByStation(int stationNumber) {
        logger.debug("Recherche des téléphones pour la station numéro: {}", stationNumber);

        // 1. Trouver les adresses couvertes par la station
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            return Optional.empty();
        }
        logger.debug("Adresses trouvées pour la station {}: {}", stationNumber, addresses);

        // 2. Trouver les personnes à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);

        // 3. Extraire les numéros de téléphone (distincts ne sont pas requis par l'énoncé ici)
        List<String> phoneNumbers = peopleAtAddresses.stream()
                .map(Person::getPhone)
                .collect(Collectors.toList()); // Collecte tous les numéros, y compris les doublons s'il y en a

        // 4. Créer le DTO résultat
        PhoneAlertDTO result = new PhoneAlertDTO(phoneNumbers);
        logger.info("Station {}: {} numéros de téléphone récupérés", stationNumber, phoneNumbers.size());

        // Retourner le DTO encapsulé (toujours présent si adresses trouvées)
        return Optional.of(result);
    }

    /**
     * Récupère une liste de foyers regroupés par adresse pour une liste donnée de numéros de station.
     * Pour chaque personne dans un foyer, inclut le nom, le téléphone, l'âge et les antécédents médicaux.
     * Le résultat est structuré par numéro de station, puis par adresse.
     *
     * @param stationNumbers Une liste de numéros de station (sous forme de String).
     * @return Un {@link Optional} contenant un {@link ListOfAddressWithListOfPersonWithMedicalRecordDTO}.
     *         Note: Dans l'implémentation actuelle, cet Optional n'est jamais vide, même si aucune
     *         station ou personne n'est trouvée ; la liste interne sera simplement vide.
     */
    public Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> getListOfPersonsWithMedicalRecordsByListOfFireStation(List<String> stationNumbers) {
        logger.info("Recherche des foyers pour les stations: {}", stationNumbers);

        // Structure principale du DTO de retour
        ListOfAddressWithListOfPersonWithMedicalRecordDTO resultContainer = new ListOfAddressWithListOfPersonWithMedicalRecordDTO();
        // Liste qui contiendra les données pour chaque station demandée
        List<FireStationAddressWithListOfPersonWithMedicalRecordDTO> fireStationDataList = new ArrayList<>();

        // Itérer sur chaque numéro de station demandé
        for (String stationNumber : stationNumbers) {
            // Liste pour stocker les données des adresses pour la station courante
            List<AddressWithListOfPersonWithMedicalRecordDTO> addressDataForStation = new ArrayList<>();
            int stationNumInt;
            try {
                stationNumInt = parseInt(stationNumber); // Convertir en entier pour la recherche d'adresses
            } catch (NumberFormatException e) {
                logger.warn("Numéro de station invalide '{}' ignoré.", stationNumber);
                continue; // Passer au numéro de station suivant
            }

            // 1. Trouver les adresses pour la station courante
            List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumInt);
            logger.debug("Adresses trouvées pour la station {}: {}", stationNumber, addresses);

            // 2. Pour chaque adresse, récupérer les personnes et leurs infos médicales
            for (String address : addresses) {
                // Utilisation du service PersonService pour obtenir le DTO pré-formaté par adresse
                // (Cette méthode contient déjà la logique pour récupérer personnes, station, et MR)
                Optional<FirePersonDTO> personsAtAddressOpt = personService.getPersonFireStationAndMedicalReportByAddress(address);

                // Si des personnes sont trouvées pour cette adresse
                if (personsAtAddressOpt.isPresent() && !personsAtAddressOpt.get().getPersons().isEmpty()) {
                    // Créer le DTO pour cette adresse et l'ajouter à la liste de la station courante
                    addressDataForStation.add(new AddressWithListOfPersonWithMedicalRecordDTO(
                            address,
                            personsAtAddressOpt.get().getPersons() // Récupère la liste de PersonWithMedicalRecordDTO
                    ));
                    logger.debug("Adresse {}: {} personnes avec infos médicales récupérées", address, personsAtAddressOpt.get().getPersons().size());
                } else {
                    logger.debug("Adresse {}: Aucune personne avec infos médicales trouvée.", address);
                }
            } // Fin boucle sur les adresses

            // 3. Créer l'objet pour la station courante et l'ajouter à la liste globale
            // N'ajoute la station que si elle dessert des adresses avec des personnes
            if (!addressDataForStation.isEmpty()) {
                fireStationDataList.add(new FireStationAddressWithListOfPersonWithMedicalRecordDTO(
                        stationNumber, // Garder le numéro de station en String comme demandé
                        addressDataForStation
                ));
                logger.info("Données agrégées pour la station {}", stationNumber);
            } else {
                logger.info("Aucune donnée de personne trouvée pour les adresses de la station {}", stationNumber);
                // Optionnel: Ajouter quand même une entrée vide pour la station si nécessaire
                // fireStationDataList.add(new FireStationAddressWithListOfPersonWithMedicalRecordDTO(stationNumber, Collections.emptyList()));
            }

        } // Fin boucle sur les numéros de station

        // 4. Assigner la liste finale au conteneur de résultat
        resultContainer.setFireStationAddressPersonMedicalRecords(fireStationDataList);
        logger.info("Agrégation terminée pour les stations {}. Nombre de stations avec données: {}",
                stationNumbers, fireStationDataList.size());

        // Retourner le DTO conteneur (jamais Optional.empty dans cette implémentation)
        return Optional.of(resultContainer);
    }


    // --- Méthodes CRUD ---

    /**
     * Ajoute un nouveau mapping entre une adresse et un numéro de station.
     * Vérifie au préalable si un mapping existe déjà pour cette adresse.
     *
     * @param fireStation L'objet {@link FireStation} contenant l'adresse et le numéro de station.
     *                    L'adresse et le numéro ne doivent pas être nuls ou vides.
     * @return L'objet {@link FireStation} sauvegardé.
     * @throws IllegalArgumentException Si les données d'entrée sont invalides (null/blank)
     *                                  ou si un mapping existe déjà pour l'adresse fournie.
     */
    public FireStation addFireStation(FireStation fireStation) {
        // Validation des entrées
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank()
                || fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            String errorMsg = "L'adresse et le numéro de station sont requis pour l'ajout.";
            logger.error(errorMsg + " Données reçues: {}", fireStation);
            throw new IllegalArgumentException(errorMsg);
        }

        // Vérification de l'existence
        if (fireStationRepository.existsByAddress(fireStation.getAddress())) {
            String errorMsg = "Un mapping existe déjà pour l'adresse : " + fireStation.getAddress();
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg); // Sera traité comme un conflit (409) par le contrôleur
        }

        logger.info("Ajout du mapping FireStation : {}", fireStation);
        // Appel au repository qui gère le trim et la sauvegarde
        return fireStationRepository.save(fireStation);
    }

    /**
     * Met à jour le numéro de station associé à une adresse existante.
     * L'adresse dans l'objet {@code fireStation} sert à identifier le mapping à mettre à jour.
     * Le numéro de station dans l'objet {@code fireStation} est la nouvelle valeur.
     *
     * @param fireStation L'objet {@link FireStation} contenant l'adresse à identifier et le nouveau numéro de station.
     *                    L'adresse et le numéro ne doivent pas être nuls ou vides.
     * @return Un {@link Optional} contenant l'objet {@link FireStation} mis à jour si l'adresse a été trouvée,
     *         sinon {@link Optional#empty()}.
     * @throws IllegalArgumentException Si les données d'entrée sont invalides (null/blank).
     */
    public Optional<FireStation> updateFireStation(FireStation fireStation) {
        // Validation des entrées
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank()
                || fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            String errorMsg = "L'adresse et le numéro de station sont requis pour la mise à jour.";
            logger.error(errorMsg + " Données reçues: {}", fireStation);
            throw new IllegalArgumentException(errorMsg);
        }

        // Vérifier si le mapping existe avant de tenter la mise à jour
        if (!fireStationRepository.existsByAddress(fireStation.getAddress())) {
            logger.warn("Tentative de mise à jour d'un mapping pour une adresse non trouvée : {}", fireStation.getAddress());
            return Optional.empty(); // Adresse non trouvée
        }

        logger.info("Mise à jour du mapping pour l'adresse '{}' vers la station '{}'",
                fireStation.getAddress(), fireStation.getStation());
        // Le save du repository gère le remplacement de l'ancienne entrée par la nouvelle
        FireStation updatedStation = fireStationRepository.save(fireStation);
        return Optional.of(updatedStation);
    }

    /**
     * Supprime le mapping adresse/station pour l'adresse spécifiée.
     *
     * @param address L'adresse dont le mapping doit être supprimé. Ne doit pas être nul ou blanc.
     * @return {@code true} si un mapping a été trouvé et supprimé, {@code false} sinon
     *         (y compris si l'adresse fournie est invalide).
     */
    public boolean deleteFireStationMapping(String address) {
        if (address == null || address.isBlank()) {
            logger.warn("Tentative de suppression de FireStation avec une adresse invalide.");
            return false;
        }
        logger.info("Tentative de suppression du mapping FireStation pour l'adresse : {}", address);
        // Le repository gère la recherche (insensible à la casse), la suppression et la sauvegarde
        return fireStationRepository.deleteByAddress(address);
    }

    /**
     * Récupère la liste complète de tous les mappings adresse/station enregistrés.
     *
     * @return Une {@code List<FireStation>} contenant tous les mappings. Peut être vide.
     */
    public List<FireStation> getAllFireStations() {
        logger.debug("Récupération de tous les mappings FireStation.");
        // Le repository retourne déjà une copie (potentiellement immuable via FileIOService)
        return fireStationRepository.findAll();
    }
}