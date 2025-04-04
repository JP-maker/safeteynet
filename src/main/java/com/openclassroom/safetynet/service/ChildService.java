package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.dto.ChildInfoDTO;
import com.openclassroom.safetynet.dto.ChildWithFamilyDTO;
import com.openclassroom.safetynet.dto.FireStationCoverageDTO;
import com.openclassroom.safetynet.dto.PersonInfoDTO;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
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
public class ChildService {

    private static final Logger logger = LoggerFactory.getLogger(ChildService.class);

    private final PersonRepository personRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public ChildService(PersonRepository personRepository,
                              MedicalRecordRepository medicalRecordRepository) {
        this.personRepository = personRepository;
        this.medicalRecordRepository = medicalRecordRepository;
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
}
