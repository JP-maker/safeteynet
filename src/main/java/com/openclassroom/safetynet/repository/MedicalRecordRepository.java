package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.MedicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class MedicalRecordRepository {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordRepository.class);
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    /**
     * Initialise ou met à jour les données des dossiers médicaux.
     * @param recordList La liste complète des dossiers médicaux chargés.
     */
    public void setData(List<MedicalRecord> recordList) {
        if (recordList != null) {
            this.medicalRecords = new ArrayList<>(recordList);
            logger.info("{} enregistrements médicaux chargés.", this.medicalRecords.size());
        } else {
            this.medicalRecords = new ArrayList<>();
            logger.warn("La liste des dossiers médicaux fournie pour l'initialisation est nulle.");
        }
    }

    /**
     * Trouve le dossier médical d'une personne par son prénom et nom.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return Un Optional contenant le dossier médical si trouvé, sinon Optional vide.
     */
    public Optional<MedicalRecord> findByFirstNameAndLastName(String firstName, String lastName) {
        return medicalRecords.stream()
                .filter(mr -> Objects.equals(mr.getFirstName(), firstName) && Objects.equals(mr.getLastName(), lastName))
                .findFirst(); // Suppose un seul dossier par personne
    }

    /**
     * Récupère tous les dossiers médicaux.
     * @return Une copie de la liste des dossiers médicaux.
     */
    public List<MedicalRecord> findAll() {
        return new ArrayList<>(medicalRecords);
    }

    // Ajoutez d'autres méthodes si nécessaire
}