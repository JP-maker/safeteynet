package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link MedicalRecordService}.
 * <p>
 * Utilise Mockito pour simuler la dépendance {@link MedicalRecordRepository}
 * afin de tester la logique métier du service en isolation.
 * </p>
 */
@ExtendWith(SpringExtension.class)
class MedicalRecordServiceTest {

    /** Mock du repository des dossiers médicaux. */
    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    /** Instance de la classe sous test, avec injection du mock. */
    @InjectMocks
    private MedicalRecordService medicalRecordService;

    // --- Tests pour addMedicalRecord ---

    /**
     * Teste l'ajout réussi d'un nouveau dossier médical lorsque celui-ci
     * n'existe pas déjà.
     * Vérifie que la méthode save du repository est appelée.
     */
    @Test
    @DisplayName("addMedicalRecord: Doit ajouter un nouveau dossier si inexistant")
    void addMedicalRecord_shouldAddNewRecord() {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setFirstName("John");
        medicalRecord.setLastName("Doe");
        medicalRecord.setBirthdate("01/01/1990");
        medicalRecord.setMedications(List.of("med1"));
        medicalRecord.setAllergies(List.of("allergy1"));

        when(medicalRecordRepository.existsByFirstNameAndLastName("John", "Doe")).thenReturn(false);
        when(medicalRecordRepository.save(medicalRecord)).thenReturn(medicalRecord);

        MedicalRecord result = medicalRecordService.addMedicalRecord(medicalRecord);

        assertThat(result).isEqualTo(medicalRecord);
        verify(medicalRecordRepository).save(medicalRecord);
    }

