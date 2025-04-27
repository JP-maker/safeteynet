package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.FireStation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Classe de test unitaire pour {@link FireStationRepository}.
 * <p>
 * Utilise Mockito pour simuler la dépendance {@link FileIOService} et ainsi
 * tester la logique du repository (filtrage, manipulation de listes, appels au service)
 * en isolation, sans accès réel au système de fichiers.
 * </p>
 */
@ExtendWith(SpringExtension.class) // Active les annotations Mockito pour JUnit 5
class FireStationRepositoryTest {

    /**
     * Mock de la dépendance FileIOService.
     * Ce mock simulera les lectures et écritures de données.
     */
    @Mock
    private FileIOService fileIOService;

    /**
     * Instance de la classe à tester (FireStationRepository).
     * Mockito injectera automatiquement le mock {@code fileIOService} dans cette instance.
     */
    @InjectMocks
    private FireStationRepository fireStationRepository;

    /**
     * Captureur d'arguments pour vérifier la liste exacte passée à {@code fileIOService.setFireStations}.
     * Utile pour les tests des méthodes {@code save} et {@code deleteByAddress}.
     */
    @Captor
    private ArgumentCaptor<List<FireStation>> fireStationListCaptor;

    // Données de test réutilisables
    private FireStation station1Adr1, station1Adr2, station2Adr3, station1Adr1Duplicate;

    /**
     * Méthode d'initialisation exécutée avant chaque méthode de test (@Test).
     * Prépare des objets {@link FireStation} cohérents pour les scénarios de test.
     */
    @BeforeEach
    void setUp() {
        // Initialiser des objets FireStation avec des données distinctes
        station1Adr1 = new FireStation();
        station1Adr1.setStation("1");
        station1Adr1.setAddress("1 Main St");

        station1Adr2 = new FireStation();
        station1Adr2.setAddress("2 Oak St");
        station1Adr2.setStation("1"); // Même station que station1Addr1

        station2Adr3 = new FireStation();
        station2Adr3.setAddress("3 Pine St");
        station2Adr3.setStation("2");

        station1Adr1Duplicate = new FireStation(); // Même adresse et station que station1Addr1
        station1Adr1Duplicate.setAddress("1 Main St");
        station1Adr1Duplicate.setStation("1");
    }

    // --- Tests pour findAddressesByStationNumber ---

    /**
     * Teste si {@code findAddressesByStationNumber} retourne correctement les adresses distinctes
     * pour un numéro de station donné, en utilisant les données mockées de {@code FileIOService}.
     */
    @Test
    @DisplayName("findAddressesByStationNumber: Doit retourner les adresses distinctes pour une station donnée")
    void findAddressesByStationNumber_shouldReturnDistinctAddressesForStation() {
        // Arrange: Configurer le mock pour retourner une liste contenant des doublons d'adresse pour la station 1
        List<FireStation> mockList = List.of(station1Adr1, station1Adr2, station2Adr3, station1Adr1Duplicate);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act: Appeler la méthode à tester
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(1);

        // Assert: Vérifier que la liste retournée contient les adresses attendues, sans doublons
        assertThat(addresses)
                .isNotNull()
                .hasSize(2) // "1 Main St" et "2 Oak St" seulement
                .containsExactlyInAnyOrder("1 Main St", "2 Oak St");

        // Verify: S'assurer que la méthode getFireStations du mock a été appelée
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code findAddressesByStationNumber} retourne une liste vide lorsqu'aucune
     * station ne correspond au numéro demandé.
     */
    @Test
    @DisplayName("findAddressesByStationNumber: Doit retourner une liste vide si aucune station ne correspond")
    void findAddressesByStationNumber_shouldReturnEmptyListWhenNoMatch() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station1Adr2, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(99); // Numéro inexistant

        // Assert
        assertThat(addresses).isNotNull().isEmpty();

