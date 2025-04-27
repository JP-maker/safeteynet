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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;

@Service
public class FireStationService {

    private static final Logger logger = LoggerFactory.getLogger(FireStationService.class);

    private final FireStationRepository fireStationRepository;
    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PersonService personService;

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
     * Récupère les personnes couvertes par une station donnée, avec le compte des adultes et enfants.
     */
    public Optional<FireStationCoverageDTO> getPeopleCoveredByStation(int stationNumber) {
        logger.debug("Recherche des adresses pour la station numéro: {}", stationNumber);

        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            return Optional.empty();
        }
        logger.debug("Adresses trouvées: {}", addresses);

        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);

        List<PersonInfoDTO> personInfos = new ArrayList<>();
        int adultCount = 0;
        int childCount = 0;

        for (Person person : peopleAtAddresses) {
            personInfos.add(new PersonInfoDTO(
                    person.getFirstName(),
                    person.getLastName(),
                    person.getAddress(),
                    person.getPhone()
            ));

            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );

            if (medicalRecordOpt.isPresent()) {
                try {
                    int age = AgeCalculator.calculateAge(medicalRecordOpt.get().getBirthdate());
                    if (age <= ConfigData.CHILD_AGE_THRESHOLD) {
                        childCount++;
                    } else {
                        adultCount++;
                    }
                } catch (Exception e) {
                    logger.error("Erreur de calcul d'âge pour {} {}: {}", person.getFirstName(), person.getLastName(), e.getMessage());
                }
            } else {
                logger.warn("Pas de dossier médical pour {} {}", person.getFirstName(), person.getLastName());
            }
        }

        FireStationCoverageDTO result = new FireStationCoverageDTO(personInfos, adultCount, childCount);
        logger.info("Station {}: {} personnes, {} adultes, {} enfants", stationNumber, personInfos.size(), adultCount, childCount);

        return Optional.of(result);
    }

    /**
     * Récupère les numéros de téléphone des personnes couvertes par une station donnée.
     */
    public Optional<PhoneAlertDTO> getPhoneNumberByStation(int stationNumber) {
        logger.debug("Recherche des téléphones pour la station numéro: {}", stationNumber);

        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            return Optional.empty();
        }

        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);

        List<String> phoneNumbers = new ArrayList<>();
        for (Person person : peopleAtAddresses) {
            phoneNumbers.add(person.getPhone());
        }

        PhoneAlertDTO result = new PhoneAlertDTO(phoneNumbers);
        logger.info("Station {}: {} numéros de téléphone récupérés", stationNumber, phoneNumbers.size());

        return Optional.of(result);
    }

    /**
     * Récupère les personnes et leurs antécédents médicaux par liste de stations.
     */
    public Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> getListOfPersonsWithMedicalRecordsByListOfFireStation(List<String> stationNumbers) {
        ListOfAddressWithListOfPersonWithMedicalRecordDTO result = new ListOfAddressWithListOfPersonWithMedicalRecordDTO();
        List<FireStationAddressWithListOfPersonWithMedicalRecordDTO> fireStationData = new ArrayList<>();

        for (String stationNumber : stationNumbers) {
            List<AddressWithListOfPersonWithMedicalRecordDTO> addressData = new ArrayList<>();

            List<String> addresses = fireStationRepository.findAddressesByStationNumber(parseInt(stationNumber));
            logger.debug("Adresses pour la station {}: {}", stationNumber, addresses);

            for (String address : addresses) {
                Optional<FirePersonDTO> persons = personService.getPersonFireStationAndMedicalReportByAddress(address);
                if (persons.isPresent()) {
                    addressData.add(new AddressWithListOfPersonWithMedicalRecordDTO(address, persons.get().getPersons()));
                    logger.info("Adresse {}: {} personnes récupérées", address, persons.get().getPersons().size());
                }
            }

            fireStationData.add(new FireStationAddressWithListOfPersonWithMedicalRecordDTO(
                    stationNumber,
                    addressData.isEmpty() ? null : addressData
            ));
        }

        result.setFireStationAddressPersonMedicalRecords(fireStationData);
        logger.info("Résultat global pour les stations {}: {}", stationNumbers, result);

        return Optional.of(result);
    }

    /**
     * Ajoute un nouveau mapping adresse/station.
     */
    public FireStation addFireStation(FireStation fireStation) {
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank()
                || fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            logger.error("Données invalides pour ajout: {}", fireStation);
            throw new IllegalArgumentException("Adresse et station sont requises.");
        }

        if (fireStationRepository.existsByAddress(fireStation.getAddress())) {
            logger.warn("Adresse déjà existante: {}", fireStation.getAddress());
            throw new IllegalArgumentException("Mapping existant pour l'adresse: " + fireStation.getAddress());
        }

        logger.info("Ajout du mapping: {}", fireStation);
        return fireStationRepository.save(fireStation);
    }

    /**
     * Met à jour une station existante pour une adresse.
     */
    public Optional<FireStation> updateFireStation(FireStation fireStation) {
        if (fireStation == null || fireStation.getAddress() == null || fireStation.getAddress().isBlank()
                || fireStation.getStation() == null || fireStation.getStation().isBlank()) {
            logger.error("Données invalides pour mise à jour: {}", fireStation);
            throw new IllegalArgumentException("Adresse et station sont requises pour mise à jour.");
        }

        if (!fireStationRepository.existsByAddress(fireStation.getAddress())) {
            logger.warn("Adresse introuvable pour mise à jour: {}", fireStation.getAddress());
            return Optional.empty();
        }

        logger.info("Mise à jour du mapping pour adresse '{}'", fireStation.getAddress());
        return Optional.of(fireStationRepository.save(fireStation));
    }

    /**
     * Supprime le mapping d'une adresse donnée.
     */
    public boolean deleteFireStationMapping(String address) {
        if (address == null || address.isBlank()) {
            logger.warn("Adresse invalide pour suppression.");
            return false;
        }

        logger.info("Suppression du mapping pour l'adresse: {}", address);
        return fireStationRepository.deleteByAddress(address);
    }

    /**
     * Récupère tous les mappings adresse/station.
     */
    public List<FireStation> getAllFireStations() {
        return fireStationRepository.findAll();
    }
}
