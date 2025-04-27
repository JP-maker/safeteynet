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

@ExtendWith(SpringExtension.class) // Active les annotations Mockito
class FireStationRepositoryTest {

    @Mock // Crée un mock de FileIOService
    private FileIOService fileIOService;

    @InjectMocks // Crée une instance de FireStationRepository et injecte le mock fileIOService
    private FireStationRepository fireStationRepository;

    @Captor // Capture les arguments passés aux méthodes mockées
    private ArgumentCaptor<List<FireStation>> fireStationListCaptor;

    // Données de test
    private FireStation station1Adr1, station1Adr2, station2Adr3, station1Adr1Duplicate;

    @BeforeEach
    void setUp() {
        // Initialiser des données cohérentes avant chaque test
        station1Adr1 = new FireStation();
        station1Adr1.setStation("1");
        station1Adr1.setAddress("1 Main St");
        station1Adr2 = new FireStation(); // Même station, adresse différente
        station1Adr2.setAddress("2 Oak St");
        station1Adr2.setStation("1");
        station2Adr3 = new FireStation();
        station2Adr3.setAddress("3 Pine St");
        station2Adr3.setStation("2");
        station1Adr1Duplicate = new FireStation(); // Doublon pour tester distinct
        station1Adr1Duplicate.setAddress("1 Main St");
        station1Adr1Duplicate.setStation("1");
    }

    // --- Tests pour findAddressesByStationNumber ---

