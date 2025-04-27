package com.openclassroom.safetynet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.dto.*; // Importer tous les DTOs
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FireStationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // Ajout import
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Import corrigé
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension; // Optionnel
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Static imports pour la lisibilité
import static org.mockito.ArgumentMatchers.*; // any(), eq(), anyInt(), anyString() etc.
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Classe de test unitaire pour {@link FireStationController}.
 * <p>
 * Utilise {@link WebMvcTest} pour tester la couche contrôleur en isolation,
 * en simulant les requêtes HTTP avec {@link MockMvc} et en mockant la dépendance
 * {@link FireStationService} avec {@link MockBean}.
 * </p>
 */
@WebMvcTest(FireStationController.class) // Cible le contrôleur spécifique
//@ExtendWith(SpringExtension.class) // Non nécessaire avec les annotations Boot Test récentes
public class FireStationControllerTest {

    /**
     * Utilitaire Spring pour simuler les appels HTTP vers le contrôleur.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock de la dépendance FireStationService, injecté par Spring Test.
     * Permet de contrôler le comportement du service pendant les tests.
     */
    @MockBean
    private FireStationService fireStationService;

    /**
     * Utilitaire Jackson (fourni par Spring) pour convertir les objets Java en JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Objets FireStation de test réutilisables
    private FireStation station1;
    private FireStation station2;
    private FireStation stationToUpdate;

    /**
     * Méthode d'initialisation exécutée avant chaque test (@Test).
     * Prépare des objets FireStation de test communs.
     */
    @BeforeEach
    void setUp() {
        station1 = new FireStation();
        station1.setStation("3");
        station1.setAddress("1509 Culver St");

        station2 = new FireStation();
        station2.setStation("3"); // Autre adresse pour la même station
        station2.setAddress("834 Binoc Ave");

        stationToUpdate = new FireStation();
        stationToUpdate.setAddress("1509 Culver St"); // Adresse existante
        stationToUpdate.setStation("5"); // Nouveau numéro de station
    }


    // --- Tests pour GET /firestation?stationNumber=<station_number> ---

