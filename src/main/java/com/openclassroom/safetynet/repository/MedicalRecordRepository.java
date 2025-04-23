package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.FileIOService;
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
    private final FileIOService fileIOService;

    public MedicalRecordRepository(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * Trouve le dossier médical d'une personne par son prénom et nom.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return Un Optional contenant le dossier médical si trouvé, sinon Optional vide.
     */
    public Optional<MedicalRecord> findByFirstNameAndLastName(String firstName, String lastName) {
        return fileIOService.getMedicalRecords().stream()
                .filter(mr -> Objects.equals(mr.getFirstName(), firstName) && Objects.equals(mr.getLastName(), lastName))
                .findFirst(); // Suppose un seul dossier par personne
    }

    /**
     * Récupère tous les dossiers médicaux.
     * @return Une copie de la liste des dossiers médicaux.
     */
    public List<MedicalRecord> findAll() {
        return new ArrayList<>(fileIOService.getMedicalRecords());
    }

    /**
     * Vérifie si un dossier médical existe pour un prénom et nom donné.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return true si un dossier existe, false sinon.
     */
    public boolean existsByFirstNameAndLastName(String firstName, String lastName) {
        if (firstName == null || lastName == null) {
            return false;
        }
        // Comparaison insensible à la casse
        return fileIOService.getMedicalRecords().stream()
                .anyMatch(mr -> mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstName.trim()) &&
                        mr.getLastName().equalsIgnoreCase(lastName.trim()));
    }


    /**
     * Sauvegarde un dossier médical (ajoute si nouveau, met à jour si existant).
     * L'unicité est basée sur la combinaison prénom/nom (insensible à la casse).
     * @param medicalRecord Le dossier à sauvegarder.
     * @return Le dossier sauvegardé.
     * @throws IllegalArgumentException si medicalRecord ou son prénom/nom est null/vide.
     */
    public MedicalRecord save(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getFirstName() == null || medicalRecord.getFirstName().isBlank() ||
                medicalRecord.getLastName() == null || medicalRecord.getLastName().isBlank()) {
            throw new IllegalArgumentException("MedicalRecord et son prénom/nom ne peuvent être nuls ou vides.");
        }

        String firstNameTrimmed = medicalRecord.getFirstName().trim();
        String lastNameTrimmed = medicalRecord.getLastName().trim();

        // Obtenir une copie mutable de la liste actuelle
        List<MedicalRecord> currentRecords = new ArrayList<>(fileIOService.getMedicalRecords());

        // Supprimer l'ancienne entrée si elle existe (basé sur prénom/nom, insensible à la casse)
        boolean removed = currentRecords.removeIf(mr ->
                mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        mr.getLastName().equalsIgnoreCase(lastNameTrimmed));

        if (removed) {
            logger.debug("Ancien MedicalRecord pour {} {} supprimé avant sauvegarde.", firstNameTrimmed, lastNameTrimmed);
        }

        // Créer une copie "propre" pour s'assurer qu'on ajoute bien les données voulues
        MedicalRecord recordToSave = new MedicalRecord();
        recordToSave.setFirstName(firstNameTrimmed);
        recordToSave.setLastName(lastNameTrimmed);
        recordToSave.setBirthdate(medicalRecord.getBirthdate());
        // S'assurer que les listes ne sont pas nulles
        recordToSave.setMedications(medicalRecord.getMedications() != null ? new ArrayList<>(medicalRecord.getMedications()) : new ArrayList<>());
        recordToSave.setAllergies(medicalRecord.getAllergies() != null ? new ArrayList<>(medicalRecord.getAllergies()) : new ArrayList<>());

        // Ajouter la nouvelle version (ou la première version)
        currentRecords.add(recordToSave);
        logger.debug("Nouveau MedicalRecord pour {} {} ajouté à la liste.", firstNameTrimmed, lastNameTrimmed);

        // Sauvegarder la liste complète
        fileIOService.setMedicalRecords(currentRecords);

        logger.info("MedicalRecord sauvegardé pour : {} {}", firstNameTrimmed, lastNameTrimmed);
        return recordToSave;
    }

    /**
     * Supprime un dossier médical par prénom et nom.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return true si un dossier a été trouvé et supprimé, false sinon.
     */
    public boolean deleteByFirstNameAndLastName(String firstName, String lastName) {
        if (firstName == null || lastName == null || firstName.isBlank() || lastName.isBlank()) {
            return false;
        }
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim();

        List<MedicalRecord> currentRecords = new ArrayList<>(fileIOService.getMedicalRecords());

        boolean removed = currentRecords.removeIf(mr ->
                mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        mr.getLastName().equalsIgnoreCase(lastNameTrimmed));

        // Si supprimé, sauvegarder la nouvelle liste
        if (removed) {
            fileIOService.setMedicalRecords(currentRecords);
            logger.info("MedicalRecord pour {} {} supprimé.", firstNameTrimmed, lastNameTrimmed);
        } else {
            logger.warn("Aucun MedicalRecord trouvé pour {} {} lors de la tentative de suppression.", firstNameTrimmed, lastNameTrimmed);
        }
        return removed;
    }
}