package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.constants.ConfigData;
import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.repository.FireStationRepository;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import com.openclassroom.safetynet.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class) // Active les annotations Mockito (@Mock, @InjectMocks)
class PersonServiceTest {

    @Mock // Crée un mock pour ce repository
    private PersonRepository personRepository;

    @Mock // Crée un mock pour ce repository
    private MedicalRecordRepository medicalRecordRepository;

    @Mock // Crée un mock pour ce repository
    private FireStationRepository fireStationRepository;

    @InjectMocks // Crée une instance de PersonService et injecte les mocks déclarés ci-dessus
    private PersonService personService;

    // Données de test réutilisables
    private Person personAdult1;
    private Person personAdult2;
    private Person personChild1;
    private MedicalRecord mrAdult1;
    private MedicalRecord mrAdult2;
    private MedicalRecord mrChild1;
    private String address1 = "123 Main St";
    private String city1 = "Culver";


    @BeforeEach
    void setUp() {
        // Formatter pour simuler AgeCalculator
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ConfigData.DATE_FORMAT);
        LocalDate now = LocalDate.now();

        personAdult1 = new Person();
        personAdult1.setFirstName("John");
        personAdult1.setLastName("Doe");
        personAdult1.setAddress(address1);
        personAdult1.setCity(city1);
        personAdult1.setZip("97451");
        personAdult1.setPhone("555-111");
        personAdult1.setEmail("john.doe@mail.com");
        personAdult2 = new Person();
        personAdult2.setFirstName("Jane");
        personAdult2.setLastName("Doe");
        personAdult2.setAddress(address1);
        personAdult2.setCity(city1);
        personAdult2.setZip("97451");
        personAdult2.setPhone("555-222");
        personAdult2.setEmail("jane.doe@mail.com");
        personChild1 = new Person(); // Même tel que parent
        personChild1.setFirstName("Tim");
        personChild1.setLastName("Doe");
        personChild1.setAddress(address1);
        personChild1.setCity(city1);
        personChild1.setZip("97451");
        personChild1.setPhone("555-111");
        personChild1.setEmail("john.doe@mail.com");

        // Dates de naissance pour calculer l'âge
        String adultBirthDate1 = now.minusYears(30).format(formatter); // Adulte
        String adultBirthDate2 = now.minusYears(28).format(formatter); // Adulte
        String childBirthDate1 = now.minusYears(10).format(formatter); // Enfant

        mrAdult1 = new MedicalRecord();
        mrAdult1.setFirstName("John");
        mrAdult1.setLastName("Doe");
        mrAdult1.setBirthdate(adultBirthDate1);
        mrAdult1.setMedications(List.of("medA:100mg"));
        mrAdult1.setAllergies(List.of("allergyA"));

