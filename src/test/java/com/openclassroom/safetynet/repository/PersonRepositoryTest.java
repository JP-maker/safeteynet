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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link PersonRepository}.
 * <p>
 * Utilise Mockito pour simuler la dépendance {@link FileIOService} et tester
 * la logique du repository (filtrage, manipulation de listes, appels au service)
 * en isolation, sans accès réel au système de fichiers.
 * </p>
 */
@ExtendWith(MockitoExtension.class) // Active les annotations Mockito pour JUnit 5
class PersonRepositoryTest {

    /**
     * Mock de la dépendance FileIOService.
     * Simule la source de données des personnes.
     */
    @Mock
    private FileIOService fileIOService;

    /**
     * Instance de la classe sous test (PersonRepository).
     * Le mock {@code fileIOService} sera injecté ici.
     */
    @InjectMocks
    private PersonRepository personRepository;

    /**
     * Captureur d'arguments pour vérifier la liste exacte passée à
     * {@code fileIOService.setPersons} lors des opérations d'écriture.
     */
    @Captor
    private ArgumentCaptor<List<Person>> personListCaptor;

    // Données de test réutilisables
    private Person johnDoe, janeDoe, timDoe, peterPan;
    private String addr1 = "1 Main St", addr2 = "2 Oak St", addr3 = "NonExistent St";
    private String city1 = "Culver", city2 = "Springfield";

