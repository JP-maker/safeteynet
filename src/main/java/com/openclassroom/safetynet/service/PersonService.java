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

@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final FireStationRepository fireStationRepository;

    public PersonService(PersonRepository personRepository,
                         MedicalRecordRepository medicalRecordRepository) {
        this.personRepository = personRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.fireStationRepository = new FireStationRepository();
    }

    public Optional<ChildWithFamilyDTO> getChildAndFamilyByAddress(String address) {
        logger.debug("Recherche des personnes pour l'adresse: {}", address);

        List<String> addresses = new ArrayList<>();
        addresses.add(address);
        List<ChildInfoDTO> childInfos = new ArrayList<>();
        List<PersonInfoDTO> personInfos = new ArrayList<>();

        // 1. Trouver toutes les personnes vivant à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), address);

        // 2. Pour chaque personne, on calcule l'âge et on les classe
        for (Person person : peopleAtAddresses) {

            // Trouver le dossier médical pour calculer l'âge
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());
                    if (age <= ConfigData.CHILD_AGE_THRESHOLD) {
                        childInfos.add(new ChildInfoDTO(
                                person.getFirstName(),
                                person.getLastName(),
                                age
                        ));
                    } else {
                        personInfos.add(new PersonInfoDTO(
                                person.getFirstName(),
                                person.getLastName(),
                                person.getAddress(),
                                person.getPhone()
                        ));
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

        ChildWithFamilyDTO result = new ChildWithFamilyDTO(childInfos, personInfos);
        logger.info("Résultat pour l'adresse {}: {} enfants, {} membres de famille",
                address, childInfos.size(), personInfos.size());

        if (childInfos.isEmpty()) {
            logger.warn("Aucun enfant trouvé pour l'adresse {}", address);
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }

    public Optional<FirePersonDTO> getPersonFireStationAndMedicalReportByAddress(String address) {
        logger.debug("Recherche des personnes, caserne et informations médicales pour l'adresse: {}", address);

        List<String> addresses = new ArrayList<>();
        addresses.add(address);
        String fireStationNumber = null;
        List<PersonWithMedicalRecordDTO> personInfos = new ArrayList<>();

        // 1. Trouver toutes les personnes vivant à ces adresses
        List<Person> peopleAtAddresses = personRepository.findByAddressIn(addresses);
        logger.debug("{} personnes trouvées aux adresses {}", peopleAtAddresses.size(), address);


        // 2. Pour chaque personne, on trouve la caserne, le dossier médical et on calcule l'âge
        for (Person person : peopleAtAddresses) {

            // 2.1 Pour chaque personne, on recherche le numéro de la caserne de pompiers
            fireStationNumber = fireStationRepository.findStationNumberByAddress(address);
            logger.debug("Le numéro trouvé de caserne est le {}", fireStationNumber);

            // 2.2 Trouver le dossier médical pour calculer l'âge
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                    person.getFirstName(), person.getLastName()
            );
            logger.debug("{} dossier médical trouvé", medicalRecordOpt.isPresent() ? 1 : 0);

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                try {
                    int age = AgeCalculator.calculateAge(medicalRecord.getBirthdate());

                    personInfos.add(new PersonWithMedicalRecordDTO(
                            person.getLastName(),
                            person.getPhone(),
                            fireStationNumber,
                            age,
                            medicalRecord.getMedications(),
                            medicalRecord.getAllergies()
                    ));
                } catch (Exception e) {
                    // Gérer les exceptions lors du calcul de l'âge
                    logger.error("getPersonFireStationAndMedicalReportByAddress - Impossible de calculer l'âge pour {} {} (date: {}): {}",
                            person.getFirstName(), person.getLastName(), medicalRecord.getBirthdate(), e.getMessage());
                }
            } else {
                logger.warn("getPersonFireStationAndMedicalReportByAddress - Aucun dossier médical trouvé pour {} {}, impossible de déterminer l'âge.",
                        person.getFirstName(), person.getLastName());
                // Décidez comment compter cette personne (ex: adulte par défaut?)
            }
        }

        FirePersonDTO result = new FirePersonDTO(personInfos);
        logger.info("Résultat pour l'adresse {}: {} résidents",
                personInfos.size());

        if (personInfos.isEmpty()) {
            logger.warn("Aucun résident trouvé pour l'adresse {}", address);
            return Optional.empty();
        } else {
            return Optional.of(result);
        }
    }
}
