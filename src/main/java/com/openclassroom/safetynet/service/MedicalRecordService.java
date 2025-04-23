package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MedicalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordService.class);
    private final MedicalRecordRepository medicalRecordRepository;

    @Autowired
    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    /**
     * Ajoute un nouveau dossier médical.
     * @param medicalRecord Le dossier à ajouter.
     * @return Le dossier ajouté.
     * @throws IllegalArgumentException Si un dossier existe déjà pour ce nom/prénom ou si données invalides.
     */
    public MedicalRecord addMedicalRecord(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getFirstName() == null || medicalRecord.getFirstName().isBlank() ||
                medicalRecord.getLastName() == null || medicalRecord.getLastName().isBlank()) {
            logger.error("Tentative d'ajout de MedicalRecord avec prénom/nom invalides : {}", medicalRecord);
            throw new IllegalArgumentException("Le prénom et le nom sont requis pour ajouter un dossier médical.");
        }

        // Vérifier l'existence
        if (medicalRecordRepository.existsByFirstNameAndLastName(medicalRecord.getFirstName(), medicalRecord.getLastName())) {
            String errorMsg = "Un dossier médical existe déjà pour " + medicalRecord.getFirstName() + " " + medicalRecord.getLastName();
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg); // Sera attrapé comme Conflit par le contrôleur
        }

        logger.info("Ajout d'un nouveau dossier médical pour : {} {}", medicalRecord.getFirstName(), medicalRecord.getLastName());
        // save gère l'ajout et la persistance
        return medicalRecordRepository.save(medicalRecord);
    }

    /**
     * Met à jour un dossier médical existant.
     * Le prénom et le nom ne peuvent pas être modifiés.
     * @param medicalRecord Les informations de mise à jour (doit contenir prénom/nom pour identifier).
     * @return Optional contenant le dossier mis à jour si trouvé, sinon Optional vide.
     * @throws IllegalArgumentException si données invalides.
     */
    public Optional<MedicalRecord> updateMedicalRecord(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getFirstName() == null || medicalRecord.getFirstName().isBlank() ||
                medicalRecord.getLastName() == null || medicalRecord.getLastName().isBlank()) {
            logger.error("Tentative de mise à jour de MedicalRecord avec prénom/nom invalides : {}", medicalRecord);
            throw new IllegalArgumentException("Le prénom et le nom sont requis pour identifier le dossier à mettre à jour.");
        }

        // 1. Trouver le dossier existant
        Optional<MedicalRecord> existingRecordOpt = medicalRecordRepository.findByFirstNameAndLastName(
                medicalRecord.getFirstName(), medicalRecord.getLastName());

        if (existingRecordOpt.isEmpty()) {
            logger.warn("Tentative de mise à jour d'un dossier médical non trouvé pour : {} {}",
                    medicalRecord.getFirstName(), medicalRecord.getLastName());
            return Optional.empty(); // Dossier non trouvé
        }

        // 2. Mettre à jour les champs autorisés sur l'objet existant
        MedicalRecord existingRecord = existingRecordOpt.get();
        logger.info("Mise à jour du dossier médical pour : {} {}", existingRecord.getFirstName(), existingRecord.getLastName());

        // Mettre à jour uniquement birthdate, medications, allergies
        existingRecord.setBirthdate(medicalRecord.getBirthdate());
        // Remplacer les listes (s'assurer qu'elles ne sont pas nulles)
        existingRecord.setMedications(medicalRecord.getMedications() != null ? new ArrayList<>(medicalRecord.getMedications()) : new ArrayList<>());
        existingRecord.setAllergies(medicalRecord.getAllergies() != null ? new ArrayList<>(medicalRecord.getAllergies()) : new ArrayList<>());

        // 3. Sauvegarder l'objet existant MODIFIÉ
        MedicalRecord updatedRecord = medicalRecordRepository.save(existingRecord);
        return Optional.of(updatedRecord);
    }

    /**
     * Supprime un dossier médical par prénom et nom.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return true si le dossier a été trouvé et supprimé, false sinon.
     */
    public boolean deleteMedicalRecord(String firstName, String lastName) {
        if (firstName == null || lastName == null || firstName.isBlank() || lastName.isBlank()) {
            logger.warn("Tentative de suppression de MedicalRecord avec prénom/nom invalide.");
            return false;
        }
        logger.info("Tentative de suppression du dossier médical pour : {} {}", firstName, lastName);
        return medicalRecordRepository.deleteByFirstNameAndLastName(firstName, lastName);
    }

    // --- Méthodes Read (Optionnelles) ---
    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll();
    }

    public Optional<MedicalRecord> getMedicalRecord(String firstName, String lastName) {
        return medicalRecordRepository.findByFirstNameAndLastName(firstName, lastName);
    }
}