    @Test
    @DisplayName("findAddressesByStationNumber: Doit retourner les adresses distinctes pour une station donnée")
    void findAddressesByStationNumber_shouldReturnDistinctAddressesForStation() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station1Adr2, station2Adr3, station1Adr1Duplicate);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(1);

        // Assert
        assertThat(addresses)
                .isNotNull()
                .hasSize(2) // "1 Main St" ne doit apparaître qu'une fois
                .containsExactlyInAnyOrder("1 Main St", "2 Oak St");

        // Verify
        verify(fileIOService).getFireStations();
    }

    @Test
    @DisplayName("findAddressesByStationNumber: Doit retourner une liste vide si aucune station ne correspond")
    void findAddressesByStationNumber_shouldReturnEmptyListWhenNoMatch() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station1Adr2, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        List<String> addresses = fireStationRepository.findAddressesByStationNumber(99); // Station inexistante

        // Assert
        assertThat(addresses).isNotNull().isEmpty();

        // Verify
        verify(fileIOService).getFireStations();
    }

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

    @Test
    @DisplayName("findStationNumberByAddress: Doit retourner le numéro de station pour une adresse existante")
    void findStationNumberByAddress_shouldReturnStationNumberWhenAddressExists() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        String stationNumber = fireStationRepository.findStationNumberByAddress("1 Main St");

        // Assert
        assertThat(stationNumber).isNotNull().isEqualTo("1");

        // Verify
        verify(fileIOService).getFireStations();
    }

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

    @Test
    @DisplayName("findAll: Doit retourner une copie de la liste de FileIOService")
    void findAll_shouldReturnCopyOfList() {
        // Arrange
        List<FireStation> originalList = new ArrayList<>(List.of(station1Adr1, station2Adr3));
        // Simuler le retour d'une liste mutable par getFireStations (même si la nouvelle impl retourne immuable, le repo fait une copie)
        when(fileIOService.getFireStations()).thenReturn(originalList);

        // Act
        List<FireStation> resultList = fireStationRepository.findAll();

        // Assert
        assertThat(resultList)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(originalList) // Contenu égal
                .isNotSameAs(originalList); // Mais instance différente (vérifie la copie)

        // Verify
        verify(fileIOService).getFireStations();
    }

    // --- Tests pour save ---

    @Test
    @DisplayName("save: Doit ajouter une nouvelle station et appeler setFireStations")
    void save_shouldAddNewStationAndCallSetFireStations() {
        // Arrange
        FireStation newStation = new FireStation(); // Avec espaces pour tester trim
        newStation.setAddress("4 Elm St");
        newStation.setStation("3");
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1));
        when(fileIOService.getFireStations()).thenReturn(initialList);
        // Ne pas mocker setFireStations, mais utiliser le captor

        // Act
        FireStation savedStation = fireStationRepository.save(newStation);

        // Assert
        // 1. Vérifier l'objet retourné (doit être trimé)
        assertThat(savedStation).isNotNull();
        assertThat(savedStation.getAddress()).isEqualTo("4 Elm St");
        assertThat(savedStation.getStation()).isEqualTo("3");

        // 2. Vérifier que setFireStations a été appelé
        verify(fileIOService).setFireStations(fireStationListCaptor.capture());

        // 3. Vérifier le contenu de la liste passée à setFireStations
        List<FireStation> capturedList = fireStationListCaptor.getValue();
        assertThat(capturedList).hasSize(2); // L'initiale + la nouvelle
        // Vérifier que l'élément ajouté est présent et correct (trimé)
        assertThat(capturedList).anyMatch(fs ->
                fs.getAddress().equals("4 Elm St") && fs.getStation().equals("3"));
        // Vérifier que l'élément initial est toujours là
        assertThat(capturedList).contains(station1Adr1);

        // Verify
        verify(fileIOService).getFireStations(); // Appelée une fois dans save
        verify(fileIOService).setFireStations(anyList()); // Appelée une fois
    }

    @Test
    @DisplayName("save: Doit mettre à jour une station existante et appeler setFireStations")
    void save_shouldUpdateExistingStationAndCallSetFireStations() {
        // Arrange
        FireStation stationToUpdate = new FireStation(); // Mise à jour station pour "1 Main St"
        stationToUpdate.setAddress("  1 Main St ");
        stationToUpdate.setStation(" 99 ");
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1, station2Adr3));
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        FireStation savedStation = fireStationRepository.save(stationToUpdate);

        // Assert
        assertThat(savedStation.getAddress()).isEqualTo("1 Main St"); // trimé
        assertThat(savedStation.getStation()).isEqualTo("99");    // trimé

        verify(fileIOService).setFireStations(fireStationListCaptor.capture());
        List<FireStation> capturedList = fireStationListCaptor.getValue();
        assertThat(capturedList).hasSize(2); // Taille inchangée
        // Vérifier que l'élément mis à jour est correct
        assertThat(capturedList).anyMatch(fs ->
                fs.getAddress().equals("1 Main St") && fs.getStation().equals("99"));
        // Vérifier que l'autre élément est toujours là
        assertThat(capturedList).contains(station2Adr3);

        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }

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

        // Verify: Ne doit pas appeler les méthodes de FileIOService
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    @Test
    @DisplayName("save: Doit lancer IllegalArgumentException si le numéro de station est blanc")
    void save_shouldThrowExceptionWhenStationIsBlank() {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress("1 Main St");
        invalidStation.setStation("  ");

        // Act & Assert
        assertThatThrownBy(() -> fireStationRepository.save(invalidStation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adresse et son numéro de station");

        // Verify
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    // --- Tests pour deleteByAddress ---

    @Test
    @DisplayName("deleteByAddress: Doit supprimer la station et appeler setFireStations si trouvée")
    void deleteByAddress_shouldDeleteAndCallSetFireStationsWhenFound() {
        // Arrange
        String addressToDelete = "1 Main St";
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
                .containsExactly(station2Adr3); // Ne contient que l'élément restant

        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }

    @Test
    @DisplayName("deleteByAddress: Doit être insensible à la casse pour l'adresse")
    void deleteByAddress_shouldBeCaseInsensitive() {
        // Arrange
        String addressToDelete = " 1 MAIN St "; // Case différente et espaces
        List<FireStation> initialList = new ArrayList<>(List.of(station1Adr1, station2Adr3));
        when(fileIOService.getFireStations()).thenReturn(initialList);

        // Act
        boolean result = fireStationRepository.deleteByAddress(addressToDelete);

        // Assert
        assertThat(result).isTrue();

        verify(fileIOService).setFireStations(fireStationListCaptor.capture());
        List<FireStation> capturedList = fireStationListCaptor.getValue();
        assertThat(capturedList).hasSize(1).containsExactly(station2Adr3);

        // Verify
        verify(fileIOService).getFireStations();
        verify(fileIOService).setFireStations(anyList());
    }


    @Test
    @DisplayName("deleteByAddress: Doit retourner false et ne pas appeler setFireStations si non trouvée")
    void deleteByAddress_shouldReturnFalseAndNotCallSetWhenNotFound() {
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

    @Test
    @DisplayName("deleteByAddress: Doit retourner false pour une adresse nulle ou blanche")
    void deleteByAddress_shouldReturnFalseForNullOrBlankAddress() {
        // Act & Assert
        assertThat(fireStationRepository.deleteByAddress(null)).isFalse();
        assertThat(fireStationRepository.deleteByAddress("   ")).isFalse();
        assertThat(fireStationRepository.deleteByAddress("")).isFalse();

        // Verify: Ne doit jamais appeler FileIOService
        verify(fileIOService, never()).getFireStations();
        verify(fileIOService, never()).setFireStations(anyList());
    }

    // --- Tests pour existsByAddress ---

    @Test
    @DisplayName("existsByAddress: Doit retourner true si l'adresse existe (insensible à la casse)")
    void existsByAddress_shouldReturnTrueWhenAddressExistsCaseInsensitive() {
        // Arrange
        List<FireStation> mockList = List.of(station1Adr1, station2Adr3);
        when(fileIOService.getFireStations()).thenReturn(mockList);

        // Act
        boolean existsLower = fireStationRepository.existsByAddress("1 main st");
        boolean existsUpper = fireStationRepository.existsByAddress("1 MAIN ST");
        boolean existsMixed = fireStationRepository.existsByAddress("1 MaIn St");
        boolean existsWithSpace = fireStationRepository.existsByAddress("  1 Main St  ");

        // Assert
        assertThat(existsLower).isTrue();
        assertThat(existsUpper).isTrue();
        assertThat(existsMixed).isTrue();
        assertThat(existsWithSpace).isTrue(); // Trim est appliqué

        // Verify
        verify(fileIOService, times(4)).getFireStations();
    }

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

    @Test
    @DisplayName("existsByAddress: Doit retourner false pour une adresse nulle ou blanche")
    void existsByAddress_shouldReturnFalseForNullOrBlankAddress() {
        // Act & Assert
        assertThat(fireStationRepository.existsByAddress(null)).isFalse();
        assertThat(fireStationRepository.existsByAddress("")).isFalse();
        assertThat(fireStationRepository.existsByAddress("   ")).isFalse();

        // Verify: Ne doit pas appeler getFireStations pour null/blank
        verify(fileIOService, never()).getFireStations();
    }
}