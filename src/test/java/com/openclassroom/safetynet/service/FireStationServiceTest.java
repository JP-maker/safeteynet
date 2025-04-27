package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.repository.FireStationRepository;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import com.openclassroom.safetynet.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // Ajout import
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension; // Import corrigé pour JUnit 5

import java.util.*; // Import pour Collections
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link FireStationService}.
 * <p>
 * Utilise Mockito pour simuler les dépendances (repositories et autres services)
 * afin de tester la logique métier du service en isolation.
 * </p>
 */
@ExtendWith(MockitoExtension.class) // Utiliser l'extension Mockito pour JUnit 5
class FireStationServiceTest {

    /** Mock du repository des casernes. */
    @Mock
    private FireStationRepository fireStationRepository;
    /** Mock du repository des personnes. */
    @Mock
    private PersonRepository personRepository;
    /** Mock du repository des dossiers médicaux. */
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    /** Mock du service des personnes (utilisé par une des méthodes). */
    @Mock
    private PersonService personService;

    /** Instance de la classe sous test, avec injection des mocks. */
    @InjectMocks
    private FireStationService fireStationService;

    // Aucune configuration @BeforeEach nécessaire pour ces tests car les mocks
    // sont configurés spécifiquement dans chaque méthode de test.

    /**
     * Teste {@link FireStationService#getPeopleCoveredByStation(int)} dans le cas nominal
     * où la station et les personnes associées sont trouvées.
     * Vérifie que le DTO retourné contient les bonnes informations et les bons comptages.
     */
    @Test
    @DisplayName("getPeopleCoveredByStation: Doit retourner DTO avec personnes et comptages si station trouvée")
    void getPeopleCoveredByStation_shouldReturnCoverage() {
        // Arrange: Définir les données mockées
        int stationNumber = 1;
        List<String> addresses = List.of("123 Main St");
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setAddress("123 Main St");
        person.setCity("City");
        person.setZip("12345");
        person.setPhone("111-222-3333");
        person.setEmail("email@example.com");
        MedicalRecord record = new MedicalRecord(); // Adulte (>18 ans)
        record.setFirstName("John");
        record.setLastName("Doe");
        record.setBirthdate("01/01/1990");
        record.setMedications(List.of());
        record.setAllergies(List.of());

        // Configurer les mocks
        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(addresses);
        when(personRepository.findByAddressIn(addresses)).thenReturn(List.of(person));
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(record));

        // Act: Appeler la méthode à tester
        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        // Assert: Vérifier le résultat
        assertThat(result).isPresent();
        FireStationCoverageDTO dto = result.get();
        assertThat(dto.getPeople()).hasSize(1); // Vérifier la liste de PersonInfoDTO (renommé depuis le test)
        assertThat(dto.getAdultCount()).isEqualTo(1); // John Doe est adulte
        assertThat(dto.getChildCount()).isEqualTo(0);

