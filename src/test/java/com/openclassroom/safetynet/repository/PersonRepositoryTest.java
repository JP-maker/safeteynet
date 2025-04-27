package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.FileIOService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class PersonRepositoryTest {

    @Mock
    private FileIOService fileIOService;

    @InjectMocks
    private PersonRepository personRepository;

    @Captor
    private ArgumentCaptor<List<Person>> personListCaptor;

    // Données de test
    private Person johnDoe, janeDoe, timDoe, peterPan;
    private String addr1 = "1 Main St", addr2 = "2 Oak St", addr3 = "NonExistent St";
    private String city1 = "Culver", city2 = "Springfield";

    @BeforeEach
    void setUp() {
        johnDoe = new Person();
        johnDoe.setFirstName("John");
        johnDoe.setLastName("Doe");
        johnDoe.setAddress(addr1);
        johnDoe.setCity(city1);
        johnDoe.setZip("111");
        johnDoe.setPhone("555-111");
        johnDoe.setEmail("j.doe@mail.com");
        janeDoe = new Person(); // Même nom, même adresse
        janeDoe.setFirstName("Jane");
        janeDoe.setLastName("Doe");
        janeDoe.setAddress(addr1);
        janeDoe.setCity(city1);
        janeDoe.setZip("111");
        janeDoe.setPhone("555-222");
        janeDoe.setEmail("jane.doe@mail.com");

        timDoe = new Person(); // Même nom, adresse différente
        timDoe.setFirstName("Tim");
        timDoe.setLastName("Doe");
        timDoe.setAddress(addr2);
        timDoe.setCity(city1);
        timDoe.setZip("222");
        timDoe.setPhone("555-333");
        timDoe.setEmail("t.doe@mail.com");
        peterPan = new Person(); // Casse différente
        peterPan.setFirstName("peter");
        peterPan.setLastName("pan");
        peterPan.setAddress(addr1);
        peterPan.setCity(city2);
        peterPan.setZip("333");
        peterPan.setPhone("555-444");
        peterPan.setEmail("p.pan@mail.com");
    }

    // --- Tests pour findByAddressIn ---

    @Test
    @DisplayName("findByAddressIn: Doit retourner les personnes aux adresses spécifiées")
    void findByAddressIn_shouldReturnPeopleAtSpecifiedAddresses() {
        // Arrange
        List<Person> allPersons = List.of(johnDoe, janeDoe, timDoe, peterPan);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        List<String> addressesToSearch = List.of(addr1, addr2); // Rechercher addr1 et addr2

        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(addressesToSearch);

        // Assert
        assertThat(foundPersons)
                .hasSize(4) // john, jane (addr1), tim (addr2), peter (addr1)
                .containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe, peterPan);

        // Verify
        verify(fileIOService).getPersons();
    }

    @Test
    @DisplayName("findByAddressIn: Doit être insensible à la casse des adresses d'entrée et stockées")
    void findByAddressIn_shouldBeCaseInsensitive() {
        // Arrange
        Person personLowerAddr = new Person();
        personLowerAddr.setFirstName("Test");
        personLowerAddr.setLastName("User");
        personLowerAddr.setAddress("lower case st");
        personLowerAddr.setCity(city1);
        personLowerAddr.setZip("444");
        personLowerAddr.setPhone("555");
        personLowerAddr.setEmail("t@mail.com");
        List<Person> allPersons = List.of(johnDoe, personLowerAddr);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        List<String> addressesToSearchUpper = List.of("1 MAIN ST"); // Adresse en majuscules
        List<String> addressesToSearchMixed = List.of("Lower Case St"); // Adresse en casse mixte

        // Act
        List<Person> foundUpper = personRepository.findByAddressIn(addressesToSearchUpper);
        List<Person> foundMixed = personRepository.findByAddressIn(addressesToSearchMixed);

        // Assert
        assertThat(foundUpper).containsExactly(johnDoe);
        assertThat(foundMixed).containsExactly(personLowerAddr);

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }


    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si aucune adresse ne correspond")
    void findByAddressIn_shouldReturnEmptyWhenNoMatch() {
        // Arrange
        List<Person> allPersons = List.of(johnDoe, janeDoe, timDoe);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        List<String> addressesToSearch = List.of(addr3); // Adresse inexistante

        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(addressesToSearch);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService).getPersons();
    }

    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si la liste d'adresses en entrée est vide")
    void findByAddressIn_shouldReturnEmptyWhenInputListIsEmpty() {
        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(Collections.emptyList());

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify: Ne doit pas appeler getPersons
        verify(fileIOService, never()).getPersons();
    }

    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si la liste d'adresses en entrée est nulle")
    void findByAddressIn_shouldReturnEmptyWhenInputListIsNull() {
        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(null);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService, never()).getPersons();
    }

    // --- Tests pour findByFirstNameAndLastName ---

    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner la personne si trouvée (sensible à la casse)")
    void findByFirstNameAndLastName_shouldReturnPersonWhenFoundCaseSensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, peterPan));

        // Act
        Optional<Person> resultJohn = personRepository.findByFirstNameAndLastName("John", "Doe");
        Optional<Person> resultPeter = personRepository.findByFirstNameAndLastName("peter", "pan"); // Casse exacte

        // Assert
        assertThat(resultJohn).isPresent().contains(johnDoe);
        assertThat(resultPeter).isPresent().contains(peterPan);

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner Optional vide si non trouvé ou casse différente")
    void findByFirstNameAndLastName_shouldReturnEmptyWhenNotFoundOrDifferentCase() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe));

        // Act
        Optional<Person> resultNotFound = personRepository.findByFirstNameAndLastName("Jane", "Doe");
        Optional<Person> resultDifferentCase = personRepository.findByFirstNameAndLastName("john", "doe");

        // Assert
        assertThat(resultNotFound).isEmpty();
        assertThat(resultDifferentCase).isEmpty(); // Car Objects.equals est sensible à la casse

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    // --- Tests pour findByAddress ---

    @Test
    @DisplayName("findByAddress: Doit retourner les personnes à l'adresse exacte (sensible à la casse)")
    void findByAddress_shouldReturnPeopleAtExactAddressCaseSensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe, timDoe));

        // Act
        List<Person> foundPersons = personRepository.findByAddress(addr1);

        // Assert
        assertThat(foundPersons).hasSize(2).containsExactlyInAnyOrder(johnDoe, janeDoe);

        // Verify
        verify(fileIOService).getPersons();
    }

    @Test
    @DisplayName("findByAddress: Doit retourner une liste vide si l'adresse ne correspond pas ou casse différente")
    void findByAddress_shouldReturnEmptyWhenNoMatchOrDifferentCase() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe));

        // Act
        List<Person> foundNoMatch = personRepository.findByAddress(addr2);
        List<Person> foundDifferentCase = personRepository.findByAddress("1 MAIN ST");

        // Assert
        assertThat(foundNoMatch).isEmpty();
        assertThat(foundDifferentCase).isEmpty();

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    // --- Tests pour findAll ---

    @Test
    @DisplayName("findAll: Doit retourner une copie de la liste de personnes")
    void findAll_shouldReturnCopyOfPersonList() {
        // Arrange
        List<Person> originalList = new ArrayList<>(List.of(johnDoe, timDoe));
        when(fileIOService.getPersons()).thenReturn(originalList);

        // Act
        List<Person> resultList = personRepository.findAll();

        // Assert
        assertThat(resultList)
                .isEqualTo(originalList) // Contenu égal
                .isNotSameAs(originalList); // Instance différente

        // Verify
        verify(fileIOService).getPersons();
    }

    // --- Tests pour findByLastName ---

    @Test
    @DisplayName("findByLastName: Doit retourner les personnes correspondantes (insensible à la casse)")
    void findByLastName_shouldReturnMatchingPeopleCaseInsensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe, timDoe, peterPan));

        // Act
        List<Person> foundDoeLower = personRepository.findByLastName("doe");
        List<Person> foundDoeUpper = personRepository.findByLastName("DOE");
        List<Person> foundPan = personRepository.findByLastName("Pan");

        // Assert
        assertThat(foundDoeLower).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundDoeUpper).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundPan).hasSize(1).containsExactly(peterPan);

        // Verify
        verify(fileIOService, times(3)).getPersons();
    }

    @Test
    @DisplayName("findByLastName: Doit retourner une liste vide si nom non trouvé")
    void findByLastName_shouldReturnEmptyWhenLastNameNotFound() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe));

        // Act
        List<Person> foundPersons = personRepository.findByLastName("Smith");

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService).getPersons();
    }

    // --- Tests pour findByCity ---

    @Test
    @DisplayName("findByCity: Doit retourner les personnes correspondantes (insensible à la casse)")
    void findByCity_shouldReturnMatchingPeopleCaseInsensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe, timDoe, peterPan));

        // Act
        List<Person> foundCulverLower = personRepository.findByCity("culver");
        List<Person> foundCulverUpper = personRepository.findByCity("CULVER");
        List<Person> foundSpringfield = personRepository.findByCity("Springfield");

        // Assert
        assertThat(foundCulverLower).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundCulverUpper).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundSpringfield).hasSize(1).containsExactly(peterPan);

        // Verify
        verify(fileIOService, times(3)).getPersons();
    }

    @Test
    @DisplayName("findByCity: Doit retourner une liste vide si ville non trouvée")
    void findByCity_shouldReturnEmptyWhenCityNotFound() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe));

        // Act
        List<Person> foundPersons = personRepository.findByCity("London");

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService).getPersons();
    }


    // --- Tests pour save ---

    @Test
    @DisplayName("save: Doit ajouter une nouvelle personne et appeler setPersons")
    void save_shouldAddNewPersonAndCallSetter() {
        // Arrange
        Person newPerson = new Person();
        newPerson.setFirstName("Alice");
        newPerson.setLastName("Wonder");
        newPerson.setAddress(addr2);
        newPerson.setCity(city2);
        newPerson.setZip("444");
        newPerson.setPhone("555");
        newPerson.setEmail("a.wonder@mail.com");
        List<Person> initialList = new ArrayList<>(List.of(johnDoe));
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        Person savedPerson = personRepository.save(newPerson);

        // Assert
        assertThat(savedPerson).isEqualTo(newPerson); // Save retourne l'objet d'entrée

        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();
        assertThat(capturedList).hasSize(2).contains(johnDoe, newPerson);

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    @Test
    @DisplayName("save: Doit mettre à jour une personne existante (insensible à la casse)")
    void save_shouldUpdateExistingPersonCaseInsensitive() {
        // Arrange
        Person updatedJohn = new Person();
        updatedJohn.setFirstName("john");
        updatedJohn.setLastName("DOE");
        updatedJohn.setAddress(addr2);
        updatedJohn.setCity(city2);
        updatedJohn.setZip("111-updated");
        updatedJohn.setPhone("555-updated");
        updatedJohn.setEmail("j.doe.updated@mail.com");
        List<Person> initialList = new ArrayList<>(List.of(johnDoe, timDoe)); // johnDoe original
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        Person savedPerson = personRepository.save(updatedJohn);

        // Assert
        assertThat(savedPerson).isEqualTo(updatedJohn);

        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();
        assertThat(capturedList).hasSize(2); // Taille inchangée
        assertThat(capturedList).contains(timDoe); // L'autre personne est là
        assertThat(capturedList).contains(updatedJohn); // La nouvelle version est là
        assertThat(capturedList).doesNotContain(johnDoe); // L'ancienne version n'est plus là

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    // --- Tests pour deleteByFirstNameAndLastName ---

    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit supprimer la personne si trouvée (insensible à la casse) et retourner true")
    void deleteByFirstNameAndLastName_shouldDeleteWhenFoundCaseInsensitiveAndReturnTrue() {
        // Arrange
        List<Person> initialList = new ArrayList<>(List.of(johnDoe, timDoe));
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        boolean result = personRepository.deleteByFirstNameAndLastName(" JOHN ", "doe"); // Casse et espaces différents

        // Assert
        assertThat(result).isTrue();

        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();
        assertThat(capturedList).hasSize(1).containsExactly(timDoe); // Seule personne restante

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false et ne pas appeler setPersons si non trouvé")
    void deleteByFirstNameAndLastName_shouldReturnFalseAndNotCallSetterWhenNotFound() {
        // Arrange
        List<Person> initialList = new ArrayList<>(List.of(johnDoe));
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        boolean result = personRepository.deleteByFirstNameAndLastName("Jane", "Doe");

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(fileIOService).getPersons();
        verify(fileIOService, never()).setPersons(anyList()); // Ne doit pas être appelé
    }

    // --- Tests pour existsById ---

    @Test
    @DisplayName("existsById: Doit retourner true si trouvé (insensible à la casse)")
    void existsById_shouldReturnTrueWhenFoundCaseInsensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, peterPan));

        // Act & Assert
        assertThat(personRepository.existsById("john", "doe")).isTrue();
        assertThat(personRepository.existsById("JOHN", "DOE")).isTrue();
        assertThat(personRepository.existsById(" Peter ", " PAN ")).isTrue();

        // Verify
        verify(fileIOService, times(3)).getPersons();
    }

    @Test
    @DisplayName("existsById: Doit retourner false si non trouvé")
    void existsById_shouldReturnFalseWhenNotFound() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe));

        // Act
        boolean exists = personRepository.existsById("Jane", "Doe");

        // Assert
        assertThat(exists).isFalse();

        // Verify
        verify(fileIOService).getPersons();
    }
}