    /**
     * Teste l'endpoint GET /firestation avec un numéro de station valide.
     * Doit retourner le statut OK (200) et les données de couverture attendues.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /firestation?stationNumber - Doit retourner OK avec données de couverture si station trouvée")
    void getFireStationByStationNumber_whenValidInput_shouldReturnOk() throws Exception {
        // --- Arrange ---
        // Créer des données DTO exemple pour la réponse attendue
        List<PersonInfoDTO> expectedPeople = Arrays.asList(
                new PersonInfoDTO("Peter", "Duncan", "644 Gershwin Cir", "841-874-6512"),
                new PersonInfoDTO("John", "Smith", "644 Gershwin Cir", "841-874-6512") // Exemple simplifié
        );
        FireStationCoverageDTO expectedCoverageDTO = new FireStationCoverageDTO(expectedPeople, 2, 0); // 2 adultes, 0 enfant

        // Configurer le mock du service
        when(fireStationService.getPeopleCoveredByStation(eq(3))).thenReturn(Optional.of(expectedCoverageDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/firestation")
                        .param("stationNumber", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.adultCount", is(2))) // Vérifier les données du DTO
                .andExpect(jsonPath("$.childCount", is(0)))
                .andExpect(jsonPath("$.people", hasSize(2)))
                .andExpect(jsonPath("$.people[0].firstName", is("Peter")));

        // --- Verify ---
        verify(fireStationService).getPeopleCoveredByStation(eq(3));
    }

    // --- Test spécifique pour GET /phoneAlert?firestation=1 ---

    /**
     * Teste l'endpoint GET /phoneAlert avec le numéro de station 1.
     * Doit retourner le statut OK (200) et la liste spécifique de numéros de téléphone.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /phoneAlert?firestation=1 - Doit retourner OK avec la liste de téléphones attendue")
    void getPhoneNumberByFireStation_whenStation1Exists_shouldReturnSpecificPhoneList() throws Exception {
        // --- Arrange ---
        List<String> expectedPhones = Arrays.asList(
                "841-874-6512", "841-874-8547", "841-874-7462",
                "841-874-7784", "841-874-7784", "841-874-7784", "841-874-6512"
        );
        PhoneAlertDTO expectedPhoneAlertDTO = new PhoneAlertDTO(expectedPhones);
        when(fireStationService.getPhoneNumberByStation(eq(1))).thenReturn(Optional.of(expectedPhoneAlertDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/phoneAlert")
                        .param("firestation", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.phones", hasSize(7)))
                .andExpect(jsonPath("$.phones[0]", is("841-874-6512")))
                .andExpect(jsonPath("$.phones[6]", is("841-874-6512")));

        // --- Verify ---
        verify(fireStationService).getPhoneNumberByStation(eq(1));
    }


    // --- Test spécifique pour GET /flood/stations?stations=1,2 ---

    /**
     * Teste l'endpoint GET /flood/stations avec les numéros de station 1 et 2.
     * Doit retourner le statut OK (200) et la structure de données complexe attendue.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /flood/stations?stations=1,2 - Doit retourner OK avec la structure de données attendue")
    void getFloodData_whenStations1And2Exist_shouldReturnSpecificStructure() throws Exception {
        // --- Arrange ---
        List<String> requestedStations = List.of("1", "2");
        // Construire l'objet DTO complexe attendu (version simplifiée pour le test)
        // Vous devriez idéalement construire l'objet complet comme dans l'exemple précédent
        // ou utiliser une chaîne JSON attendue pour la comparaison.
        ListOfAddressWithListOfPersonWithMedicalRecordDTO expectedFloodDTO = new ListOfAddressWithListOfPersonWithMedicalRecordDTO();
        // Ajouter des données minimales pour vérifier la structure de base
        FireStationAddressWithListOfPersonWithMedicalRecordDTO station1Data = new FireStationAddressWithListOfPersonWithMedicalRecordDTO("1", Collections.emptyList());
        FireStationAddressWithListOfPersonWithMedicalRecordDTO station2Data = new FireStationAddressWithListOfPersonWithMedicalRecordDTO("2", Collections.emptyList());
        expectedFloodDTO.setFireStationAddressPersonMedicalRecords(List.of(station1Data, station2Data));

        when(fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(eq(requestedStations)))
                .thenReturn(Optional.of(expectedFloodDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/flood/stations")
                        .param("stations", "1","2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords", hasSize(2)))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].fireStation", is("1")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].fireStation", is("2")));
        // Ajouter des assertions plus profondes si l'objet DTO arrangé était plus complet

        // --- Verify ---
        verify(fireStationService).getListOfPersonsWithMedicalRecordsByListOfFireStation(eq(requestedStations));
    }

    // --- Tests pour POST /firestation ---

    /**
     * Teste l'ajout d'un mapping station/adresse avec des données valides.
     * Doit retourner le statut Created (201) et le mapping créé.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /firestation - Doit retourner Created si données valides")
    void addFireStation_whenValidInput_shouldReturnCreated() throws Exception {
        // Arrange
        when(fireStationService.addFireStation(any(FireStation.class))).thenReturn(station1);

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.address", is(station1.getAddress())))
                .andExpect(jsonPath("$.station", is(station1.getStation())));

        // Verify
        verify(fireStationService).addFireStation(any(FireStation.class));
    }

    /**
     * Teste l'ajout d'un mapping pour une adresse qui existe déjà.
     * Doit retourner le statut Conflict (409).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /firestation - Doit retourner Conflict si adresse existe déjà")
    void addFireStation_whenAddressExists_shouldReturnConflict() throws Exception {
        // Arrange: Simuler l'exception de conflit
        when(fireStationService.addFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("existe déjà")); // Message clé

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isConflict()); // 409

        // Verify
        verify(fireStationService).addFireStation(any(FireStation.class));
    }

    /**
     * Teste l'ajout d'un mapping avec des données invalides (adresse null).
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /firestation - Doit retourner Bad Request si données invalides")
    void addFireStation_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setStation("1");
        // Simuler l'exception pour données invalides (message différent de "existe déjà")
        when(fireStationService.addFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("Adresse requise"));

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(fireStationService).addFireStation(any(FireStation.class));
    }

    /**
     * Teste l'ajout d'un mapping lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /firestation - Doit retourner Internal Server Error en cas d'erreur service")
    void addFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(fireStationService.addFireStation(any(FireStation.class))).thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(fireStationService).addFireStation(any(FireStation.class));
    }


    // --- Tests pour PUT /firestation ---

    /**
     * Teste la mise à jour d'un mapping existant avec des données valides.
     * Doit retourner le statut OK (200) et le mapping mis à jour.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /firestation - Doit retourner OK si station existe et données valides")
    void updateFireStation_whenStationExists_shouldReturnOk() throws Exception {
        // Arrange
        when(fireStationService.updateFireStation(any(FireStation.class))).thenReturn(Optional.of(stationToUpdate));

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationToUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.address", is(stationToUpdate.getAddress())))
                .andExpect(jsonPath("$.station", is(stationToUpdate.getStation()))); // Vérifier station mise à jour

        // Verify
        verify(fireStationService).updateFireStation(any(FireStation.class));
    }

    /**
     * Teste la mise à jour d'un mapping pour une adresse qui n'existe pas.
     * Doit retourner le statut Not Found (404).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /firestation - Doit retourner Not Found si adresse n'existe pas")
    void updateFireStation_whenStationNotFound_shouldReturnNotFound() throws Exception {
        // Arrange: Simuler service retournant vide
        when(fireStationService.updateFireStation(any(FireStation.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationToUpdate)))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(fireStationService).updateFireStation(any(FireStation.class));
    }

    /**
     * Teste la mise à jour d'un mapping avec des données invalides (station null).
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /firestation - Doit retourner Bad Request si données invalides")
    void updateFireStation_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress("Some Address");
        // Simuler l'exception du service
        when(fireStationService.updateFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("Station requise"));

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(fireStationService).updateFireStation(any(FireStation.class));
    }

    /**
     * Teste la mise à jour d'un mapping lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /firestation - Doit retourner Internal Server Error en cas d'erreur service")
    void updateFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(fireStationService.updateFireStation(any(FireStation.class))).thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationToUpdate)))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(fireStationService).updateFireStation(any(FireStation.class));
    }

    // --- Tests pour DELETE /firestation ---

    /**
     * Teste la suppression d'un mapping existant.
     * Doit retourner le statut No Content (204).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /firestation - Doit retourner No Content si station existe")
    void deleteFireStation_whenStationExists_shouldReturnNoContent() throws Exception {
        // Arrange
        String addressToDelete = "1509 Culver St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete))
                .andExpect(status().isNoContent()); // 204

        // Verify
        verify(fireStationService).deleteFireStationMapping(eq(addressToDelete));
    }

    /**
     * Teste la suppression d'un mapping pour une adresse qui n'existe pas.
     * Doit retourner le statut Not Found (404).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /firestation - Doit retourner Not Found si station n'existe pas")
    void deleteFireStation_whenStationNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        String addressToDelete = "NonExistent St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(fireStationService).deleteFireStationMapping(eq(addressToDelete));
    }

    /**
     * Teste la suppression sans fournir le paramètre 'address'.
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /firestation - Doit retourner Bad Request si paramètre 'address' manquant")
    void deleteFireStation_whenMissingAddressParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/firestation")) // Pas de paramètre 'address'
                .andExpect(status().isBadRequest()); // 400 (Spring rejette car @RequestParam est requis)

        // Verify: Le service ne doit pas être appelé
        verify(fireStationService, never()).deleteFireStationMapping(anyString());
    }

    /**
     * Teste la suppression lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /firestation - Doit retourner Internal Server Error en cas d'erreur service")
    void deleteFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        String addressToDelete = "1509 Culver St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(fireStationService).deleteFireStationMapping(eq(addressToDelete));
    }


    // --- Tests pour GET /firestation/all ---

    /**
     * Teste la récupération de tous les mappings station/adresse.
     * Doit retourner le statut OK (200) et la liste des mappings.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("GET /firestation/all - Doit retourner OK avec la liste des stations")
    void getAllFireStations_shouldReturnOkWithListOfStations() throws Exception {
        // Arrange
        List<FireStation> stationList = Arrays.asList(station1, station2);
        when(fireStationService.getAllFireStations()).thenReturn(stationList);

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].address", is(station1.getAddress())))
                .andExpect(jsonPath("$[1].address", is(station2.getAddress())));

        // Verify
        verify(fireStationService).getAllFireStations();
    }

    /**
     * Teste la récupération de tous les mappings lorsqu'il n'y en a aucun.
     * Doit retourner le statut OK (200) avec une liste vide.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("GET /firestation/all - Doit retourner OK avec liste vide si aucune station")
    void getAllFireStations_whenNoStations_shouldReturnOkWithEmptyList() throws Exception {
        // Arrange
        when(fireStationService.getAllFireStations()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // Liste vide

        // Verify
        verify(fireStationService).getAllFireStations();
    }

    /**
     * Teste la récupération de tous les mappings lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("GET /firestation/all - Doit retourner Internal Server Error en cas d'erreur service")
    void getAllFireStations_whenServiceThrowsError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(fireStationService.getAllFireStations()).thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(fireStationService).getAllFireStations();
    }

    // --- Test Renommé ---
    /**
     * Teste la récupération de tous les mappings (synonyme du test précédent).
     * Doit retourner le statut OK (200).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("GET /firestation/all - Test alias pour getAllFireStations") // Renommé pour éviter confusion
    void getAllFireStationAlias() throws Exception { // Renommé pour éviter conflit
        // Arrange
        when(fireStationService.getAllFireStations()).thenReturn(Collections.emptyList()); // Retourne vide pour simplifier

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // 200

        // Verify
        verify(fireStationService).getAllFireStations();
    }
}