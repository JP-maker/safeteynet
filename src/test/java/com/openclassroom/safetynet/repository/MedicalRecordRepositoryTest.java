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
import org.mockito.junit.jupiter.MockitoExtension; // Correction import extension JUnit 5
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link MedicalRecordRepository}.
 * <p>
 * Utilise Mockito pour simuler la dépendance {@link FileIOService} et tester
 * la logique du repository (filtrage, manipulation de listes, appels au service)
 * en isolation, sans accès réel au système de fichiers.
 * </p>
 */
@ExtendWith(SpringExtension.class) // Active les annotations Mockito pour JUnit 5
class MedicalRecordRepositoryTest {

    /**
     * Mock de la dépendance FileIOService.
     * Simule la source de données des dossiers médicaux.
     */
    @Mock
    private FileIOService fileIOService;

    /**
     * Instance de la classe sous test (MedicalRecordRepository).
     * Le mock {@code fileIOService} sera injecté ici.
     */
    @InjectMocks
    private MedicalRecordRepository medicalRecordRepository;

    /**
     * Captureur d'arguments pour vérifier la liste exacte passée à
     * {@code fileIOService.setMedicalRecords} lors des opérations d'écriture.
     */
    @Captor
    private ArgumentCaptor<List<MedicalRecord>> medicalRecordListCaptor;

    // Données de test réutilisables
    private MedicalRecord record1, record2, record1Update;

    /**
     * Méthode d'initialisation exécutée avant chaque test (@Test).
     * Prépare des objets {@link MedicalRecord} de test.
     */
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