        // Verify
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code findAddressesByStationNumber} retourne une liste vide lorsque
     * la source de données (mockée) est vide.
     */
    @Test
    @DisplayName("findAddressesByStationNumber: Doit retourner une liste vide si la source est vide")
    void findAddressesByStationNumber_shouldReturnEmptyListWhenSourceIsEmpty() {
        // Arrange
        when(fileIOService.getFireStations()).thenReturn(Collections.emptyList());

        // Act
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(1);

        // Assert
        assertThat(addresses).isNotNull().isEmpty();

        // Verify
        verify(fileIOService).getFireStations();
    }

    // --- Tests pour findStationNumberByAddress ---

    /**
     * Teste si {@code findStationNumberByAddress} retourne le bon numéro de station
     * pour une adresse existante (recherche sensible à la casse).
     */
    @Test
    @DisplayName("findStationNumberByAddress: Doit retourner le numéro de station pour une adresse existante")
    void findStationNumberByAddress_shouldReturnStationNumberWhenAddressExists() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        String stationNumber = fireStationRepository.findStationNumberByAddress("1 Main St"); // Adresse exacte

        // Assert
        assertThat(stationNumber).isNotNull().isEqualTo("1");

        // Verify
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code findStationNumberByAddress} retourne null lorsqu'une adresse
     * n'est pas trouvée dans les données mockées.
     */
    @Test
    @DisplayName("findStationNumberByAddress: Doit retourner null si l'adresse n'existe pas")
    void findStationNumberByAddress_shouldReturnNullWhenAddressNotFound() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        String stationNumber = fireStationRepository.findStationNumberByAddress("NonExistent St");

        // Assert
        assertThat(stationNumber).isNull();

        // Verify
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code findStationNumberByAddress} retourne null lorsque l'adresse existe
     * mais avec une casse différente (car la méthode utilise Objects.equals).
     */
    @Test
    @DisplayName("findStationNumberByAddress: Doit retourner null si casse différente (sensible)")
    void findStationNumberByAddress_shouldReturnNullForDifferentCase() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        String stationNumber = fireStationRepository.findStationNumberByAddress("1 MAIN ST"); // Casse différente

        // Assert
        assertThat(stationNumber).isNull(); // Objects.equals est sensible à la casse

        // Verify
        verify(fileIOService).getFireStations();
    }


    /**
     * Teste si {@code findStationNumberByAddress} retourne null lorsque la source de données est vide.
     */
    @Test
    @DisplayName("findStationNumberByAddress: Doit retourner null si la source est vide")
    void findStationNumberByAddress_shouldReturnNullWhenSourceIsEmpty() {
        // Arrange
        when(fileIOService.getFireStations()).thenReturn(Collections.emptyList());

        // Act
        String stationNumber = fireStationRepository.findStationNumberByAddress("1 Main St");

        // Assert
        assertThat(stationNumber).isNull();

        // Verify
        verify(fileIOService).getFireStations();
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
        List<FireStation> originalList = List.of(station1Adr1, station2Adr3); // Peut être immuable ici
        when(fileIOService.getFireStations()).thenReturn(originalList);

        // Act
        List<FireStation> resultList = fireStationRepository.findAll();

        // Assert
        assertThat(resultList)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(originalList) // Le contenu est le même
                .isNotSameAs(originalList); // L'instance est différente (copie)

        // Verify
        verify(fileIOService).getFireStations();
    }

    // --- Tests pour save ---

    /**
     * Teste si {@code save} ajoute correctement une nouvelle station à la liste
     * et appelle {@code setFireStations} avec la liste mise à jour.
     * Vérifie également que les données sont nettoyées (trim).
     */
    @Test
    @DisplayName("save: Doit ajouter une nouvelle station, nettoyer les données et appeler setFireStations")
    void save_shouldAddNewStationAndCallSetFireStationsWithTrimmedData() {
        // Arrange
        FireStation newStation = new FireStation();
        newStation.setAddress(" 4 Elm St "); // Avec espaces
        newStation.setStation(" 3 ");        // Avec espaces
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1)); // Liste mutable pour la simulation interne
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        FireStation savedStation = fireStationRepository.save(newStation);

        // Assert
        // 1. Vérifier l'objet retourné (doit être trimé)
        assertThat(savedStation).isNotNull();
        assertThat(savedStation.getAddress()).isEqualTo("4 Elm St");
        assertThat(savedStation.getStation()).isEqualTo("3");

        // 2. Vérifier l'appel à setFireStations et capturer l'argument
        verify(fileIOService).setFireStations(fireStationListCaptor.capture());

        // 3. Vérifier le contenu de la liste capturée passée à setFireStations
        List<FireStation> capturedList = fireStationListCaptor.getValue();
        assertThat(capturedList).hasSize(2); // Taille = initiale + nouvelle
        // Vérifier que l'élément ajouté est présent et correctement trimé
        assertThat(capturedList).anySatisfy(fs -> {
            assertThat(fs.getAddress()).isEqualTo("4 Elm St");
            assertThat(fs.getStation()).isEqualTo("3");
        });
        // Vérifier que l'élément initial est toujours là
        assertThat(capturedList).contains(station1Adr1);

        // Verify: Vérifier les interactions avec le mock
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }

    /**
     * Teste si {@code save} met à jour correctement une station existante (basé sur l'adresse,
     * insensible à la casse) et appelle {@code setFireStations}.
     * Vérifie également le nettoyage (trim).
     */
    @Test
    @DisplayName("save: Doit mettre à jour une station existante (casse différente), nettoyer et appeler setFireStations")
    void save_shouldUpdateExistingStationCaseInsensitiveAndCallSetFireStations() {
        // Arrange
        FireStation stationToUpdate = new FireStation();
        stationToUpdate.setAddress("  1 MAIN St "); // Casse différente + espaces
        stationToUpdate.setStation(" 99 ");        // Nouveau numéro + espaces
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1, station2Adr3)); // Contient "1 Main St"
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        FireStation savedStation = fireStationRepository.save(stationToUpdate);

        // Assert
        // 1. Vérifier l'objet retourné (trimé)
        assertThat(savedStation.getAddress()).isEqualTo("1 MAIN St"); // Note: trim seulement
        assertThat(savedStation.getStation()).isEqualTo("99");

        // 2. Capturer la liste passée à setFireStations
        verify(fileIOService).setFireStations(fireStationListCaptor.capture());
        List<FireStation> capturedList = fireStationListCaptor.getValue();

        // 3. Vérifier la liste capturée
        assertThat(capturedList).hasSize(2); // Taille inchangée
        assertThat(capturedList).contains(station2Adr3); // L'autre station est là
        // Vérifier que l'ancienne version de "1 Main St" n'est plus là
        assertThat(capturedList).noneMatch(fs -> fs.getAddress().equals("1 Main St") && fs.getStation().equals("1"));
        // Vérifier que la nouvelle version (trimée) est là
        assertThat(capturedList).anySatisfy(fs -> {
            assertThat(fs.getAddress()).isEqualTo("1 MAIN St"); // L'adresse est trimée
            assertThat(fs.getStation()).isEqualTo("99"); // La station est trimée
        });


        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }

    /**
     * Teste si {@code save} lance une {@link IllegalArgumentException} lorsque l'adresse
     * dans l'objet {@code FireStation} est nulle.
     */
    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si l'adresse est nulle")
    void save_shouldThrowExceptionWhenAddressIsNull() {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress(null);
        invalidStation.setStation("1");

        // Act & Assert
        assertThatThrownBy(() -> fireStationRepository.save(invalidStation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adresse et son numéro de station");

        // Verify: Le service ne doit pas être appelé
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    /**
     * Teste si {@code save} lance une {@link IllegalArgumentException} lorsque le numéro de station
     * dans l'objet {@code FireStation} est blanc (espaces uniquement).
     */
    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si le numéro de station est blanc")
    void save_shouldThrowExceptionWhenStationIsBlank() {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress("1 Main St");
        invalidStation.setStation("  "); // Station blanche

        // Act & Assert
        assertThatThrownBy(() -> fireStationRepository.save(invalidStation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adresse et son numéro de station");

        // Verify
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    // --- Tests pour deleteByAddress ---

    /**
     * Teste si {@code deleteByAddress} supprime la station correspondante (insensible à la casse),
     * appelle {@code setFireStations} et retourne {@code true}.
     */
    @Test
    @DisplayName("deleteByAddress: Doit supprimer la station (casse différente), appeler setFireStations et retourner true")
    void deleteByAddress_shouldDeleteCaseInsensitiveAndCallSetterAndReturnTrue() {
        // Arrange
        String addressToDelete = " 1 MAIN St "; // Casse différente et espaces
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1, station2Adr3));
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        boolean result = fireStationRepository.deleteByAddress(addressToDelete);

        // Assert
        assertThat(result).isTrue();

        // Vérifier la liste passée à setFireStations
        verify(fileIOService).setFireStations(fireStationListCaptor.capture());
        List<FireStation> capturedList = fireStationListCaptor.getValue();
        assertThat(capturedList)
                .hasSize(1)
                .containsExactly(station2Adr3); // Seule station2Adr3 doit rester

        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }


    /**
     * Teste si {@code deleteByAddress} retourne {@code false} et n'appelle pas {@code setFireStations}
     * lorsqu'aucune station ne correspond à l'adresse fournie.
     */
    @Test
    @DisplayName("deleteByAddress: Doit retourner false et ne pas appeler setFireStations si non trouvée")
    void deleteByAddress_shouldReturnFalseAndNotCallSetterWhenNotFound() {
        // Arrange
        String addressToDelete = "NonExistent St";
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1, station2Adr3));
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        boolean result = fireStationRepository.deleteByAddress(addressToDelete);

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList()); // Ne doit pas être appelé
    }

    /**
     * Teste si {@code deleteByAddress} retourne {@code false} lorsque l'adresse fournie est
     * nulle, vide ou blanche, sans interagir avec {@code FileIOService}.
     */
    @Test
    @DisplayName("deleteByAddress: Doit retourner false pour une adresse nulle ou blanche sans appeler le service")
    void deleteByAddress_shouldReturnFalseForNullOrBlankAddressWithoutServiceCall() {
        // Act & Assert
        assertThat(fireStationRepository.deleteByAddress(null)).isFalse();
        assertThat(fireStationRepository.deleteByAddress("   ")).isFalse();
        assertThat(fireStationRepository.deleteByAddress("")).isFalse();

        // Verify: FileIOService ne doit pas être appelé
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    // --- Tests pour existsByAddress ---

    /**
     * Teste si {@code existsByAddress} retourne {@code true} lorsqu'une adresse correspondante existe,
     * en ignorant la casse et les espaces de début/fin.
     */
    @Test
    @DisplayName("existsByAddress: Doit retourner true si l'adresse existe (insensible à la casse et trim)")
    void existsByAddress_shouldReturnTrueWhenAddressExistsCaseInsensitiveAndTrimmed() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act & Assert pour différentes variations
        assertThat(fireStationRepository.existsByAddress("1 main st")).isTrue();
        assertThat(fireStationRepository.existsByAddress("1 MAIN ST")).isTrue();
        assertThat(fireStationRepository.existsByAddress("  1 Main St  ")).isTrue(); // Avec espaces

        // Verify
        verify(fileIOService, times(3)).getFireStations(); // Appelée pour chaque vérification
    }

    /**
     * Teste si {@code existsByAddress} retourne {@code false} lorsqu'aucune adresse ne correspond.
     */
    @Test
    @DisplayName("existsByAddress: Doit retourner false si l'adresse n'existe pas")
    void existsByAddress_shouldReturnFalseWhenAddressDoesNotExist() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        boolean exists = fireStationRepository.existsByAddress("NonExistent St");

        // Assert
        assertThat(exists).isFalse();

        // Verify
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code existsByAddress} retourne {@code false} lorsque la source de données est vide.
     */
    @Test
    @DisplayName("existsByAddress: Doit retourner false si la source est vide")
    void existsByAddress_shouldReturnFalseWhenSourceIsEmpty() {
        // Arrange
        when(fileIOService.getFireStations()).thenReturn(Collections.emptyList());

        // Act
        boolean exists = fireStationRepository.existsByAddress("1 Main St");

        // Assert
        assertThat(exists).isFalse();

        // Verify
        verify(fileIOService).getFireStations();
    }

    /**
     * Teste si {@code existsByAddress} retourne {@code false} lorsque l'adresse fournie est
     * nulle, vide ou blanche, sans interagir avec {@code FileIOService}.
     */
    @Test
    @DisplayName("existsByAddress: Doit retourner false pour une adresse nulle ou blanche sans appeler le service")
    void existsByAddress_shouldReturnFalseForNullOrBlankAddressWithoutServiceCall() {
        // Act & Assert
        assertThat(fireStationRepository.existsByAddress(null)).isFalse();
        assertThat(fireStationRepository.existsByAddress("")).isFalse();
        assertThat(fireStationRepository.existsByAddress("   ")).isFalse();

        // Verify: FileIOService ne doit pas être appelé
        verify(fileIOService, never()).getFireStations();
    }
}