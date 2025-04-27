package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.FileIOService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class) // Active Mockito
class MedicalRecordRepositoryTest {

    @Mock // Mock la dépendance
    private FileIOService fileIOService;

    @InjectMocks // Injecte le mock dans le repository testé
    private MedicalRecordRepository medicalRecordRepository;

    @Captor // Capture les arguments passés à setMedicalRecords
    private ArgumentCaptor<List<MedicalRecord>> medicalRecordListCaptor;

    // Données de test
    private MedicalRecord record1, record2, record1Update;

    @BeforeEach
    void setUp() {
        record1 = new MedicalRecord();
        record1.setFirstName("John");
        record1.setLastName("Doe");
        record1.setBirthdate("01/01/1990");
        record1.setMedications(List.of("medA:100mg"));
        record1.setAllergies(List.of("allergyA"));
        record2 = new MedicalRecord();
        record2.setFirstName("Jane");
        record2.setLastName("Smith");
        record2.setBirthdate("02/02/1995");
        record2.setMedications(List.of());
        record2.setAllergies(List.of("allergyB"));
        record1Update = new MedicalRecord(); // Même nom/prénom
        record1Update.setFirstName("John");
        record1Update.setLastName("Doe");
        record1Update.setBirthdate("01/01/1991");
        record1Update.setMedications(List.of("medC:500mg"));
        record1Update.setAllergies(List.of());
    }

    // --- Tests pour findByFirstNameAndLastName ---

    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner le dossier si trouvé (sensible à la casse)")
    void findByFirstNameAndLastName_shouldReturnRecordWhenFoundCaseSensitive() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act
        Optional<MedicalRecord> result = medicalRecordRepository.findByFirstNameAndLastName("John", "Doe");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(record1);

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner Optional vide si non trouvé")
    void findByFirstNameAndLastName_shouldReturnEmptyWhenNotFound() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act
        Optional<MedicalRecord> result = medicalRecordRepository.findByFirstNameAndLastName("Unknown", "Person");

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner Optional vide si la source est vide")
    void findByFirstNameAndLastName_shouldReturnEmptyWhenSourceIsEmpty() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(Collections.emptyList());