        // Verify: Vérifier les interactions
        verify(fireStationRepository).findAddressesByStationNumber(stationNumber);
        verify(personRepository).findByAddressIn(addresses);
        verify(medicalRecordRepository).findByFirstNameAndLastName("John", "Doe");
    }

    /**
     * Teste {@link FireStationService#getPeopleCoveredByStation(int)} lorsque le numéro de station
     * ne correspond à aucune adresse dans le repository.
     * Doit retourner un Optional vide.
     */
    @Test
    @DisplayName("getPeopleCoveredByStation: Doit retourner Optional vide si aucune adresse trouvée")
    void getPeopleCoveredByStation_shouldReturnEmptyIfNoAddress() {
        // Arrange
        int stationNumber = 99;
        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(Collections.emptyList());

        // Act
        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(fireStationRepository).findAddressesByStationNumber(stationNumber);
        verify(personRepository, never()).findByAddressIn(anyList()); // Ne doit pas chercher de personnes
        verify(medicalRecordRepository, never()).findByFirstNameAndLastName(anyString(), anyString());
    }

    /**
     * Teste {@link FireStationService#getPhoneNumberByStation(int)} dans le cas nominal.
     * Doit retourner un DTO contenant les numéros de téléphone des personnes couvertes.
     */
    @Test
    @DisplayName("getPhoneNumberByStation: Doit retourner les numéros de téléphone si station trouvée")
    void getPhoneNumberByStation_shouldReturnPhoneNumbers() {
        // Arrange
        int stationNumber = 1;
        List<String> addresses = List.of("123 Main St");
        Person person = new Person();
        person.setFirstName("Jane");
        person.setLastName("Doe");
        person.setAddress("123 Main St");
        person.setCity("City");
        person.setZip("12345");
        person.setPhone("999-888-7777");
        person.setEmail("email@example.com");
        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(addresses);
        when(personRepository.findByAddressIn(addresses)).thenReturn(List.of(person));

        // Act
        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(stationNumber);

        // Assert
        assertThat(result).isPresent();
        // Vérifier le contenu de la liste dans le DTO (renommé depuis le test)
        assertThat(result.get().getPhones()).containsExactly("999-888-7777");

        // Verify
        verify(fireStationRepository).findAddressesByStationNumber(stationNumber);
        verify(personRepository).findByAddressIn(addresses);
    }

    /**
     * Teste {@link FireStationService#getPhoneNumberByStation(int)} lorsque le numéro de station
     * ne correspond à aucune adresse.
     * Doit retourner un Optional vide.
     */
    @Test
    @DisplayName("getPhoneNumberByStation: Doit retourner Optional vide si aucune adresse trouvée")
    void getPhoneNumberByStation_shouldReturnEmptyIfNoAddresses() {
        // Arrange
        when(fireStationRepository.findAddressesByStationNumber(anyInt())).thenReturn(Collections.emptyList());

        // Act
        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(1);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(fireStationRepository).findAddressesByStationNumber(1);
        verify(personRepository, never()).findByAddressIn(anyList());
    }

    /**
     * Teste {@link FireStationService#getListOfPersonsWithMedicalRecordsByListOfFireStation(List)}
     * dans un cas simple avec une station.
     * Doit retourner les données agrégées attendues.
     */
    @Test
    @DisplayName("getListOfPersonsWithMedicalRecordsByListOfFireStation: Doit retourner données agrégées")
    void getListOfPersonsWithMedicalRecordsByListOfFireStation_shouldReturnData() {
        // Arrange
        List<String> stationNumbers = List.of("1");
        List<String> addresses = List.of("123 Main St");
        // Préparer le DTO retourné par le mock personService
        FirePersonDTO firePersonDTO = new FirePersonDTO(List.of(new PersonWithMedicalRecordDTO()));
        // Simuler les appels aux repositories et au service mocké
        when(fireStationRepository.findAddressesByStationNumber(1)).thenReturn(addresses);
        when(personService.getPersonFireStationAndMedicalReportByAddress("123 Main St")).thenReturn(Optional.of(firePersonDTO));

        // Act
        Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> result =
                fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(stationNumbers);

        // Assert
        assertThat(result).isPresent();
        // Vérifier la structure du résultat (renommé depuis le test)
        List<FireStationAddressWithListOfPersonWithMedicalRecordDTO> listResult = result.get().getFireStationAddressPersonMedicalRecords();
        assertThat(listResult).hasSize(1);
        assertThat(listResult.get(0).getFireStation()).isEqualTo("1");
        assertThat(listResult.get(0).getPersons()).hasSize(1); // Vérifier la liste d'AddressWith...DTO
        assertThat(listResult.get(0).getPersons().get(0).getAddress()).isEqualTo("123 Main St");
        assertThat(listResult.get(0).getPersons().get(0).getPersons()).hasSize(1); // Vérifier la liste de PersonWith...DTO

        // Verify
        verify(fireStationRepository).findAddressesByStationNumber(1);
        verify(personService).getPersonFireStationAndMedicalReportByAddress("123 Main St");
    }

    // --- Tests CRUD ---

    /**
     * Teste l'ajout réussi d'un nouveau mapping station/adresse.
     * Vérifie que la méthode save du repository est appelée.
     */
    @Test
    @DisplayName("addFireStation: Doit ajouter le mapping si l'adresse n'existe pas")
    void addFireStation_shouldAddMapping() {
        // Arrange
        FireStation fireStation = new FireStation();
        fireStation.setAddress("123 Main St");
        fireStation.setStation("1");
        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(false); // L'adresse n'existe pas
        when(fireStationRepository.save(fireStation)).thenReturn(fireStation); // Simuler la sauvegarde

        // Act
        FireStation result = fireStationService.addFireStation(fireStation);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        assertThat(result.getStation()).isEqualTo("1");

        // Verify
        verify(fireStationRepository).existsByAddress("123 Main St");
        verify(fireStationRepository).save(fireStation);
    }

    /**
     * Teste l'ajout d'un mapping lorsque l'adresse existe déjà.
     * Doit lancer une {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("addFireStation: Doit lancer IllegalArgumentException si l'adresse existe déjà")
    void addFireStation_shouldThrowExceptionIfAddressExists() {
        // Arrange
        FireStation fireStation = new FireStation();
        fireStation.setAddress("123 Main St");
        fireStation.setStation("1");
        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(true); // L'adresse existe

        // Act & Assert
        assertThatThrownBy(() -> fireStationService.addFireStation(fireStation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Un mapping existe déjà pour l'adresse : 123 Main St"); // Vérifier le message exact ou une partie

        // Verify
        verify(fireStationRepository).existsByAddress("123 Main St");
        verify(fireStationRepository, never()).save(any(FireStation.class)); // Save ne doit pas être appelé
    }

    /**
     * Teste la mise à jour réussie d'un mapping station/adresse existant.
     */
    @Test
    @DisplayName("updateFireStation: Doit mettre à jour le mapping si l'adresse existe")
    void updateFireStation_shouldUpdateMapping() {
        // Arrange
        FireStation fireStation = new FireStation(); // Nouvelle station
        fireStation.setAddress("123 Main St");
        fireStation.setStation("2");
        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(true); // L'adresse existe
        when(fireStationRepository.save(fireStation)).thenReturn(fireStation); // Simuler la sauvegarde

        // Act
        Optional<FireStation> result = fireStationService.updateFireStation(fireStation);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStation()).isEqualTo("2");

        // Verify
        verify(fireStationRepository).existsByAddress("123 Main St");
        verify(fireStationRepository).save(fireStation);
    }

    /**
     * Teste la mise à jour d'un mapping pour une adresse qui n'existe pas.
     * Doit retourner un Optional vide.
     */
    @Test
    @DisplayName("updateFireStation: Doit retourner Optional vide si l'adresse n'existe pas")
    void updateFireStation_shouldReturnEmptyIfAddressNotFound() {
        // Arrange
        FireStation fireStation = new FireStation();
        fireStation.setAddress("Unknown St");
        fireStation.setStation("2");
        when(fireStationRepository.existsByAddress("Unknown St")).thenReturn(false); // L'adresse n'existe pas

        // Act
        Optional<FireStation> result = fireStationService.updateFireStation(fireStation);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(fireStationRepository).existsByAddress("Unknown St");
        verify(fireStationRepository, never()).save(any(FireStation.class)); // Save ne doit pas être appelé
    }

    /**
     * Teste la suppression réussie d'un mapping existant.
     */
    @Test
    @DisplayName("deleteFireStationMapping: Doit retourner true si suppression réussie")
    void deleteFireStationMapping_shouldDeleteSuccessfully() {
        // Arrange
        when(fireStationRepository.deleteByAddress("123 Main St")).thenReturn(true); // Simuler suppression réussie

        // Act
        boolean result = fireStationService.deleteFireStationMapping("123 Main St");

        // Assert
        assertThat(result).isTrue();

        // Verify
        verify(fireStationRepository).deleteByAddress("123 Main St");
    }

    /**
     * Teste la suppression d'un mapping pour une adresse qui n'existe pas.
     */
    @Test
    @DisplayName("deleteFireStationMapping: Doit retourner false si adresse non trouvée")
    void deleteFireStationMapping_shouldReturnFalseIfNotFound() {
        // Arrange
        when(fireStationRepository.deleteByAddress("Unknown St")).thenReturn(false); // Simuler non trouvé

        // Act
        boolean result = fireStationService.deleteFireStationMapping("Unknown St");

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(fireStationRepository).deleteByAddress("Unknown St");
    }


    /**
     * Teste la suppression avec une adresse invalide (vide).
     * Doit retourner false sans appeler le repository.
     */
    @Test
    @DisplayName("deleteFireStationMapping: Doit retourner false si adresse invalide")
    void deleteFireStationMapping_shouldReturnFalseForInvalidAddress() {
        // Act
        boolean result = fireStationService.deleteFireStationMapping("");

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(fireStationRepository, never()).deleteByAddress(anyString()); // Ne doit pas appeler le repo
    }

    /**
     * Teste la récupération de toutes les stations.
     * Doit retourner la liste fournie par le repository.
     */
    @Test
    @DisplayName("getAllFireStations: Doit retourner la liste du repository")
    void getAllFireStations_shouldReturnList() {
        // Arrange
        FireStation fireStation = new FireStation();
        fireStation.setAddress("123 Main St");
        fireStation.setStation("1");
        List<FireStation> fireStations = List.of(fireStation);
        when(fireStationRepository.findAll()).thenReturn(fireStations); // Simuler le retour du repo

        // Act
        List<FireStation> result = fireStationService.getAllFireStations();

        // Assert
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0)).isEqualTo(fireStation);

        // Verify
        verify(fireStationRepository).findAll();
    }
}