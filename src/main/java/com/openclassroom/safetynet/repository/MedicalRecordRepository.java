package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.FileIOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections; // Ajout import manquant
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors; // Import non utilisé directement mais bon à avoir

/**
 * Repository responsable de l'accès et de la manipulation des données des dossiers médicaux (Medical Records).
 * <p>
 * Ce repository interagit avec le {@link FileIOService} pour obtenir la liste actuelle des dossiers médicaux
 * et pour persister les modifications via {@link FileIOService#setMedicalRecords(List)}.
 * Il fournit des méthodes pour rechercher des dossiers par nom/prénom et pour effectuer les opérations CRUD
 * de base sur les dossiers médicaux. L'unicité d'un dossier est définie par la combinaison prénom/nom.
 * </p>
 */
@Repository
public class MedicalRecordRepository {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordRepository.class);
    private final FileIOService fileIOService;

    /**
     * Construit une nouvelle instance de MedicalRecordRepository.
     *
     * @param fileIOService Le service injecté responsable de l'accès aux données brutes du fichier.
     */
    @Autowired // Optionnel ici
    public MedicalRecordRepository(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * Trouve le dossier médical d'une personne en utilisant son prénom et son nom de famille.
     * <p>
     * La recherche est effectuée de manière <b>sensible à la casse</b> pour le prénom et le nom,
     * conformément à l'utilisation de {@link Objects#equals(Object, Object)}.
     * On suppose qu'il n'existe qu'un seul dossier médical par combinaison unique de prénom et nom.
     * </p>
     *
     * @param firstName Le prénom exact (sensible à la casse) de la personne.
     * @param lastName Le nom de famille exact (sensible à la casse) de la personne.
     * @return Un {@link Optional<MedicalRecord>} contenant le dossier médical s'il est trouvé,
     *         sinon {@link Optional#empty()}. Retourne également {@link Optional#empty()} si
     *         le prénom ou le nom fourni est {@code null}.
     */
    public Optional<MedicalRecord> findByFirstNameAndLastName(String firstName, String lastName) {
        // Pas de recherche si l'un des identifiants est null
        if (firstName == null || lastName == null) {
            return Optional.empty();
        }

        // Obtient la liste actuelle depuis le cache de FileIOService
        List<MedicalRecord> currentRecords = fileIOService.getMedicalRecords();

        // Utilise un stream pour trouver la première correspondance exacte (sensible à la casse)
        return currentRecords.stream()
                .filter(mr -> Objects.equals(mr.getFirstName(), firstName) && Objects.equals(mr.getLastName(), lastName))
                .findFirst();
    }

    /**
     * Récupère la liste complète de tous les dossiers médicaux actuellement enregistrés.
     * Retourne une nouvelle copie mutable de la liste obtenue depuis {@link FileIOService}.
     *
     * @return Une {@code List<MedicalRecord>} contenant tous les dossiers.
     *         Cette liste est une copie mutable et peut être vide.
     */
    public List<MedicalRecord> findAll() {
        // Crée une copie mutable de la liste retournée par FileIOService
        return new ArrayList<>(fileIOService.getMedicalRecords());
    }

    /**
     * Vérifie si un dossier médical existe pour une combinaison donnée de prénom et nom.
     * <p>
     * La vérification est effectuée de manière <b>insensible à la casse</b> et ignore les espaces
     * de début/fin pour le prénom et le nom fournis.
     * </p>
     *
     * @param firstName Le prénom (insensible à la casse, trimé) à vérifier.
     * @param lastName Le nom de famille (insensible à la casse, trimé) à vérifier.
     * @return {@code true} si un dossier médical existe pour cette personne, {@code false} sinon
     *         (y compris si le prénom ou le nom fourni est nul ou vide).
     */
    public boolean existsByFirstNameAndLastName(String firstName, String lastName) {
        // Validation des entrées
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            logger.trace("Vérification d'existence pour nom/prénom nul ou vide -> false");
            return false;
        }
        // Nettoyage des entrées pour la comparaison
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim();

        // Obtient la liste actuelle
        List<MedicalRecord> currentRecords = fileIOService.getMedicalRecords();

        // Vérifie si au moins un enregistrement correspond (insensible à la casse)
        return currentRecords.stream()
                .anyMatch(mr -> mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        mr.getLastName().equalsIgnoreCase(lastNameTrimmed));
    }


    /**
     * Sauvegarde (ajoute ou met à jour) un dossier médical.
     * <p>
     * L'unicité est basée sur la combinaison prénom/nom. Si un dossier existe déjà
     * pour cette combinaison (vérification insensible à la casse), il est remplacé par
     * les nouvelles informations. Sinon, un nouveau dossier est ajouté.
     * </p><p>
     * Les valeurs de prénom et nom sont nettoyées (trim) avant la sauvegarde.
     * Les listes de médications et d'allergies sont également copiées pour assurer
     * qu'elles ne sont pas nulles dans l'objet sauvegardé.
     * </p><p>
     * Après modification de la liste en mémoire, la liste complète est passée à
     * {@link FileIOService#setMedicalRecords(List)} pour persistance.
     * </p>
     *
     * @param medicalRecord Le dossier médical {@link MedicalRecord} à sauvegarder.
     *                      Le prénom et le nom ne doivent pas être nuls ou vides.
     * @return L'objet {@link MedicalRecord} tel qu'il a été sauvegardé (avec prénom/nom trimés
     *         et listes de médications/allergies non nulles).
     * @throws IllegalArgumentException si {@code medicalRecord} ou son prénom/nom sont nuls ou vides.
     */
    public MedicalRecord save(MedicalRecord medicalRecord) {
        // Validation des entrées minimales
        if (medicalRecord == null || medicalRecord.getFirstName() == null || medicalRecord.getFirstName().isBlank() ||
                medicalRecord.getLastName() == null || medicalRecord.getLastName().isBlank()) {
            throw new IllegalArgumentException("MedicalRecord et son prénom/nom ne peuvent être nuls ou vides.");
        }

        // Nettoyage des identifiants
        String firstNameTrimmed = medicalRecord.getFirstName().trim();
        String lastNameTrimmed = medicalRecord.getLastName().trim();

        // Obtenir une copie mutable de la liste actuelle
        List<MedicalRecord> currentRecords = new ArrayList<>(fileIOService.getMedicalRecords());

        // Supprimer l'ancienne entrée si elle existe (insensible à la casse)
        boolean removed = currentRecords.removeIf(mr ->
                mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        mr.getLastName().equalsIgnoreCase(lastNameTrimmed));

        if (removed) {
            logger.debug("Ancien MedicalRecord pour {} {} supprimé avant sauvegarde.", firstNameTrimmed, lastNameTrimmed);
        }

        // Créer l'objet à sauvegarder en s'assurant de la propreté des données
        MedicalRecord recordToSave = new MedicalRecord();
        recordToSave.setFirstName(firstNameTrimmed);
        recordToSave.setLastName(lastNameTrimmed);
        recordToSave.setBirthdate(medicalRecord.getBirthdate()); // Conserver tel quel
        // Copier les listes ou initialiser si null pour éviter NPE plus tard
        recordToSave.setMedications(medicalRecord.getMedications() != null ? new ArrayList<>(medicalRecord.getMedications()) : new ArrayList<>());
        recordToSave.setAllergies(medicalRecord.getAllergies() != null ? new ArrayList<>(medicalRecord.getAllergies()) : new ArrayList<>());

        // Ajouter la nouvelle version (ou la version mise à jour)
        currentRecords.add(recordToSave);
        logger.debug("Nouveau MedicalRecord pour {} {} ajouté à la liste.", firstNameTrimmed, lastNameTrimmed);

        // Sauvegarder la liste complète
        fileIOService.setMedicalRecords(currentRecords);

        logger.info("MedicalRecord sauvegardé pour : {} {}", firstNameTrimmed, lastNameTrimmed);
        // Retourner l'objet tel qu'il a été ajouté à la liste
        return recordToSave;
    }

    /**
     * Supprime un dossier médical identifié par la combinaison prénom/nom.
     * <p>
     * La recherche du dossier à supprimer est effectuée de manière <b>insensible à la casse</b>
     * et ignore les espaces de début/fin (trim) pour le prénom et le nom fournis.
     * </p><p>
     * Si un dossier est trouvé et supprimé, la liste mise à jour est persistée via
     * {@link FileIOService#setMedicalRecords(List)}.
     * </p>
     *
     * @param firstName Le prénom (insensible à la casse, trimé) de la personne dont le dossier doit être supprimé.
     * @param lastName Le nom de famille (insensible à la casse, trimé) de la personne dont le dossier doit être supprimé.
     * @return {@code true} si un dossier a été trouvé et supprimé, {@code false} sinon (y compris
     *         si le prénom ou le nom fourni est nul ou vide).
     */
    public boolean deleteByFirstNameAndLastName(String firstName, String lastName) {
        // Validation et nettoyage des entrées
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            logger.debug("Tentative de suppression avec prénom ou nom nul ou vide.");
            return false;
        }
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim();

        // Obtenir une copie mutable
        List<MedicalRecord> currentRecords = new ArrayList<>(fileIOService.getMedicalRecords());

        // Essayer de supprimer l'entrée correspondante (insensible à la casse)
        boolean removed = currentRecords.removeIf(mr ->
                mr.getFirstName() != null && mr.getLastName() != null &&
                        mr.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        mr.getLastName().equalsIgnoreCase(lastNameTrimmed));

        // Si suppression réussie, persister la nouvelle liste
        if (removed) {
            fileIOService.setMedicalRecords(currentRecords);
            logger.info("MedicalRecord pour {} {} supprimé.", firstNameTrimmed, lastNameTrimmed);
        } else {
            // Logguer si non trouvé, mais ne rien faire d'autre
            logger.warn("Aucun MedicalRecord trouvé pour {} {} lors de la tentative de suppression.", firstNameTrimmed, lastNameTrimmed);
        }
        // Retourner le résultat de l'opération de suppression
        return removed;
    }
}