    /**
     * Méthode d'initialisation exécutée avant chaque méthode de test (@Test).
     * Prépare des objets {@link Person} cohérents pour les scénarios de test.
     */
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
        janeDoe.setLastName("Doe"); // Casse différente
        janeDoe.setAddress(addr1); // Casse différente
        janeDoe.setCity(city1); // Casse différente
        janeDoe.setZip("111"); // Casse différente
        janeDoe.setPhone("555-222"); // Casse différente
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
        peterPan.setAddress(addr1); // Casse différente
        peterPan.setCity(city2); // Casse différente
        peterPan.setZip("333"); // Casse différente
        peterPan.setPhone("555-444"); // Casse différente
        peterPan.setEmail("p.pan@mail.com");
    }

    // --- Tests pour findByAddressIn ---

    /**
     * Teste si {@code findByAddressIn} retourne correctement les personnes résidant
     * aux adresses spécifiées, en ignorant la casse.
     */
    @Test
    @DisplayName("findByAddressIn: Doit retourner les personnes aux adresses spécifiées (insensible casse)")
    void findByAddressIn_shouldReturnPeopleAtSpecifiedAddressesCaseInsensitive() {
        // Arrange
        List<Person> allPersons = List.of(johnDoe, janeDoe, timDoe, peterPan);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        // Utiliser une liste mutable pour le test, même si ConvertToUpper crée une nouvelle liste
        List<String> addressesToSearch = new ArrayList<>(List.of("1 MAIN st", addr2)); // Casse différente + existante

        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(addressesToSearch);

        // Assert
        assertThat(foundPersons)
                .hasSize(4) // john, jane, peter (addr1), tim (addr2)
                .containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe, peterPan);

        // Verify
        verify(fileIOService).getPersons();
        // Si ConvertToUpper est utilisé, on pourrait mocker et vérifier son appel statique (plus complexe)
    }

    /**
     * Teste {@code findByAddressIn} lorsque l'adresse d'une personne dans la source est null.
     * Ces personnes ne doivent pas être retournées.
     */
    @Test
    @DisplayName("findByAddressIn: Doit ignorer les personnes avec adresse nulle")
    void findByAddressIn_shouldIgnorePersonsWithNullAddress() {
        // Arrange
        Person personNullAddr = new Person();
        personNullAddr.setFirstName("Null");
        personNullAddr.setLastName("Addr");
        personNullAddr.setAddress(null);
        personNullAddr.setCity(city1);
        personNullAddr.setZip("555");
        personNullAddr.setPhone("555");
        personNullAddr.setEmail("n@a.com");
        List<Person> allPersons = List.of(johnDoe, personNullAddr);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        List<String> addressesToSearch = new ArrayList<>(List.of(addr1));

        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(addressesToSearch);

        // Assert
        assertThat(foundPersons)
                .hasSize(1) // Seul johnDoe doit être trouvé
                .containsExactly(johnDoe);

        // Verify
        verify(fileIOService).getPersons();
    }


    /**
     * Teste si {@code findByAddressIn} retourne une liste vide si aucune adresse ne correspond.
     */
    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si aucune adresse ne correspond")
    void findByAddressIn_shouldReturnEmptyWhenNoMatch() {
        // Arrange
        List<Person> allPersons = List.of(johnDoe, janeDoe, timDoe);
        when(fileIOService.getPersons()).thenReturn(allPersons);
        List<String> addressesToSearch = new ArrayList<>(List.of(addr3)); // Adresse inexistante

        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(addressesToSearch);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService).getPersons();
    }

    /**
     * Teste {@code findByAddressIn} avec une liste d'adresses vide en entrée.
     * Doit retourner une liste vide sans appeler le service.
     */
    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si liste d'adresses vide")
    void findByAddressIn_shouldReturnEmptyWhenInputListIsEmpty() {
        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(Collections.emptyList());

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify: Ne doit pas appeler getPersons
        verify(fileIOService, never()).getPersons();
    }

    /**
     * Teste {@code findByAddressIn} avec une liste d'adresses nulle en entrée.
     * Doit retourner une liste vide sans appeler le service.
     */
    @Test
    @DisplayName("findByAddressIn: Doit retourner une liste vide si liste d'adresses nulle")
    void findByAddressIn_shouldReturnEmptyWhenInputListIsNull() {
        // Act
        List<Person> foundPersons = personRepository.findByAddressIn(null);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService, never()).getPersons();
    }

    // --- Tests pour findByFirstNameAndLastName ---

    /**
     * Teste {@code findByFirstNameAndLastName} lorsqu'une personne correspondante est trouvée.
     * Vérifie la sensibilité à la casse et le trim.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner la personne si trouvée (sensible casse, trim)")
    void findByFirstNameAndLastName_shouldReturnPersonWhenFoundCaseSensitiveTrimmed() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, peterPan));

        // Act
        Optional<Person> resultJohn = personRepository.findByFirstNameAndLastName(" John ", " Doe "); // Avec espaces
        Optional<Person> resultPeter = personRepository.findByFirstNameAndLastName("peter", "pan"); // Casse exacte

        // Assert
        assertThat(resultJohn).isPresent().contains(johnDoe);
        assertThat(resultPeter).isPresent().contains(peterPan);

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    /**
     * Teste {@code findByFirstNameAndLastName} lorsqu'aucune personne ne correspond
     * ou si la casse est différente.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner vide si non trouvé ou casse différente")
    void findByFirstNameAndLastName_shouldReturnEmptyWhenNotFoundOrDifferentCase() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe));

        // Act
        Optional<Person> resultNotFound = personRepository.findByFirstNameAndLastName("Jane", "Doe");
        Optional<Person> resultDifferentCase = personRepository.findByFirstNameAndLastName("john", "doe");

        // Assert
        assertThat(resultNotFound).isEmpty();
        assertThat(resultDifferentCase).isEmpty();

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    /**
     * Teste {@code findByFirstNameAndLastName} avec des arguments nuls.
     * Devrait retourner Optional vide sans appeler le service.
     */
    @Test
    @DisplayName("findByFirstNameAndLastName: Doit retourner Optional vide si arguments nuls")
    void findByFirstNameAndLastName_shouldReturnEmptyForNullArgs() {
        // Act & Assert
        assertThat(personRepository.findByFirstNameAndLastName(null, "Doe")).isEmpty();
        assertThat(personRepository.findByFirstNameAndLastName("John", null)).isEmpty();
        assertThat(personRepository.findByFirstNameAndLastName(null, null)).isEmpty();

        // Verify: Ne doit pas appeler le service si les args sont nuls
        verify(fileIOService, never()).getPersons();
    }


    // --- Tests pour findByAddress ---

    /**
     * Teste {@code findByAddress} lorsqu'une ou plusieurs personnes correspondent exactement
     * à l'adresse (sensible à la casse).
     */
    @Test
    @DisplayName("findByAddress: Doit retourner les personnes à l'adresse exacte (sensible casse)")
    void findByAddress_shouldReturnPeopleAtExactAddressCaseSensitive() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe, timDoe));

        // Act
        List<Person> foundPersons = personRepository.findByAddress(addr1); // "1 Main St"

        // Assert
        assertThat(foundPersons).hasSize(2).containsExactlyInAnyOrder(johnDoe, janeDoe);

        // Verify
        verify(fileIOService).getPersons();
    }

    /**
     * Teste {@code findByAddress} lorsqu'aucune personne ne correspond à l'adresse
     * ou si la casse est différente.
     */
    @Test
    @DisplayName("findByAddress: Doit retourner vide si non trouvé ou casse différente")
    void findByAddress_shouldReturnEmptyWhenNoMatchOrDifferentCase() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe));

        // Act
        List<Person> foundNoMatch = personRepository.findByAddress(addr2); // Adresse différente
        List<Person> foundDifferentCase = personRepository.findByAddress("1 MAIN ST"); // Casse différente

        // Assert
        assertThat(foundNoMatch).isEmpty();
        assertThat(foundDifferentCase).isEmpty();

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    /**
     * Teste {@code findByAddress} avec une adresse nulle.
     * Doit retourner une liste vide sans appeler le service.
     */
    @Test
    @DisplayName("findByAddress: Doit retourner vide si adresse nulle")
    void findByAddress_shouldReturnEmptyIfAddressIsNull() {
        // Act
        List<Person> foundPersons = personRepository.findByAddress(null);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService, never()).getPersons();
    }


    // --- Tests pour findAll ---

    /**
     * Teste si {@code findAll} retourne une copie mutable de la liste des personnes.
     */
    @Test
    @DisplayName("findAll: Doit retourner une copie mutable de la liste de personnes")
    void findAll_shouldReturnMutableCopyOfPersonList() {
        // Arrange
        List<Person> originalList = List.of(johnDoe, timDoe);
        when(fileIOService.getPersons()).thenReturn(originalList);

        // Act
        List<Person> resultList = personRepository.findAll();

        // Assert
        assertThat(resultList)
                .isEqualTo(originalList)
                .isNotSameAs(originalList); // C'est une copie

        // Verify
        verify(fileIOService).getPersons();
    }

    // --- Tests pour findByLastName ---

    /**
     * Teste si {@code findByLastName} retourne les personnes correspondantes
     * en ignorant la casse et les espaces.
     */
    @Test
    @DisplayName("findByLastName: Doit retourner les personnes (insensible casse, trim)")
    void findByLastName_shouldReturnMatchingPeopleCaseInsensitiveTrimmed() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, janeDoe, timDoe, peterPan));

        // Act
        List<Person> foundDoeLower = personRepository.findByLastName("doe");
        List<Person> foundDoeUpper = personRepository.findByLastName("DOE");
        List<Person> foundPan = personRepository.findByLastName(" Pan "); // Avec espaces

        // Assert
        assertThat(foundDoeLower).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundDoeUpper).hasSize(3).containsExactlyInAnyOrder(johnDoe, janeDoe, timDoe);
        assertThat(foundPan).hasSize(1).containsExactly(peterPan);

        // Verify
        verify(fileIOService, times(3)).getPersons();
    }

    /**
     * Teste {@code findByLastName} lorsqu'aucune personne ne correspond.
     */
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

    /**
     * Teste {@code findByLastName} avec un nom nul.
     * Doit retourner une liste vide sans appeler le service.
     */
    @Test
    @DisplayName("findByLastName: Doit retourner vide si nom nul")
    void findByLastName_shouldReturnEmptyWhenLastNameIsNull() {
        // Act
        List<Person> foundPersons = personRepository.findByLastName(null);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService, never()).getPersons();
    }


    // --- Tests pour findByCity ---

    /**
     * Teste si {@code findByCity} retourne les personnes correspondantes
     * en ignorant la casse.
     */
    @Test
    @DisplayName("findByCity: Doit retourner les personnes (insensible casse)")
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

    /**
     * Teste {@code findByCity} lorsqu'aucune personne ne correspond à la ville.
     */
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

    /**
     * Teste {@code findByCity} avec une ville nulle.
     * Doit retourner une liste vide sans appeler le service.
     */
    @Test
    @DisplayName("findByCity: Doit retourner vide si ville nulle")
    void findByCity_shouldReturnEmptyWhenCityIsNull() {
        // Act
        List<Person> foundPersons = personRepository.findByCity(null);

        // Assert
        assertThat(foundPersons).isEmpty();

        // Verify
        verify(fileIOService, never()).getPersons();
    }


    // --- Tests pour save ---

    /**
     * Teste si {@code save} ajoute correctement une nouvelle personne à la liste
     * et appelle {@code setPersons} avec la liste mise à jour.
     */
    @Test
    @DisplayName("save: Doit ajouter une nouvelle personne et appeler setPersons")
    void save_shouldAddNewPersonAndCallSetter() {
        // Arrange
        Person newPerson =new Person();
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
        // 1. Vérifier l'objet retourné (doit être l'objet d'entrée)
        assertThat(savedPerson).isSameAs(newPerson);

        // 2. Capturer la liste passée à setPersons
        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();

        // 3. Vérifier le contenu de la liste capturée
        assertThat(capturedList).hasSize(2).contains(johnDoe, newPerson);

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    /**
     * Teste si {@code save} met à jour correctement une personne existante (basé sur nom/prénom,
     * insensible à la casse) et appelle {@code setPersons}.
     */
    @Test
    @DisplayName("save: Doit mettre à jour une personne existante (insensible casse)")
    void save_shouldUpdateExistingPersonCaseInsensitive() {
        // Arrange
        Person updatedJohn = new Person();
        updatedJohn.setFirstName("john");
        updatedJohn.setLastName("DOE");
        updatedJohn.setAddress(addr2);
        updatedJohn.setCity(city2);
        updatedJohn.setZip("111-upd");
        updatedJohn.setPhone("555-upd");
        updatedJohn.setEmail("j.doe.upd@mail.com");
        List<Person> initialList = new ArrayList<>(List.of(johnDoe, timDoe)); // Contient "John", "Doe"
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        Person savedPerson = personRepository.save(updatedJohn);

        // Assert
        assertThat(savedPerson).isSameAs(updatedJohn);

        // Capturer la liste
        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();

        // Vérifier la liste
        assertThat(capturedList).hasSize(2); // Taille inchangée
        assertThat(capturedList).contains(timDoe); // L'autre est là
        assertThat(capturedList).contains(updatedJohn); // La nouvelle version est là
        assertThat(capturedList).doesNotContain(johnDoe); // L'ancienne version n'est plus là

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    /**
     * Teste le comportement de {@code save} si l'objet Person en entrée est null.
     * Devrait lancer NullPointerException à cause de l'appel à equalsIgnoreCase.
     */
    @Test
    @DisplayName("save: Doit lancer NullPointerException si Person est nulle")
    void save_shouldThrowNullPointerExceptionIfPersonIsNull() {
        // Arrange
        List<Person> initialList = new ArrayList<>(List.of(johnDoe));
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act & Assert
        assertThatThrownBy(() -> personRepository.save(null))
                .isInstanceOf(NullPointerException.class);

        // Verify
        verify(fileIOService).getPersons(); // removeIf est appelé avant l'ajout
        verify(fileIOService, never()).setPersons(anyList());
    }

    // --- Tests pour deleteByFirstNameAndLastName ---

    /**
     * Teste si {@code deleteByFirstNameAndLastName} supprime la personne correspondante
     * (insensible à la casse, trim), appelle {@code setPersons} et retourne {@code true}.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit supprimer (casse/espace différent), appeler setter et retourner true")
    void deleteByFirstNameAndLastName_shouldDeleteCaseInsensitiveTrimmedAndCallSetterAndReturnTrue() {
        // Arrange
        List<Person> initialList = new ArrayList<>(List.of(johnDoe, timDoe));
        when(fileIOService.getPersons()).thenReturn(initialList);

        // Act
        boolean result = personRepository.deleteByFirstNameAndLastName(" JOHN ", "doe"); // Casse et espaces

        // Assert
        assertThat(result).isTrue();

        // Vérifier la liste passée à setPersons
        verify(fileIOService).setPersons(personListCaptor.capture());
        List<Person> capturedList = personListCaptor.getValue();
        assertThat(capturedList).hasSize(1).containsExactly(timDoe); // Seul timDoe reste

        // Verify counts
        verify(fileIOService).getPersons();
        verify(fileIOService).setPersons(anyList());
    }

    /**
     * Teste si {@code deleteByFirstNameAndLastName} retourne {@code false} et n'appelle pas
     * {@code setPersons} si aucune personne ne correspond.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false et ne pas appeler setter si non trouvé")
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
        verify(fileIOService, never()).setPersons(anyList());
    }

    /**
     * Teste {@code deleteByFirstNameAndLastName} avec des noms/prénoms nuls ou blancs.
     * Doit retourner {@code false} sans appeler le service.
     */
    @Test
    @DisplayName("deleteByFirstNameAndLastName: Doit retourner false pour nom/prénom nul ou blanc sans appeler service")
    void deleteByFirstNameAndLastName_shouldReturnFalseForNullOrBlankNamesWithoutServiceCall() {
        // Act & Assert
        assertThat(personRepository.deleteByFirstNameAndLastName(null, "Doe")).isFalse();
        assertThat(personRepository.deleteByFirstNameAndLastName("John", null)).isFalse();
        assertThat(personRepository.deleteByFirstNameAndLastName("  ", "Doe")).isFalse();
        assertThat(personRepository.deleteByFirstNameAndLastName("John", "")).isFalse();

        // Verify
        verify(fileIOService, never()).getPersons();
        verify(fileIOService, never()).setPersons(anyList());
    }


    // --- Tests pour existsById ---

    /**
     * Teste si {@code existsById} retourne {@code true} lorsqu'une personne correspondante
     * est trouvée, en ignorant la casse et les espaces.
     */
    @Test
    @DisplayName("existsById: Doit retourner true si trouvé (insensible casse, trim)")
    void existsById_shouldReturnTrueWhenFoundCaseInsensitiveTrimmed() {
        // Arrange
        when(fileIOService.getPersons()).thenReturn(List.of(johnDoe, peterPan));

        // Act & Assert
        assertThat(personRepository.existsById("john", "doe")).isTrue();
        assertThat(personRepository.existsById(" Peter ", " PAN ")).isTrue(); // Avec espaces et casse différente

        // Verify
        verify(fileIOService, times(2)).getPersons();
    }

    /**
     * Teste si {@code existsById} retourne {@code false} si aucune personne ne correspond.
     */
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

    /**
     * Teste {@code existsById} avec des noms/prénoms nuls ou blancs.
     * Doit retourner {@code false} sans appeler le service.
     */
    @Test
    @DisplayName("existsById: Doit retourner false pour nom/prénom nul ou blanc sans appeler service")
    void existsById_shouldReturnFalseForNullOrBlankNamesWithoutServiceCall() {
        // Act & Assert
        assertThat(personRepository.existsById(null, "Doe")).isFalse();
        assertThat(personRepository.existsById("John", null)).isFalse();
        assertThat(personRepository.existsById("  ", "Doe")).isFalse();
        assertThat(personRepository.existsById("John", "")).isFalse();

        // Verify
        verify(fileIOService, never()).getPersons();
    }
}