package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.dto.PersonInfoDTO;
import com.openclassroom.safetynet.dto.PhoneAlertDTO;
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

@Service
public class FireStationService {

    private static final Logger logger = LoggerFactory.getLogger(FireStationService.class);

    private final FireStationRepository fireStationRepository;
    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public FireStationService(FireStationRepository fireStationRepository,
                              PersonRepository personRepository,
                              MedicalRecordRepository medicalRecordRepository) {
        this.fireStationRepository = fireStationRepository;
        this.personRepository = personRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public Optional<FireStationCoverageDTO> getPeopleCoveredByStation(int stationNumber) {
        logger.debug("Recherche des adresses pour la station numéro: {}", stationNumber);

        // 1. Trouver toutes les adresses couvertes par cette station
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            // Retourner un Optional vide si la station n'existe pas ou ne couvre aucune adresse
            return Optional.empty();
            // Ou un DTO vide avec une réponse 200 avec des listes/comptes vides:
            // return Optional.of(new FireStationCoverageDTO(new ArrayList<>(), 0, 0));
        }
        logger.debug("Adresses trouvées pour la station {}: {}", stationNumber, addresses);

        // 2. Trouver toutes les personnes vivant à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);


        List<PersonInfoDTO> personInfos = new ArrayList<>();
        int adultCount = 0;
        int childCount = 0;

        // 3. Pour chaque personne, récupérer les infos et calculer l'âge
        for (Person person : peopleAtAddresses) {
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
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());
                    if (age <= ConfigData.CHILD_AGE_THRESHOLD) {
                        childCount++;
                    } else {
                        adultCount++;
                    }
                } catch (Exception e) {
                    // Gérer les exceptions lors du calcul de l'âge
                    logger.error("Impossible de calculer l'âge pour {} {} (date: {}): {}",
                            person.getFirstName(), person.getLastName(), medicalRecord.getBirthdate(), e.getMessage());
                }
            } else {
                logger.warn("Aucun dossier médical trouvé pour {} {}, impossible de déterminer l'âge.",
                        person.getFirstName(), person.getLastName());
                // Décidez comment compter cette personne (ex: adulte par défaut?)
            }
        }

        FireStationCoverageDTO result = new FireStationCoverageDTO(personInfos, adultCount, childCount);
        logger.info("Résultat pour station {}: {} personnes, {} adultes, {} enfants",
                stationNumber, personInfos.size(), adultCount, childCount);

        return Optional.of(result);
    }

    public Optional<PhoneAlertDTO> getPhoneNumberByStation(int stationNumber) {
        logger.debug("Recherche des téléphone pour la station numéro: {}", stationNumber);

        // 1. Trouver toutes les adresses couvertes par cette station
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(stationNumber);

        if (addresses.isEmpty()) {
            logger.warn("Aucune adresse trouvée pour la station numéro: {}", stationNumber);
            // Retourner un Optional vide si la station n'existe pas ou ne couvre aucune adresse
            return Optional.empty();
        }
        logger.debug("Adresses trouvées pour la station {}: {}", stationNumber, addresses);

        // 2. Trouver toutes les personnes vivant à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), addresses);

        List<String> phoneNumber = new ArrayList<>();

        // 3. Pour chaque personne, récupérer le numéro de téléphone
        for (Person person : peopleAtAddresses) {
            phoneNumber.add(person.getPhone());
        }

        PhoneAlertDTO result = new PhoneAlertDTO(phoneNumber);
        logger.info("Résultat pour station {}: {} numéros de téléphone",
                stationNumber, phoneNumber.size());

        return Optional.of(result);
    }
}