        mrAdult2 = new MedicalRecord();
        mrAdult2.setFirstName("Jane");
        mrAdult2.setLastName("Doe");
        mrAdult2.setBirthdate(adultBirthDate2);
        mrAdult2.setMedications(List.of());
        mrAdult2.setAllergies(List.of());
        mrChild1 = new MedicalRecord();
        mrChild1.setFirstName("Tim");
        mrChild1.setLastName("Doe");
        mrChild1.setBirthdate(childBirthDate1);
        mrChild1.setMedications(List.of("medB:50mg"));
        mrChild1.setAllergies(List.of("allergyB"));
    }

    // --- Tests pour getChildAndFamilyByAddress ---

    @Test
    @DisplayName("Test getChildAndFamilyByAddress: Doit retourner les enfants et adultes à l'adresse donnée")
    void getChildAndFamilyByAddress_shouldReturnChildrenAndAdults() {
        // Arrange
        List<Person> peopleAtAddress = List.of(personAdult1, personAdult2, personChild1);
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(peopleAtAddress);
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));
        when(medicalRecordRepository.findByFirstNameAndLastName("Jane", "Doe")).thenReturn(Optional.of(mrAdult2));
        when(medicalRecordRepository.findByFirstNameAndLastName("Tim", "Doe")).thenReturn(Optional.of(mrChild1));

        // Act
        Optional<ChildWithFamilyDTO> resultOpt = personService.getChildAndFamilyByAddress(address1);

        // Assert
        assertThat(resultOpt).isPresent();
        ChildWithFamilyDTO result = resultOpt.get();
        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().get(0).getFirstName()).isEqualTo("Tim");
        assertThat(result.getChildren().get(0).getAge()).isLessThanOrEqualTo(ConfigData.CHILD_AGE_THRESHOLD); // Vérifier l'âge calculé
        assertThat(result.getFamilyMembers()).hasSize(2);
        assertThat(result.getFamilyMembers())
                .extracting(PersonInfoDTO::getFirstName) // Utilise AssertJ pour extraire les prénoms
                .containsExactlyInAnyOrder("John", "Jane");

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(medicalRecordRepository, times(3)).findByFirstNameAndLastName(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getChildAndFamilyByAddress: Doit retourner Optional vide si aucun enfant n'est trouvé")
    void getChildAndFamilyByAddress_shouldReturnEmptyWhenNoChildren() {
        // Arrange
        List<Person> peopleAtAddress = List.of(personAdult1, personAdult2); // Pas d'enfant
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(peopleAtAddress);
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));
        when(medicalRecordRepository.findByFirstNameAndLastName("Jane", "Doe")).thenReturn(Optional.of(mrAdult2));

        // Act
        Optional<ChildWithFamilyDTO> resultOpt = personService.getChildAndFamilyByAddress(address1);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(medicalRecordRepository, times(2)).findByFirstNameAndLastName(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getChildAndFamilyByAddress: Doit retourner Optional vide si aucune personne à l'adresse")
    void getChildAndFamilyByAddress_shouldReturnEmptyWhenNoPeopleAtAddress() {
        // Arrange
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(Collections.emptyList());

        // Act
        Optional<ChildWithFamilyDTO> resultOpt = personService.getChildAndFamilyByAddress(address1);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(medicalRecordRepository, never()).findByFirstNameAndLastName(anyString(), anyString()); // Ne doit pas être appelé
    }

    @Test
    @DisplayName("Test getChildAndFamilyByAddress: Gère les personnes sans dossier médical")
    void getChildAndFamilyByAddress_shouldHandlePeopleWithoutMedicalRecord() {
        // Arrange
        List<Person> peopleAtAddress = List.of(personAdult1, personChild1); // Un avec MR, un sans
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(peopleAtAddress);
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));
        when(medicalRecordRepository.findByFirstNameAndLastName("Tim", "Doe")).thenReturn(Optional.empty()); // Pas de MR pour Tim

        // Act
        Optional<ChildWithFamilyDTO> resultOpt = personService.getChildAndFamilyByAddress(address1);

        // Assert
        // Comme Tim n'a pas de MR, il n'est pas compté comme enfant, donc Optional.empty est retourné
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(medicalRecordRepository, times(2)).findByFirstNameAndLastName(anyString(), anyString());
    }


    // --- Tests pour getPersonFireStationAndMedicalReportByAddress ---

    @Test
    @DisplayName("Test getPersonFireStationAndMedicalReportByAddress: Doit retourner les infos pour une adresse")
    void getPersonFireStationAndMedicalReportByAddress_shouldReturnInfo() {
        // Arrange
        String stationNumber = "3";
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(List.of(personAdult1));
        when(fireStationRepository.findStationNumberByAddress(eq(address1))).thenReturn(stationNumber);
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));

        // Act
        Optional<FirePersonDTO> resultOpt = personService.getPersonFireStationAndMedicalReportByAddress(address1);

        // Assert
        assertThat(resultOpt).isPresent();
        FirePersonDTO result = resultOpt.get();
        assertThat(result.getPersons()).hasSize(1);
        PersonWithMedicalRecordDTO personInfo = result.getPersons().get(0);
        assertThat(personInfo.getLastName()).isEqualTo("Doe");
        assertThat(personInfo.getPhone()).isEqualTo("555-111");
        assertThat(personInfo.getFireStation()).isEqualTo(stationNumber);
        assertThat(personInfo.getAge()).isGreaterThan(ConfigData.CHILD_AGE_THRESHOLD);
        assertThat(personInfo.getMedications()).isEqualTo(mrAdult1.getMedications());
        assertThat(personInfo.getAllergies()).isEqualTo(mrAdult1.getAllergies());

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(fireStationRepository).findStationNumberByAddress(eq(address1));
        verify(medicalRecordRepository).findByFirstNameAndLastName("John", "Doe");
    }

    @Test
    @DisplayName("Test getPersonFireStationAndMedicalReportByAddress: Doit retourner Optional vide si personne à l'adresse")
    void getPersonFireStationAndMedicalReportByAddress_shouldReturnEmptyWhenNoPeople() {
        // Arrange
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(Collections.emptyList());

        // Act
        Optional<FirePersonDTO> resultOpt = personService.getPersonFireStationAndMedicalReportByAddress(address1);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(fireStationRepository, never()).findStationNumberByAddress(anyString());
        verify(medicalRecordRepository, never()).findByFirstNameAndLastName(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getPersonFireStationAndMedicalReportByAddress: Gère si pas de station trouvée")
    void getPersonFireStationAndMedicalReportByAddress_shouldHandleNoStationFound() {
        // Arrange
        when(personRepository.findByAddressIn(eq(List.of(address1)))).thenReturn(List.of(personAdult1));
        when(fireStationRepository.findStationNumberByAddress(eq(address1))).thenReturn(null); // Pas de station
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));

        // Act
        Optional<FirePersonDTO> resultOpt = personService.getPersonFireStationAndMedicalReportByAddress(address1);

        // Assert
        assertThat(resultOpt).isPresent();
        FirePersonDTO result = resultOpt.get();
        assertThat(result.getPersons()).hasSize(1);
        assertThat(result.getPersons().get(0).getFireStation()).isNull(); // Vérifier que le numéro est null

        // Verify
        verify(personRepository).findByAddressIn(anyList());
        verify(fireStationRepository).findStationNumberByAddress(eq(address1));
        verify(medicalRecordRepository).findByFirstNameAndLastName("John", "Doe");
    }


    // --- Tests pour getPersonInfoByLastName ---

    @Test
    @DisplayName("Test getPersonInfoByLastName: Doit retourner les infos pour les personnes trouvées")
    void getPersonInfoByLastName_shouldReturnInfoForFoundPersons() {
        // Arrange
        String lastName = "Doe";
        when(personRepository.findByLastName(eq(lastName))).thenReturn(List.of(personAdult1, personChild1));
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(mrAdult1));
        when(medicalRecordRepository.findByFirstNameAndLastName("Tim", "Doe")).thenReturn(Optional.of(mrChild1));

        // Act
        Optional<ListOfPersonInfolastNameDTO> resultOpt = personService.getPersonInfoByLastName(lastName);

        // Assert
        assertThat(resultOpt).isPresent();
        ListOfPersonInfolastNameDTO result = resultOpt.get();
        assertThat(result.getLastName()).isEqualTo(lastName);
        assertThat(result.getPersonInfoList()).hasSize(2);
        // Vérifier quelques détails, ex: email du premier, âge du second
        assertThat(result.getPersonInfoList().get(0).getEmail()).isEqualTo(personAdult1.getEmail());
        assertThat(result.getPersonInfoList().get(1).getAge()).isLessThanOrEqualTo(ConfigData.CHILD_AGE_THRESHOLD);

        // Verify
        verify(personRepository).findByLastName(eq(lastName));
        verify(medicalRecordRepository, times(2)).findByFirstNameAndLastName(anyString(), eq(lastName));
    }

    @Test
    @DisplayName("Test getPersonInfoByLastName: Doit retourner Optional vide si personne non trouvée")
    void getPersonInfoByLastName_shouldReturnEmptyWhenNotFound() {
        // Arrange
        String lastName = "Unknown";
        when(personRepository.findByLastName(eq(lastName))).thenReturn(Collections.emptyList());

        // Act
        Optional<ListOfPersonInfolastNameDTO> resultOpt = personService.getPersonInfoByLastName(lastName);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByLastName(eq(lastName));
        verify(medicalRecordRepository, never()).findByFirstNameAndLastName(anyString(), anyString());
    }

    // --- Tests pour getCommunityEmailByCity ---

    @Test
    @DisplayName("Test getCommunityEmailByCity: Doit retourner les emails pour la ville donnée")
    void getCommunityEmailByCity_shouldReturnEmailsForCity() {
        // Arrange
        when(personRepository.findByCity(eq(city1))).thenReturn(List.of(personAdult1, personAdult2));

        // Act
        Optional<CommunityEmailDTO> resultOpt = personService.getCommunityEmailByCity(city1);

        // Assert
        assertThat(resultOpt).isPresent();
        CommunityEmailDTO result = resultOpt.get();
        assertThat(result.getCity()).isEqualTo(city1);
        assertThat(result.getEmails())
                .hasSize(2)
                .containsExactlyInAnyOrder(personAdult1.getEmail(), personAdult2.getEmail());

        // Verify
        verify(personRepository).findByCity(eq(city1));
    }

    @Test
    @DisplayName("Test getCommunityEmailByCity: Doit retourner Optional vide si aucune personne dans la ville")
    void getCommunityEmailByCity_shouldReturnEmptyWhenNoPeopleInCity() {
        // Arrange
        String unknownCity = "UnknownCity";
        when(personRepository.findByCity(eq(unknownCity))).thenReturn(Collections.emptyList());

        // Act
        Optional<CommunityEmailDTO> resultOpt = personService.getCommunityEmailByCity(unknownCity);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByCity(eq(unknownCity));
    }

    // --- Tests pour getAllPersons ---

    @Test
    @DisplayName("Test getAllPersons: Doit retourner la liste des personnes du repository")
    void getAllPersons_shouldReturnAllPersons() {
        // Arrange
        List<Person> expectedList = List.of(personAdult1, personAdult2);
        when(personRepository.findAll()).thenReturn(expectedList);

        // Act
        List<Person> actualList = personService.getAllPersons();

        // Assert
        assertThat(actualList).isEqualTo(expectedList);

        // Verify
        verify(personRepository).findAll();
    }

    // --- Tests pour addPerson ---

    @Test
    @DisplayName("Test addPerson: Doit ajouter une personne si elle n'existe pas")
    void addPerson_shouldAddPersonWhenNotExists() {
        // Arrange
        when(personRepository.existsById(eq(personAdult1.getFirstName()), eq(personAdult1.getLastName()))).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenReturn(personAdult1); // Simuler le retour de save

        // Act
        Person addedPerson = personService.addPerson(personAdult1);

        // Assert
        assertThat(addedPerson).isNotNull();
        assertThat(addedPerson.getFirstName()).isEqualTo(personAdult1.getFirstName());

        // Verify
        verify(personRepository).existsById(eq(personAdult1.getFirstName()), eq(personAdult1.getLastName()));
        verify(personRepository).save(eq(personAdult1)); // Vérifier que save est appelé avec le bon objet
    }

    @Test
    @DisplayName("Test addPerson: Doit lancer une exception si la personne existe déjà")
    void addPerson_shouldThrowExceptionWhenExists() {
        // Arrange
        when(personRepository.existsById(eq(personAdult1.getFirstName()), eq(personAdult1.getLastName()))).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> personService.addPerson(personAdult1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // Verify
        verify(personRepository).existsById(eq(personAdult1.getFirstName()), eq(personAdult1.getLastName()));
        verify(personRepository, never()).save(any(Person.class)); // Save ne doit pas être appelé
    }

    // --- Tests pour updatePerson ---

    @Test
    @DisplayName("Test updatePerson: Doit mettre à jour la personne si elle existe")
    void updatePerson_shouldUpdatePersonWhenExists() {
        // Arrange
        Person updatedInfo = new Person();
        updatedInfo.setFirstName("John");
        updatedInfo.setLastName("Doe");
        updatedInfo.setAddress("456 New St");
        updatedInfo.setCity("NewCity");
        updatedInfo.setZip("54321");
        updatedInfo.setPhone("555-999");
        updatedInfo.setEmail("j.doe.new@mail.com");

        // Simuler le retour de la personne existante
        when(personRepository.findByFirstNameAndLastName(eq("John"), eq("Doe"))).thenReturn(Optional.of(personAdult1));
        // Simuler la sauvegarde (on peut juste retourner l'argument passé pour vérifier la mise à jour)
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Person> resultOpt = personService.updatePerson(updatedInfo);

        // Assert
        assertThat(resultOpt).isPresent();
        Person result = resultOpt.get();
        // Vérifier que les champs ont été mis à jour (sauf nom/prénom)
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getAddress()).isEqualTo(updatedInfo.getAddress());
        assertThat(result.getCity()).isEqualTo(updatedInfo.getCity());
        assertThat(result.getZip()).isEqualTo(updatedInfo.getZip());
        assertThat(result.getPhone()).isEqualTo(updatedInfo.getPhone());
        assertThat(result.getEmail()).isEqualTo(updatedInfo.getEmail());


        // Verify
        verify(personRepository).findByFirstNameAndLastName(eq("John"), eq("Doe"));
        verify(personRepository).save(any(Person.class)); // Vérifier que save a été appelé
    }

    @Test
    @DisplayName("Test updatePerson: Doit retourner Optional vide si la personne n'existe pas")
    void updatePerson_shouldReturnEmptyWhenNotExists() {
        // Arrange
        Person nonExistentPerson = new Person();
        nonExistentPerson.setFirstName("Unknown");
        nonExistentPerson.setLastName("Person");
        nonExistentPerson.setAddress("Addr");
        nonExistentPerson.setCity("City");
        nonExistentPerson.setZip("Zip");
        nonExistentPerson.setPhone("Phone");
        nonExistentPerson.setEmail("email");

        when(personRepository.findByFirstNameAndLastName(eq("Unknown"), eq("Person"))).thenReturn(Optional.empty());

        // Act
        Optional<Person> resultOpt = personService.updatePerson(nonExistentPerson);

        // Assert
        assertThat(resultOpt).isEmpty();

        // Verify
        verify(personRepository).findByFirstNameAndLastName(eq("Unknown"), eq("Person"));
        verify(personRepository, never()).save(any(Person.class)); // Save ne doit pas être appelé
    }

    // --- Tests pour deletePerson ---

    @Test
    @DisplayName("Test deletePerson: Doit retourner true si la suppression réussit")
    void deletePerson_shouldReturnTrueWhenDeleted() {
        // Arrange
        when(personRepository.deleteByFirstNameAndLastName(eq("John"), eq("Doe"))).thenReturn(true);

        // Act
        boolean result = personService.deletePerson("John", "Doe");

        // Assert
        assertThat(result).isTrue();

        // Verify
        verify(personRepository).deleteByFirstNameAndLastName(eq("John"), eq("Doe"));
    }

    @Test
    @DisplayName("Test deletePerson: Doit retourner false si la personne n'est pas trouvée")
    void deletePerson_shouldReturnFalseWhenNotFound() {
        // Arrange
        when(personRepository.deleteByFirstNameAndLastName(eq("Unknown"), eq("Person"))).thenReturn(false);

        // Act
        boolean result = personService.deletePerson("Unknown", "Person");

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(personRepository).deleteByFirstNameAndLastName(eq("Unknown"), eq("Person"));
    }
}