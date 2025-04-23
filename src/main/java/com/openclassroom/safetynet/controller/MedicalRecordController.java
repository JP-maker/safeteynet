package com.openclassroom.safetynet.controller;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.MedicalRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/medicalRecord")
public class MedicalRecordController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordController.class);
    private final MedicalRecordService medicalRecordService;

    @Autowired
    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * Endpoint pour ajouter un nouveau dossier médical.
     * POST /medicalRecord
     * Corps: { "firstName":"...", "lastName":"...", "birthdate":"...", "medications":[...], "allergies": [...] }
     * @param medicalRecord Le dossier médical à ajouter.
     * @return ResponseEntity contenant le dossier créé ou une réponse 409 si conflit.
     */
    @PostMapping
    public ResponseEntity<MedicalRecord> addMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        try {
            MedicalRecord createdRecord = medicalRecordService.addMedicalRecord(medicalRecord);
            logger.info("Endpoint POST /medicalRecord: Dossier créé pour {} {}", createdRecord.getFirstName(), createdRecord.getLastName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
        } catch (IllegalArgumentException e) {
            logger.warn("Endpoint POST /medicalRecord: Échec de création - {}", e.getMessage());
            if (e.getMessage().contains("existe déjà")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
            } else {
                return ResponseEntity.badRequest().build(); // 400 pour données invalides
            }
        } catch (Exception e) {
            logger.error("Endpoint POST /medicalRecord: Erreur interne", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint pour mettre à jour un dossier médical existant.
     * PUT /medicalRecord
     * Corps: { "firstName":"...", "lastName":"...", "birthdate":"...", "medications":[...], "allergies": [...] }
     * Le prénom/nom identifie le dossier, seuls les autres champs sont mis à jour.
     * @param medicalRecord Le dossier médical à mettre à jour.
     * @return ResponseEntity contenant le dossier mis à jour ou une réponse 404 si non trouvé.
     *
     */
    @PutMapping
    public ResponseEntity<MedicalRecord> updateMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        try {
            Optional<MedicalRecord> updatedRecordOpt = medicalRecordService.updateMedicalRecord(medicalRecord);

            return updatedRecordOpt
                    .map(record -> {
                        logger.info("Endpoint PUT /medicalRecord: Dossier mis à jour pour {} {}", record.getFirstName(), record.getLastName());
                        return ResponseEntity.ok(record); // 200 OK
                    })
                    .orElseGet(() -> {
                        logger.warn("Endpoint PUT /medicalRecord: Dossier non trouvé pour {} {}", medicalRecord.getFirstName(), medicalRecord.getLastName());
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    });
        } catch (IllegalArgumentException e) {
            logger.warn("Endpoint PUT /medicalRecord: Échec de mise à jour - Données invalides : {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // 400
        } catch (Exception e) {
            logger.error("Endpoint PUT /medicalRecord: Erreur interne", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint pour supprimer un dossier médical.
     * DELETE /medicalRecord?firstName=<prénom>&lastName=<nom>
     * @param firstName Le prénom de la personne.
     * @param lastName Le nom de la personne.
     * @return ResponseEntity.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteMedicalRecord(@RequestParam String firstName, @RequestParam String lastName) {
        try {
            boolean deleted = medicalRecordService.deleteMedicalRecord(firstName, lastName);

            if (deleted) {
                logger.info("Endpoint DELETE /medicalRecord: Dossier supprimé pour {} {}", firstName, lastName);
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                logger.warn("Endpoint DELETE /medicalRecord: Dossier non trouvé pour {} {}", firstName, lastName);
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (Exception e) {
            logger.error("Endpoint DELETE /medicalRecord: Erreur interne pour {} {}", firstName, lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}