    /**
     * Teste la tentative d'ajout d'un dossier médical pour une personne
     * qui en possède déjà un.
     * Doit lancer une {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("addMedicalRecord: Doit lancer une exception si le dossier existe déjà")
    void addMedicalRecord_shouldThrowException_whenRecordAlreadyExists() {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setFirstName("John");
        medicalRecord.setLastName("Doe");
        medicalRecord.setBirthdate("01/01/1990");
        medicalRecord.setMedications(List.of());
        medicalRecord.setAllergies(List.of());

        when(medicalRecordRepository.existsByFirstNameAndLastName("John", "Doe")).thenReturn(true);

        assertThatThrownBy(() -> medicalRecordService.addMedicalRecord(medicalRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
    }

    /**
     * Teste la tentative d'ajout d'un dossier médical avec des données invalides
     * (prénom ou nom vide/null).
     * Doit lancer une {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("addMedicalRecord: Doit lancer une exception si données invalides (nom/prénom)")
    void addMedicalRecord_shouldThrowException_whenInvalidData() {
        MedicalRecord invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName("");
        invalidRecord.setLastName("");
        invalidRecord.setBirthdate(null);
        invalidRecord.setMedications(null);
        invalidRecord.setAllergies(null);

        assertThatThrownBy(() -> medicalRecordService.addMedicalRecord(invalidRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom et le nom sont requis");
    }

    // --- Tests pour updateMedicalRecord ---

    /**
     * Teste la mise à jour réussie d'un dossier médical existant.
     * Vérifie que les champs modifiables (birthdate, medications, allergies) sont
     * bien mis à jour et que la méthode save du repository est appelée.
     */
    @Test
    @DisplayName("updateMedicalRecord: Doit mettre à jour le dossier existant")
    void updateMedicalRecord_shouldUpdateExistingRecord() {
        MedicalRecord input = new MedicalRecord();
        input.setFirstName("John");
        input.setLastName("Doe");
        input.setBirthdate("01/01/1990");
        input.setMedications(List.of("med2"));
        input.setAllergies(List.of("allergy2"));
        MedicalRecord existing = new MedicalRecord();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setBirthdate("01/02/1990");
        existing.setMedications(List.of("med1"));
        existing.setAllergies(List.of("allergy1"));

        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(existing));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArgument(0));

        Optional<MedicalRecord> result = medicalRecordService.updateMedicalRecord(input);

        assertThat(result).isPresent();
        assertThat(result.get().getBirthdate()).isEqualTo("01/01/1990");
        assertThat(result.get().getMedications()).containsExactly("med2");
        assertThat(result.get().getAllergies()).containsExactly("allergy2");
    }

    /**
     * Teste la tentative de mise à jour d'un dossier médical pour une personne
     * qui n'a pas de dossier existant.
     * Doit retourner un Optional vide sans appeler save.
     */
    @Test
    @DisplayName("updateMedicalRecord: Doit retourner Optional vide si dossier non trouvé")
    void updateMedicalRecord_shouldReturnEmpty_whenRecordNotFound() {
        MedicalRecord input = new MedicalRecord();
        input.setFirstName("Unknown");
        input.setLastName("Person");
        input.setBirthdate("01/01/1990");
        input.setMedications(List.of());
        input.setAllergies(List.of());

        when(medicalRecordRepository.findByFirstNameAndLastName("Unknown", "Person")).thenReturn(Optional.empty());

        Optional<MedicalRecord> result = medicalRecordService.updateMedicalRecord(input);

        assertThat(result).isEmpty();
    }

    /**
     * Teste la tentative de mise à jour d'un dossier médical avec des données invalides
     * (prénom ou nom vide/null dans l'objet d'entrée).
     * Doit lancer une {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("updateMedicalRecord: Doit lancer une exception si données d'entrée invalides")
    void updateMedicalRecord_shouldThrowException_whenInvalidData() {
        MedicalRecord invalid = new MedicalRecord();
        invalid.setFirstName("");
        invalid.setLastName("");
        invalid.setBirthdate(null);
        invalid.setMedications(null);
        invalid.setAllergies(null);

        assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom et le nom sont requis");
    }

    // --- Tests pour deleteMedicalRecord ---

    /**
     * Teste la suppression réussie d'un dossier médical existant.
     * Vérifie que la méthode de suppression du repository est appelée et
     * que le service retourne {@code true}.
     */
    @Test
    @DisplayName("deleteMedicalRecord: Doit retourner true si suppression réussie")
    void deleteMedicalRecord_shouldReturnTrue_whenSuccessful() {
        when(medicalRecordRepository.deleteByFirstNameAndLastName("John", "Doe")).thenReturn(true);

        boolean result = medicalRecordService.deleteMedicalRecord("John", "Doe");

        assertThat(result).isTrue();
    }


    /**
     * Teste la tentative de suppression avec des noms invalides (vides).
     * Doit retourner {@code false} sans appeler le repository.
     */
    @Test
    @DisplayName("deleteMedicalRecord: Doit retourner false si noms invalides")
    void deleteMedicalRecord_shouldReturnFalse_whenInvalidNames() {
        boolean result = medicalRecordService.deleteMedicalRecord("", "");

        assertThat(result).isFalse();
    }

    // --- Tests pour getAllMedicalRecords ---

    /**
     * Teste la récupération de tous les dossiers médicaux.
     * Doit retourner la liste fournie par le repository.
     */
    @Test
    @DisplayName("getAllMedicalRecords: Doit retourner la liste du repository")
    void getAllMedicalRecords_shouldReturnList() {
        MedicalRecord input = new MedicalRecord();
        input.setFirstName("John");
        input.setLastName("Doe");
        input.setBirthdate("01/01/1990");
        input.setMedications(List.of());
        input.setAllergies(List.of());

        List<MedicalRecord> records = List.of(input);

        when(medicalRecordRepository.findAll()).thenReturn(records);

        when(medicalRecordRepository.findAll()).thenReturn(records);

        List<MedicalRecord> result = medicalRecordService.getAllMedicalRecords();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
    }

    // --- Tests pour getMedicalRecord ---

    /**
     * Teste la récupération d'un dossier médical spécifique par nom/prénom
     * lorsque celui-ci est trouvé.
     */
    @Test
    @DisplayName("getMedicalRecord: Doit retourner le dossier si trouvé")
    void getMedicalRecord_shouldReturnRecord_whenFound() {
        MedicalRecord record = new MedicalRecord();
        record.setFirstName("Jane");
        record.setLastName("Doe");
        record.setBirthdate("02/02/1992");
        record.setMedications(List.of());
        record.setAllergies(List.of());

        when(medicalRecordRepository.findByFirstNameAndLastName("Jane", "Doe")).thenReturn(Optional.of(record));

        Optional<MedicalRecord> result = medicalRecordService.getMedicalRecord("Jane", "Doe");

        assertThat(result).isPresent();
        assertThat(result.get().getBirthdate()).isEqualTo("02/02/1992");
    }

    /**
     * Teste la récupération d'un dossier médical spécifique par nom/prénom
     * lorsque celui-ci n'est pas trouvé.
     * Doit retourner un Optional vide.
     */
    @Test
    @DisplayName("getMedicalRecord: Doit retourner Optional vide si non trouvé")
    void getMedicalRecord_shouldReturnEmpty_whenNotFound() {
        when(medicalRecordRepository.findByFirstNameAndLastName("Unknown", "Person")).thenReturn(Optional.empty());

        Optional<MedicalRecord> result = medicalRecordService.getMedicalRecord("Unknown", "Person");

        assertThat(result).isEmpty();
    }
}
