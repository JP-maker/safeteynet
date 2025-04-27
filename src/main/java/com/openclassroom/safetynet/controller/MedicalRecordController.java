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

/**
 * Contrôleur REST gérant les requêtes HTTP pour les dossiers médicaux (Medical Records).
 * <p>
 * Expose des endpoints pour les opérations CRUD (Create, Update, Delete) sur les dossiers médicaux.
 * L'identification d'un dossier médical se fait par la combinaison unique du prénom et du nom de famille.
 * </p><p>
 * Utilise {@link MedicalRecordService} pour déléguer la logique métier.
 * </p>
 */
@RestController
@RequestMapping("/medicalRecord") // Préfixe pour toutes les routes de ce contrôleur
public class MedicalRecordController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordController.class);
    private final MedicalRecordService medicalRecordService;

    /**
     * Construit une nouvelle instance de MedicalRecordController.
     *
     * @param medicalRecordService Le service injecté pour gérer la logique métier des dossiers médicaux.
     */
    @Autowired // Optionnel ici
    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * Endpoint POST pour ajouter un nouveau dossier médical.
     * <p>
     * URL: {@code POST /medicalRecord}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link MedicalRecord}.
     * Exemple: {@code { "firstName":"John", "lastName":"Doe", "birthdate":"01/01/1990", "medications":["medA:100mg"], "allergies":["pollen"] }}
     * </p>
     *
     * @param medicalRecord L'objet {@link MedicalRecord} à créer, désérialisé depuis le corps JSON.
     *                      Le prénom et le nom ne doivent pas être nuls ou vides.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 201 (Created) avec l'objet {@link MedicalRecord} créé dans le corps en cas de succès.</li>
     *             <li>Code 409 (Conflict) si un dossier existe déjà pour cette combinaison prénom/nom.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (prénom/nom manquants, etc.).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PostMapping
    public ResponseEntity<MedicalRecord> addMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        try {
            // Délégation de l'ajout au service
            MedicalRecord createdRecord = medicalRecordService.addMedicalRecord(medicalRecord);
            logger.info("Endpoint POST /medicalRecord: Dossier créé pour {} {}", createdRecord.getFirstName(), createdRecord.getLastName());
            // Retour de la réponse succès
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord); // 201
        } catch (IllegalArgumentException e) {
            // Gestion des erreurs métier (conflit ou données invalides)
            logger.warn("Endpoint POST /medicalRecord: Échec de création - {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("existe déjà")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
            } else {
                // Autre IllegalArgumentException = Mauvaise requête
                return ResponseEntity.badRequest().build(); // 400
            }
        } catch (Exception e) {
            // Gestion des erreurs techniques imprévues
            logger.error("Endpoint POST /medicalRecord: Erreur interne inattendue", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint PUT pour mettre à jour un dossier médical existant.
     * <p>
     * URL: {@code PUT /medicalRecord}
     * </p>
     * <p>
     * Le corps de la requête doit être un JSON représentant l'objet {@link MedicalRecord}.
     * Le prénom et le nom dans le JSON identifient le dossier à mettre à jour.
     * Seuls les champs {@code birthdate}, {@code medications}, et {@code allergies} sont mis à jour.
     * Exemple: {@code { "firstName":"John", "lastName":"Doe", "birthdate":"02/02/1991", "medications":["medB:200mg"], "allergies":[] }}
     * </p>
     *
     * @param medicalRecord L'objet {@link MedicalRecord} contenant les informations de mise à jour,
     *                      désérialisé depuis le corps JSON. Prénom et nom sont utilisés pour l'identification.
     * @return Une {@link ResponseEntity} contenant:
     *         <ul>
     *             <li>Code 200 (OK) avec l'objet {@link MedicalRecord} mis à jour dans le corps si trouvé et mis à jour.</li>
     *             <li>Code 404 (Not Found) si aucun dossier n'existe pour la combinaison prénom/nom fournie.</li>
     *             <li>Code 400 (Bad Request) si les données fournies sont invalides (prénom/nom manquants).</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @PutMapping
    public ResponseEntity<MedicalRecord> updateMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        try {
            // Délégation de la mise à jour au service
            Optional<MedicalRecord> updatedRecordOpt = medicalRecordService.updateMedicalRecord(medicalRecord);

            // Traitement de la réponse du service (Optional)
            return updatedRecordOpt
                    .map(record -> {
                        // Cas où le dossier a été trouvé et mis à jour
                        logger.info("Endpoint PUT /medicalRecord: Dossier mis à jour pour {} {}", record.getFirstName(), record.getLastName());
                        return ResponseEntity.ok(record); // 200 OK
                    })
                    .orElseGet(() -> {
                        // Cas où le dossier n'a pas été trouvé par le service
                        logger.warn("Endpoint PUT /medicalRecord: Dossier non trouvé pour {} {}",
                                (medicalRecord != null ? medicalRecord.getFirstName() : "null"),
                                (medicalRecord != null ? medicalRecord.getLastName() : "null"));
                        return ResponseEntity.notFound().build(); // 404 Not Found
                    });
        } catch (IllegalArgumentException e) {
            // Gestion des données d'entrée invalides (ex: prénom/nom manquants)
            logger.warn("Endpoint PUT /medicalRecord: Échec de mise à jour - Données invalides : {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // 400
        } catch (Exception e) {
            // Gestion des erreurs techniques imprévues
            logger.error("Endpoint PUT /medicalRecord: Erreur interne inattendue", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /**
     * Endpoint DELETE pour supprimer un dossier médical.
     * <p>
     * URL: {@code DELETE /medicalRecord?firstName=<prénom>&lastName=<nom>}
     * </p>
     *
     * @param firstName Le prénom de la personne dont le dossier doit être supprimé (paramètre de requête URL).
     * @param lastName Le nom de famille de la personne dont le dossier doit être supprimé (paramètre de requête URL).
     * @return Une {@link ResponseEntity<Void>} avec:
     *         <ul>
     *             <li>Code 204 (No Content) si la suppression a réussi.</li>
     *             <li>Code 404 (Not Found) si aucun dossier n'a été trouvé pour cette combinaison prénom/nom.</li>
     *             <li>Code 400 (Bad Request) si les paramètres {@code firstName} ou {@code lastName} sont manquants.</li>
     *             <li>Code 500 (Internal Server Error) en cas d'erreur interne inattendue.</li>
     *         </ul>
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteMedicalRecord(@RequestParam String firstName, @RequestParam String lastName) {
        // La validation @RequestParam s'assure que les paramètres sont présents,
        // mais pas qu'ils sont non-blancs (le service pourrait le faire si nécessaire).
        try {
            // Délégation de la suppression au service
            boolean deleted = medicalRecordService.deleteMedicalRecord(firstName, lastName);

            if (deleted) {
                // Cas suppression réussie
                logger.info("Endpoint DELETE /medicalRecord: Dossier supprimé pour {} {}", firstName, lastName);
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                // Cas dossier non trouvé (le service retourne false)
                logger.warn("Endpoint DELETE /medicalRecord: Dossier non trouvé pour {} {}", firstName, lastName);
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (Exception e) {
            // Gestion des erreurs techniques imprévues
            logger.error("Endpoint DELETE /medicalRecord: Erreur interne lors de la suppression pour {} {}", firstName, lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}