        record1Update = new MedicalRecord(); // Pour tester la mise à jour de record1
        record1Update.setFirstName("John");
        record1Update.setLastName("Doe");
        record1Update.setBirthdate("01/01/1991"); // Date différente
        record1Update.setMedications(List.of("medC:500mg")); // Médicaments différents
        record1Update.setAllergies(List.of()); // Allergies différentes
    }

    // --- Tests pour findByFirstNameAndLastName ---

    /**
     * Teste {@code findByFirstNameAndLastName} lorsqu'un enregistrement correspondant est trouvé.
     * Vérifie que la recherche est sensible à la casse comme implémenté.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner le dossier si trouvé (sensible à la casse)")
    void findByFirstNameAndLastName_shouldReturnRecordWhenFoundCaseSensitive() {
        // Arrange: Simuler la liste retournée par le service
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act: Appeler la méthode à tester
        Optional<MedicalRecord> result = medicalRecordRepository.findByFirstNameAndLastName("John", "Doe");

        // Assert: Vérifier que l'Optional contient le bon enregistrement
        assertThat(result).isPresent().contains(record1);

        // Verify: Vérifier l'interaction avec le mock
        verify(fileIOService).getMedicalRecords();
    }

    /**
     * Teste {@code findByFirstNameAndLastName} lorsqu'aucun enregistrement ne correspond
     * au prénom et nom fournis.
     */
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

    /**
     * Teste {@code findByFirstNameAndLastName} lorsque la source de données est vide.
     */
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

    /**
     * Teste la sensibilité à la casse de {@code findByFirstNameAndLastName}.
     * Devrait retourner vide si la casse ne correspond pas exactement.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit être sensible à la casse")
    void findByFirstNameAndLastName_shouldBeCaseSensitive() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1));

        // Act
        Optional<MedicalRecord> resultLower = medicalRecordRepository.findByFirstNameAndLastName("john", "doe");
        Optional<MedicalRecord> resultUpper = medicalRecordRepository.findByFirstNameAndLastName("JOHN", "DOE");

        // Assert
        assertThat(resultLower).isEmpty(); // Objects.equals est sensible à la casse
        assertThat(resultUpper).isEmpty(); // Objects.equals est sensible à la casse

        // Verify
        verify(fileIOService, times(2)).getMedicalRecords();
    }

    /**
     * Teste {@code findByFirstNameAndLastName} avec des arguments nuls.
     * Devrait retourner Optional vide sans appeler le service.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner Optional vide si arguments nuls")
    void findByFirstNameAndLastName_shouldReturnEmptyForNullArgs() {
        // Act & Assert
        assertThat(medicalRecordRepository.findByFirstNameAndLastName(null, "Doe")).isEmpty();
        assertThat(medicalRecordRepository.findByFirstNameAndLastName("John", null)).isEmpty();
        assertThat(medicalRecordRepository.findByFirstNameAndLastName(null, null)).isEmpty();

        // Verify: Ne doit pas appeler le service si les args sont nuls
        verify(fileIOService, never()).getMedicalRecords();
    }


    // --- Tests pour findAll ---

    /**
     * Teste si {@code findAll} retourne une copie mutable de la liste fournie par
     * le {@code FileIOService}.
     */
    @Test
    @DisplayName("findAll: Doit retourner une copie mutable de la liste de FileIOService")
    void findAll_shouldReturnMutableCopyOfList() {
        // Arrange
        List<MedicalRecord> originalList = List.of(record1, record2); // Peut être immuable
        when(fileIOService.getMedicalRecords()).thenReturn(originalList);

        // Act
        List<MedicalRecord> resultList = medicalRecordRepository.findAll();

        // Assert
        assertThat(resultList)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(originalList) // Contenu égal
                .isNotSameAs(originalList); // Instance différente

        // Verify
        verify(fileIOService).getMedicalRecords();
    }

    // --- Tests pour existsByFirstNameAndLastName ---

    /**
     * Teste si {@code existsByFirstNameAndLastName} retourne {@code true} lorsqu'un enregistrement
     * correspondant est trouvé, en ignorant la casse et les espaces.
     */
    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner true si trouvé (insensible à la casse et trim)")
    void existsByFirstNameAndLastName_shouldReturnTrueWhenFoundCaseInsensitiveAndTrimmed() {
        // Arrange
        when(fileIOService.getMedicalRecords()).thenReturn(List.of(record1, record2));

        // Act & Assert
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("john", "doe")).isTrue();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName(" JOHN ", " DOE ")).isTrue(); // Trim + Casse

        // Verify
        verify(fileIOService, times(2)).getMedicalRecords();
    }

    /**
     * Teste si {@code existsByFirstNameAndLastName} retourne {@code false} lorsqu'aucun enregistrement
     * ne correspond.
     */
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

    /**
     * Teste si {@code existsByFirstNameAndLastName} retourne {@code false} lorsque la source est vide.
     */
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

    /**
     * Teste si {@code existsByFirstNameAndLastName} retourne {@code false} pour des noms/prénoms
     * nuls ou blancs, sans appeler le service.
     */
    @Test
    @DisplayName("existsByFirstNameAndLastName: Doit retourner false pour nom/prénom nul ou blanc sans appeler service")
    void existsByFirstNameAndLastName_shouldReturnFalseForNullOrBlankNamesWithoutServiceCall() {
        // Act & Assert
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName(null, "Doe")).isFalse();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("John", null)).isFalse();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("  ", "Doe")).isFalse();
        assertThat(medicalRecordRepository.existsByFirstNameAndLastName("John", "")).isFalse();

        // Verify: Ne doit pas appeler le service
        verify(fileIOService, never()).getMedicalRecords();
    }


    // --- Tests pour save ---

    /**
     * Teste si {@code save} ajoute correctement un nouvel enregistrement à la liste,
     * nettoie les identifiants (trim), et appelle {@code setMedicalRecords} avec la liste mise à jour.
     */
    @Test
    @DisplayName("save: Doit ajouter un nouveau dossier, nettoyer les identifiants et appeler setMedicalRecords")
    void save_shouldAddNewRecordTrimmedAndCallSetter() {
        // Arrange
        MedicalRecord newRecord = new MedicalRecord();
        newRecord.setFirstName(" Peter "); // Avec espaces
        newRecord.setLastName(" Pan ");   // Avec espaces
        newRecord.setBirthdate("03/03/2000");
        newRecord.setMedications(List.of("medD"));
        newRecord.setAllergies(Collections.emptyList());
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        MedicalRecord savedRecord = medicalRecordRepository.save(newRecord);

        // Assert
        // 1. Vérifier l'objet retourné (trimé et avec listes non nulles)
        assertThat(savedRecord.getFirstName()).isEqualTo("Peter");
        assertThat(savedRecord.getLastName()).isEqualTo("Pan");
        assertThat(savedRecord.getBirthdate()).isEqualTo("03/03/2000");
        assertThat(savedRecord.getMedications()).isNotNull().isEqualTo(List.of("medD"));
        assertThat(savedRecord.getAllergies()).isNotNull().isEmpty();

        // 2. Capturer la liste passée à setMedicalRecords
        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();

        // 3. Vérifier le contenu de la liste capturée
        assertThat(capturedList).hasSize(2); // initial + nouveau
        assertThat(capturedList).contains(record1); // L'ancien est là
        // Le nouveau (trimé, listes non nulles) est là
        assertThat(capturedList).anySatisfy(mr -> {
            assertThat(mr.getFirstName()).isEqualTo("Peter");
            assertThat(mr.getLastName()).isEqualTo("Pan");
            assertThat(mr.getBirthdate()).isEqualTo("03/03/2000");
            assertThat(mr.getMedications()).isEqualTo(List.of("medD"));
            assertThat(mr.getAllergies()).isEmpty();
        });

        // Verify counts
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    /**
     * Teste si {@code save} met à jour correctement un enregistrement existant (basé sur nom/prénom,
     * insensible à la casse), nettoie les identifiants et appelle {@code setMedicalRecords}.
     */
    @Test
    @DisplayName("save: Doit mettre à jour un dossier existant (casse différente), nettoyer et appeler setMedicalRecords")
    void save_shouldUpdateExistingRecordCaseInsensitiveAndCallSetter() {
        // Arrange
        MedicalRecord recordToUpdateWithDiffCase = new MedicalRecord();
        recordToUpdateWithDiffCase.setFirstName(" john "); // Casse et espaces différents
        recordToUpdateWithDiffCase.setLastName(" DOE ");
        recordToUpdateWithDiffCase.setBirthdate("01/01/1991"); // Nouvelle date
        recordToUpdateWithDiffCase.setMedications(null); // Tester la gestion du null
        recordToUpdateWithDiffCase.setAllergies(null);  // Tester la gestion du null

        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1, record2)); // Contient "John", "Doe"
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        MedicalRecord savedRecord = medicalRecordRepository.save(recordToUpdateWithDiffCase);

        // Assert
        // 1. Vérifier l'objet retourné (trimé, listes initialisées)
        assertThat(savedRecord.getFirstName()).isEqualTo("john");
        assertThat(savedRecord.getLastName()).isEqualTo("DOE");
        assertThat(savedRecord.getBirthdate()).isEqualTo("01/01/1991");
        assertThat(savedRecord.getMedications()).isNotNull().isEmpty();
        assertThat(savedRecord.getAllergies()).isNotNull().isEmpty();

        // 2. Capturer la liste
        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();

        // 3. Vérifier la liste capturée
        assertThat(capturedList).hasSize(2); // Taille inchangée
        assertThat(capturedList).contains(record2); // L'autre enregistrement est là
        // Vérifier que l'ancien "John Doe" n'est plus là
        assertThat(capturedList).noneMatch(mr ->
                mr.getFirstName().equals("John") && mr.getLastName().equals("Doe") && mr.getBirthdate().equals("01/01/1990"));
        // Vérifier que la nouvelle version (trimée, listes vides) est là
        assertThat(capturedList).anySatisfy(mr -> {
            assertThat(mr.getFirstName()).isEqualTo("john");
            assertThat(mr.getLastName()).isEqualTo("DOE");
            assertThat(mr.getBirthdate()).isEqualTo("01/01/1991");
            assertThat(mr.getMedications()).isEmpty();
            assertThat(mr.getAllergies()).isEmpty();
        });

        // Verify counts
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    /**
     * Teste si {@code save} lance {@link IllegalArgumentException} si le prénom est nul.
     */
    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si prénom nul")
    void save_shouldThrowExceptionWhenFirstNameIsNull() {
        // Arrange
        MedicalRecord invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName(null);
        invalidRecord.setLastName("Doe");
        // ... autres champs ...

        // Act & Assert
        assertThatThrownBy(() -> medicalRecordRepository.save(invalidRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom/nom ne peuvent être nuls ou vides");

        // Verify: Ne doit pas interagir avec FileIOService
        verify(fileIOService, never()).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }

    /**
     * Teste si {@code save} lance {@link IllegalArgumentException} si le nom est blanc.
     */
    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si nom blanc")
    void save_shouldThrowExceptionWhenLastNameIsBlank() {
        // Arrange
        MedicalRecord invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName("John");
        invalidRecord.setLastName("   ");
        // ... autres champs ...

        // Act & Assert
        assertThatThrownBy(() -> medicalRecordRepository.save(invalidRecord))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prénom/nom ne peuvent être nuls ou vides");

        // Verify
        verify(fileIOService, never()).getMedicalRecords();
        verify(fileIOService, never()).setMedicalRecords(anyList());
    }


    // --- Tests pour deleteByFirstNameAndLastName ---

    /**
     * Teste si {@code deleteByFirstNameAndLastName} supprime l'enregistrement correspondant
     * (insensible à la casse, trim), appelle {@code setMedicalRecords} et retourne {@code true}.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit supprimer (casse/espace différent), appeler setter et retourner true")
    void deleteByFirstNameAndLastName_shouldDeleteCaseInsensitiveTrimmedAndCallSetterAndReturnTrue() {
        // Arrange
        List<MedicalRecord> initialList = new ArrayList<>(List.of(record1, record2));
        when(fileIOService.getMedicalRecords()).thenReturn(initialList);

        // Act
        boolean result = medicalRecordRepository.deleteByFirstNameAndLastName(" john ", " DOE ");

        // Assert
        assertThat(result).isTrue();

        // Vérifier la liste passée à setMedicalRecords
        verify(fileIOService).setMedicalRecords(medicalRecordListCaptor.capture());
        List<MedicalRecord> capturedList = medicalRecordListCaptor.getValue();
        assertThat(capturedList)
                .hasSize(1)
                .containsExactly(record2); // Seul record2 doit rester

        // Verify
        verify(fileIOService).getMedicalRecords();
        verify(fileIOService).setMedicalRecords(anyList());
    }

    /**
     * Teste si {@code deleteByFirstNameAndLastName} retourne {@code false} et n'appelle pas
     * {@code setMedicalRecords} si aucun enregistrement ne correspond.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false et ne pas appeler setter si non trouvé")
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
        verify(fileIOService, never()).setMedicalRecords(anyList()); // Ne doit pas être appelé
    }

    /**
     * Teste si {@code deleteByFirstNameAndLastName} retourne {@code false} pour des noms/prénoms
     * nuls ou blancs, sans appeler le service.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false pour nom/prénom nul ou blanc sans appeler service")
    void deleteByFirstNameAndLastName_shouldReturnFalseForNullOrBlankNamesWithoutServiceCall() {
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