        // Act
        Optional<MedicalRecord> result = medicalRecordRepository.findByFirstNameAndLastName("John", "Doe");

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    @Test
    @DisplayName("findByFirstNameAndLastName: Est sensible à la casse (selon le code actuel)")
    void findByFirstNameAndLastName_shouldBeCaseSensitive() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1));

        // Act
        Optional<MedicalRecord> result = medicalRecordRepository.findByFirstNameAndLastName("john", "doe"); // Casse différente

        // Assert
        assertThat(result).isEmpty(); // Devrait être vide car Objects.equals est sensible à la casse

        // Verify
        verify(fileIOService).getMedicalRecords();
    }


    // --- Tests pour findAll ---

    @Test
    @DisplayName("findAll: Doit retourner une copie de la liste de FileIOService")
    void findAll_shouldReturnCopyOfList() {
        // Arrange
        List<MedicalRecord> originalList = new ArrayList<>(List.of(record1, record2));
        when(fileIOService.getMedicalRecords()).thenReturn(originalList);

        // Act
        List<MedicalRecord> resultList = medicalRecordRepository.findAll();

        // Assert
        assertThat(resultList)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(originalList) // Contenu identique
                .isNotSameAs(originalList); // Instance différente (copie)

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    // --- Tests pour existsByFirstNameAndLastName ---

    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner true si trouvé (insensible à la casse)")
    void existsByFirstNameAndLastName_shouldReturnTrueWhenFoundCaseInsensitive() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act & Assert
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("john", "doe")).isTrue();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("JOHN", "DOE")).isTrue();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName(" John ", " Doe ")).isTrue(); // Avec trim

        // Verify
        verify(fileIOService, times(3)).getMedicalRecords();
    }

    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner false si non trouvé")
    void existsByFirstNameAndLastName_shouldReturnFalseWhenNotFound() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act
        boolean exists = medicalRecordRepository.existsByFirstNameAndLastName("Unknown", "Person");

        // Assert
        assertThat(exists).isFalse();

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner false si la source est vide")
    void existsByFirstNameAndLastName_shouldReturnFalseWhenSourceIsEmpty() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(Collections.emptyList());

        // Act
        boolean exists = medicalRecordRepository.existsByFirstNameAndLastName("John", "Doe");

        // Assert
        assertThat(exists).isFalse();

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner false pour nom/prénom nul ou blanc")
    void existsByFirstNameAndLastName_shouldReturnFalseForNullOrBlankNames() {
        // Act & Assert
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName(null, "Doe")).isFalse();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("John", null)).isFalse();

        // Verify: Ne doit pas appeler getMedicalRecords pour null/blank
        verify(fileIOService, never()).getMedicalRecords();
    }


    // --- Tests pour save ---

    @Test
    @DisplayName("save: Doit ajouter un nouveau dossier et appeler setMedicalRecords")
    void save_shouldAddNewRecordAndCallSetter() {
        // Arrange
        MedicalRecord newRecord = new MedicalRecord();
        newRecord.setFirstName(" Peter ");
        newRecord.setLastName(" Pan ");
        newRecord.setBirthdate("03/03/2000");
        newRecord.setMedications(List.of("medD"));
        newRecord.setAllergies(List.of());
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        MedicalRecord savedRecord = medicalRecordRepository.save(newRecord);

        // Assert
        // 1. Vérifier l'objet retourné (trimé)
        assertThat(savedRecord.getFirstName()).isEqualTo("Peter");
        assertThat(savedRecord.getLastName()).isEqualTo("Pan");
        assertThat(savedRecord.getBirthdate()).isEqualTo("03/03/2000");

        // 2. Vérifier l'appel à setMedicalRecords et capturer la liste
        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();

        // 3. Vérifier le contenu de la liste capturée
        assertThat(capturedList).hasSize(2); // initial + nouveau
        assertThat(capturedList).contains(record1); // L'ancien est là
        assertThat(capturedList).anyMatch(mr -> // Le nouveau (trimé) est là
                mr.getFirstName().equals("Peter") &&
                        mr.getLastName().equals("Pan") &&
                        mr.getBirthdate().equals("03/03/2000") &&
                        mr.getMedications().equals(List.of("medD")));

        // Verify counts
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    @Test
    @DisplayName("save: Doit mettre à jour un dossier existant et appeler setMedicalRecords")
    void save_shouldUpdateExistingRecordAndCallSetter() {
        // Arrange
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1, record2));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        MedicalRecord savedRecord = medicalRecordRepository.save(record1Update); // record1Update a le même nom/prénom que record1

        // Assert
        assertThat(savedRecord.getFirstName()).isEqualTo("John");
        assertThat(savedRecord.getLastName()).isEqualTo("Doe");
        assertThat(savedRecord.getBirthdate()).isEqualTo("01/01/1991"); // Données mises à jour

        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();
        assertThat(capturedList).hasSize(2); // Taille inchangée
        assertThat(capturedList).contains(record2); // L'autre enregistrement est toujours là
        // Vérifier que l'enregistrement mis à jour est présent (et que l'ancien n'y est plus)
        assertThat(capturedList).noneMatch(mr ->
                mr.getFirstName().equals("John") && mr.getLastName().equals("Doe") && mr.getBirthdate().equals("01/01/1990"));
        assertThat(capturedList).anyMatch(mr ->
                mr.getFirstName().equals("John") && mr.getLastName().equals("Doe") && mr.getBirthdate().equals("01/01/1991"));

        // Verify counts
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si prénom nul")
    void save_shouldThrowExceptionWhenFirstNameIsNull() {
        // Arrange
        MedicalRecord invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName(null);
        invalidRecord.setLastName("Doe");
        invalidRecord.setBirthdate("01/01/1990");
        invalidRecord.setMedications(List.of());
        invalidRecord.setAllergies(List.of());

        // Act & Assert
        assertThatThrownBy(() -> medicalRecordRepository.save(invalidRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom/nom ne peuvent être nuls ou vides");

        // Verify
        verify(fileIOService, never()).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }

    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si nom blanc")
    void save_shouldThrowExceptionWhenLastNameIsBlank() {
        // Arrange
        MedicalRecord invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName("John");
        invalidRecord.setLastName("   ");
        invalidRecord.setBirthdate("01/01/1990");
        invalidRecord.setMedications(List.of());
        invalidRecord.setAllergies(List.of());

        // Act & Assert
        assertThatThrownBy(() -> medicalRecordRepository.save(invalidRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom/nom ne peuvent être nuls ou vides");

        // Verify
        verify(fileIOService, never()).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }


    // --- Tests pour deleteByFirstNameAndLastName ---

    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit supprimer le dossier et appeler setMedicalRecords si trouvé (insensible à la casse)")
    void deleteByFirstNameAndLastName_shouldDeleteAndCallSetterWhenFoundCaseInsensitive() {
        // Arrange
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1, record2));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        boolean result = medicalRecordRepository.deleteByFirstNameAndLastName(" john ", " DOE "); // Casse différente et espaces

        // Assert
        assertThat(result).isTrue();

        // Vérifier la liste passée à setMedicalRecords
        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();
        assertThat(capturedList)
                .hasSize(1)
                .containsExactly(record2); // Seul l'enregistrement restant

        // Verify
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false et ne pas appeler setMedicalRecords si non trouvé")
    void deleteByFirstNameAndLastName_shouldReturnFalseAndNotCallSetterWhenNotFound() {
        // Arrange
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1, record2));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        boolean result = medicalRecordRepository.deleteByFirstNameAndLastName("Unknown", "Person");

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }

    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false pour nom/prénom nul ou blanc")
    void deleteByFirstNameAndLastName_shouldReturnFalseForNullOrBlankNames() {
        // Act & Assert
        assertThat(medicalRecordRepository.deleteByFirstNameAndLastName(null, "Doe")).isFalse();
        assertThat(medicalRecordRepository.deleteByFirstNameAndLastName("John", null)).isFalse();
        assertThat(medicalRecordRepository.deleteByFirstNameAndLastName("  ", "Doe")).isFalse();
        assertThat(medicalRecordRepository.deleteByFirstNameAndLastName("John", "")).isFalse();

        // Verify
        verify(fileIOService, never()).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }
}