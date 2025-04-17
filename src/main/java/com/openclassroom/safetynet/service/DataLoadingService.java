package com.openclassroom.safetynet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.model.DataContainer;
import com.openclassroom.safetynet.repository.FireStationRepository;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import com.openclassroom.safetynet.repository.PersonRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DataLoadingService {

    private static final Logger logger = LoggerFactory.getLogger(DataLoadingService.class);

    private final ObjectMapper objectMapper;
    private final PersonRepository personRepository;
    private final FireStationRepository firestationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final String dataLocation;

    public DataLoadingService(ObjectMapper objectMapper,
                              PersonRepository personRepository,
                              FireStationRepository firestationRepository,
                              MedicalRecordRepository medicalRecordRepository) {
        this.objectMapper = objectMapper;
        this.personRepository = personRepository;
        this.firestationRepository = firestationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.dataLocation = ConfigData.DIRECTORY_DATA;
    }

    //@PostConstruct
    public void loadData() {
        try {
            logger.info("Tentative de chargement des données depuis : {}", dataLocation);
            Resource resource = new ClassPathResource(dataLocation);

            if (!resource.exists()) {
                logger.error("FATAL: Le fichier de données '{}' n'a pas été trouvé dans le classpath.", dataLocation);
                throw new RuntimeException("Fichier de données initiales introuvable : " + dataLocation);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                DataContainer data = objectMapper.readValue(inputStream, DataContainer.class);

                //personRepository.setData(data.getPersons());
                //firestationRepository.setData(data.getFirestations());
                //medicalRecordRepository.setData(data.getMedicalrecords());

                logger.info("Données chargées avec succès depuis {}", dataLocation);

            }
        } catch (IOException e) {
            logger.error("FATAL: Erreur lors de la lecture ou du parsing du fichier de données '{}': {}", dataLocation, e.getMessage(), e);
            // Rendre l'échec du chargement fatal pour l'application
            throw new RuntimeException("Impossible de charger les données initiales depuis " + dataLocation, e);
        } catch (Exception e) {
            logger.error("FATAL: Erreur inattendue lors du chargement des données: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors du chargement des données initiales", e);
        }
    }
}