package com.openclassroom.safetynet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.controller.FireStationController;
import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.service.FireStationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Static imports pour la lisibilité
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*; // Inclut verify, never, times etc.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize; // Pour vérifier la taille des listes JSON
import static org.hamcrest.Matchers.is; // Pour vérifier les valeurs JSON

@WebMvcTest(controllers = {FireStationController.class}) // Cible le contrôleur à tester
@ExtendWith(SpringExtension.class)
public class FireStationControllerTest {

    @Autowired
    private MockMvc mockMvc; // Pour simuler les requêtes HTTP

    @MockitoBean // Crée un mock du service injecté dans le contrôleur
    private FireStationService fireStationService;

    @Autowired
    private ObjectMapper objectMapper; // Pour la sérialisation/désérialisation JSON

    private FireStation station1;
    private FireStation station2;
    private FireStation stationToUpdate;

    @BeforeEach
    void setUp() {
        // Initialiser des données de test communes
        station1 = new FireStation();
        station1.setStation("3");
        station1.setAddress("1509 Culver St");
        station2 = new FireStation();
        station2.setStation("3");
        station2.setAddress("834 Binoc Ave");
        stationToUpdate = new FireStation(); // Même adresse, station différente
        stationToUpdate.setAddress("1509 Culver St");
        stationToUpdate.setStation("5");
    }


    // --- Tests pour GET /firestation?stationNumber=<station_number> ---

    @Test
    void getFireStationByStationNumber_whenValidInput_shouldReturnOk() throws Exception {
        // --- Arrange ---
        // 1. Créer la liste de PersonCoveredDTO attendue
        List<PersonInfoDTO> expectedPeople = Arrays.asList(
                new PersonInfoDTO("Peter", "Duncan", "644 Gershwin Cir", "841-874-6512"),
                new PersonInfoDTO("Reginold", "Walker", "908 73rd St", "841-874-8547"),
                new PersonInfoDTO("Jamie", "Peters", "908 73rd St", "841-874-7462"),
                new PersonInfoDTO("Brian", "Stelzer", "947 E. Rose Dr", "841-874-7784"),
                new PersonInfoDTO("Shawna", "Stelzer", "947 E. Rose Dr", "841-874-7784"),
                new PersonInfoDTO("Kendrik", "Stelzer", "947 E. Rose Dr", "841-874-7784"),
                new PersonInfoDTO("John", "Smith", "644 Gershwin Cir", "841-874-6512")
        );

        // 2. Créer le DTO de couverture attendu
        FireStationCoverageDTO expectedCoverageDTO = new FireStationCoverageDTO(expectedPeople, 5, 1); // 5 adultes, 1 enfant

        // 3. Configurer le mock du service pour retourner ce DTO quand la station 3 est demandée
        when(fireStationService.getPeopleCoveredByStation(eq(3))).thenReturn(Optional.of(expectedCoverageDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/firestation")
                        .param("stationNumber", "3") // Paramètre de la requête
                        .accept(MediaType.APPLICATION_JSON)) // Indique qu'on attend du JSON
                // Vérifier le statut et le type de contenu
                .andExpect(status().isOk()) // Statut 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Vérifier les compteurs globaux
                .andExpect(jsonPath("$.adultCount", is(5)))
                .andExpect(jsonPath("$.childCount", is(1)))
                // Vérifier la taille de la liste "people"
                .andExpect(jsonPath("$.people", hasSize(7)))
                // Vérifier quelques éléments clés dans la liste pour s'assurer de l'ordre et des données
                // Vérifier le premier élément
                .andExpect(jsonPath("$.people[0].firstName", is("Peter")))
                .andExpect(jsonPath("$.people[0].lastName", is("Duncan")))
                .andExpect(jsonPath("$.people[0].address", is("644 Gershwin Cir")))
                .andExpect(jsonPath("$.people[0].phone", is("841-874-6512")))
                // Vérifier un élément au milieu (par exemple, le 4ème, index 3)
                .andExpect(jsonPath("$.people[3].firstName", is("Brian")))
                .andExpect(jsonPath("$.people[3].lastName", is("Stelzer")))
                // Vérifier le dernier élément (index 6)
                .andExpect(jsonPath("$.people[6].firstName", is("John")))
                .andExpect(jsonPath("$.people[6].lastName", is("Smith")))
                .andExpect(jsonPath("$.people[6].address", is("644 Gershwin Cir")))
                .andExpect(jsonPath("$.people[6].phone", is("841-874-6512")));


        // --- Verify ---
        // Vérifier que la méthode du service a été appelée correctement
        verify(fireStationService).getPeopleCoveredByStation(eq(3));
    }


    // --- Test spécifique pour GET /phoneAlert?firestation=1 ---

    @Test
    void getPhoneNumberByFireStation_whenStation1Exists_shouldReturnSpecificPhoneList() throws Exception {
        // --- Arrange ---
        // 1. Créer la liste de numéros de téléphone attendue
        List<String> expectedPhones = Arrays.asList(
                "841-874-6512",
                "841-874-8547",
                "841-874-7462",
                "841-874-7784",
                "841-874-7784", // Note: Les doublons sont inclus comme dans l'exemple
                "841-874-7784",
                "841-874-6512"
        );

        // 2. Créer le DTO attendu (en supposant que le champ s'appelle 'phones')
        PhoneAlertDTO expectedPhoneAlertDTO = new PhoneAlertDTO(expectedPhones);

        // 3. Configurer le mock du service pour retourner ce DTO quand la station 1 est demandée
        when(fireStationService.getPhoneNumberByStation(eq(1))).thenReturn(Optional.of(expectedPhoneAlertDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/phoneAlert")
                        .param("firestation", "1") // Paramètre de la requête
                        .accept(MediaType.APPLICATION_JSON)) // Indique qu'on attend du JSON
                // Vérifier le statut et le type de contenu
                .andExpect(status().isOk()) // Statut 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Vérifier la structure et le contenu JSON
                .andExpect(jsonPath("$.phones", hasSize(7))) // Vérifie la taille du tableau 'phones'
                .andExpect(jsonPath("$.phones[0]", is("841-874-6512"))) // Vérifie le premier élément
                .andExpect(jsonPath("$.phones[1]", is("841-874-8547"))) // Vérifie le deuxième
                .andExpect(jsonPath("$.phones[3]", is("841-874-7784"))) // Vérifie un des doublons
                .andExpect(jsonPath("$.phones[6]", is("841-874-6512"))); // Vérifie le dernier élément

        // --- Verify ---
        // Vérifier que la méthode du service a été appelée correctement
        verify(fireStationService).getPhoneNumberByStation(eq(1));
    }


    // --- Test spécifique pour GET /flood/stations?stations=1,2 ---

    @Test
    void getFloodData_whenStations1And2Exist_shouldReturnSpecificStructure() throws Exception {
        // --- Arrange ---
        // 1. Définir les numéros de station demandés
        List<String> requestedStations = List.of("1", "2");

        // 2. Construire l'objet DTO complexe attendu
        // Personnes pour Station 1, Adresse 644 Gershwin Cir
        PersonWithMedicalRecordDTO duncan = new PersonWithMedicalRecordDTO("Duncan", "841-874-6512", "1", 24, List.of(), List.of("shellfish"));
        AddressWithListOfPersonWithMedicalRecordDTO addr644 = new AddressWithListOfPersonWithMedicalRecordDTO("644 Gershwin Cir", List.of(duncan));

        // Personnes pour Station 1, Adresse 908 73rd St
        PersonWithMedicalRecordDTO walker = new PersonWithMedicalRecordDTO("Walker", "841-874-8547", "1", 45, List.of("thradox:700mg"), List.of("illisoxian"));
        PersonWithMedicalRecordDTO peters = new PersonWithMedicalRecordDTO("Peters", "841-874-7462", "1", 43, List.of(), List.of());
        AddressWithListOfPersonWithMedicalRecordDTO addr908 = new AddressWithListOfPersonWithMedicalRecordDTO("908 73rd St", List.of(walker, peters));

        // Personnes pour Station 1, Adresse 947 E. Rose Dr
        PersonWithMedicalRecordDTO stelzerB = new PersonWithMedicalRecordDTO("Stelzer", "841-874-7784", "1", 49, List.of("ibupurin:200mg", "hydrapermazol:400mg"), List.of("nillacilan"));
        PersonWithMedicalRecordDTO stelzerS = new PersonWithMedicalRecordDTO("Stelzer", "841-874-7784", "1", 44, List.of(), List.of());
        PersonWithMedicalRecordDTO stelzerK = new PersonWithMedicalRecordDTO("Stelzer", "841-874-7784", "1", 11, List.of("noxidian:100mg", "pharmacol:2500mg"), List.of());
        AddressWithListOfPersonWithMedicalRecordDTO addr947 = new AddressWithListOfPersonWithMedicalRecordDTO("947 E. Rose Dr", List.of(stelzerB, stelzerS, stelzerK));

        // Données complètes pour Station 1
        FireStationAddressWithListOfPersonWithMedicalRecordDTO station1Data = new FireStationAddressWithListOfPersonWithMedicalRecordDTO("1", List.of(addr644, addr908, addr947));

        // Personnes pour Station 2, Adresse 29 15th St
        PersonWithMedicalRecordDTO marrack = new PersonWithMedicalRecordDTO("Marrack", "841-874-6513", "2", 36, List.of(), List.of());
        AddressWithListOfPersonWithMedicalRecordDTO addr29 = new AddressWithListOfPersonWithMedicalRecordDTO("29 15th St", List.of(marrack));

        // Personnes pour Station 2, Adresse 892 Downing Ct
        PersonWithMedicalRecordDTO zemicksJ = new PersonWithMedicalRecordDTO("Zemicks", "841-874-7878", "2", 37, List.of("aznol:60mg", "hydrapermazol:900mg", "pharmacol:5000mg", "terazine:500mg"), List.of("peanut", "shellfish", "aznol"));
        PersonWithMedicalRecordDTO zemicksA = new PersonWithMedicalRecordDTO("Zemicks", "841-874-7512", "2", 40, List.of(), List.of());
        PersonWithMedicalRecordDTO zemicksR = new PersonWithMedicalRecordDTO("Zemicks", "841-874-7512", "2", 8, List.of(), List.of());
        AddressWithListOfPersonWithMedicalRecordDTO addr892 = new AddressWithListOfPersonWithMedicalRecordDTO("892 Downing Ct", List.of(zemicksJ, zemicksA, zemicksR));

        // Personnes pour Station 2, Adresse 951 LoneTree Rd
        PersonWithMedicalRecordDTO cadigan = new PersonWithMedicalRecordDTO("Cadigan", "841-874-7458", "2", 79, List.of("tradoxidine:400mg"), List.of());
        AddressWithListOfPersonWithMedicalRecordDTO addr951 = new AddressWithListOfPersonWithMedicalRecordDTO("951 LoneTree Rd", List.of(cadigan));

        // Données complètes pour Station 2
        FireStationAddressWithListOfPersonWithMedicalRecordDTO station2Data = new FireStationAddressWithListOfPersonWithMedicalRecordDTO("2", List.of(addr29, addr892, addr951));

        // 3. Construire le DTO racine
        ListOfAddressWithListOfPersonWithMedicalRecordDTO expectedFloodDTO = new ListOfAddressWithListOfPersonWithMedicalRecordDTO();
        expectedFloodDTO.setFireStationAddressPersonMedicalRecords(List.of(station1Data, station2Data));

        // 4. Configurer le mock du service
        ListOfAddressWithListOfPersonWithMedicalRecordDTO serviceResultDTO = new ListOfAddressWithListOfPersonWithMedicalRecordDTO();

        when(fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(eq(requestedStations)))
                .thenReturn(Optional.of(serviceResultDTO));

        // --- Act & Assert ---
        mockMvc.perform(get("/flood/stations")
                        .param("stations", "1", "2") // Paramètre de la requête
                        .accept(MediaType.APPLICATION_JSON))
                // Vérifier le statut et le type de contenu
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Vérifier la structure globale
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords", hasSize(2))) // Clé racine et taille
                // Vérifier Station 1
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].fireStation", is("1")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons", hasSize(3))) // 3 adresses pour station 1
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[0].address", is("644 Gershwin Cir")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[0].persons", hasSize(1)))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[0].persons[0].lastName", is("Duncan")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[2].address", is("947 E. Rose Dr"))) // Dernière adresse pour station 1
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[2].persons", hasSize(3)))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[0].persons[2].persons[2].age", is(11))) // Dernier Stelzer
                // Vérifier Station 2
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].fireStation", is("2")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons", hasSize(3))) // 3 adresses pour station 2
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons[1].address", is("892 Downing Ct")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons[1].persons", hasSize(3)))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons[1].persons[0].lastName", is("Zemicks")))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons[1].persons[0].medications", hasSize(4)))
                .andExpect(jsonPath("$.fireStationAddressPersonMedicalRecords[1].persons[2].persons[0].lastName", is("Cadigan"))); // Dernier élément global

        // --- Verify ---
        // Vérifier que la méthode du service a été appelée correctement
        verify(fireStationService).getListOfPersonsWithMedicalRecordsByListOfFireStation(eq(requestedStations));
    }

    // --- Tests pour POST /firestation ---
    @Test
    void addFireStation_whenValidInput_shouldReturnCreated() throws Exception {
        // Arrange: Configurer le mock du service
        when(fireStationService.addFireStation(any(FireStation.class))).thenReturn(station1);

        // Act & Assert: Exécuter la requête et vérifier la réponse
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isCreated()) // Vérifie le statut HTTP 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.address", is(station1.getAddress()))) // Vérifie le contenu JSON
                .andExpect(jsonPath("$.station", is(station1.getStation())));

        // Verify: S'assurer que la méthode du service a été appelée
        verify(fireStationService, times(1)).addFireStation(any(FireStation.class));
    }

    @Test
    void addFireStation_whenAddressExists_shouldReturnConflict() throws Exception {
        // Arrange: Simuler le cas où l'adresse existe déjà (le service lance une exception)
        when(fireStationService.addFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("Un mapping existe déjà pour l'adresse : " + station1.getAddress()));

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isConflict()); // Vérifie le statut HTTP 409

        // Verify
        verify(fireStationService, times(1)).addFireStation(any(FireStation.class));
    }

    @Test
    void addFireStation_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange: Simuler le cas où les données sont invalides (le service lance une autre exception)
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress(null);// Adresse manquante
        invalidStation.setStation("1"); // Numéro de station valide
        when(fireStationService.addFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("L'adresse et le numéro de station sont requis."));

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest()); // Vérifie le statut HTTP 400

        // Verify
        verify(fireStationService, times(1)).addFireStation(any(FireStation.class));
    }

    @Test
    void addFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange: Simuler une erreur interne inattendue
        when(fireStationService.addFireStation(any(FireStation.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station1)))
                .andExpect(status().isInternalServerError()); // Vérifie le statut HTTP 500

        // Verify
        verify(fireStationService, times(1)).addFireStation(any(FireStation.class));
    }


    // --- Tests pour PUT /firestation ---

    @Test
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
                .andExpect(jsonPath("$.station", is(stationToUpdate.getStation())));

        // Verify
        verify(fireStationService, times(1)).updateFireStation(any(FireStation.class));
    }

    @Test
    void updateFireStation_whenStationNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        when(fireStationService.updateFireStation(any(FireStation.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationToUpdate)))
                .andExpect(status().isNotFound()); // Vérifie le statut 404

        // Verify
        verify(fireStationService, times(1)).updateFireStation(any(FireStation.class));
    }

    @Test
    void updateFireStation_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange
        FireStation invalidStation = new FireStation();
        invalidStation.setAddress("Some Address");
        invalidStation.setStation(null);// Station manquante
        when(fireStationService.updateFireStation(any(FireStation.class)))
                .thenThrow(new IllegalArgumentException("L'adresse et le numéro de station sont requis pour la mise à jour."));


        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest());

        // Verify
        verify(fireStationService, times(1)).updateFireStation(any(FireStation.class));
    }

    @Test
    void updateFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(fireStationService.updateFireStation(any(FireStation.class))).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(put("/firestation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationToUpdate)))
                .andExpect(status().isInternalServerError());

        // Verify
        verify(fireStationService, times(1)).updateFireStation(any(FireStation.class));
    }

    // --- Tests pour DELETE /firestation ---

    @Test
    void deleteFireStation_whenStationExists_shouldReturnNoContent() throws Exception {
        // Arrange
        String addressToDelete = "1509 Culver St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete)) // Paramètre de requête
                .andExpect(status().isNoContent()); // Vérifie le statut 204

        // Verify
        verify(fireStationService, times(1)).deleteFireStationMapping(eq(addressToDelete));
    }

    @Test
    void deleteFireStation_whenStationNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        String addressToDelete = "NonExistent St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete))
                .andExpect(status().isNotFound()); // Vérifie le statut 404

        // Verify
        verify(fireStationService, times(1)).deleteFireStationMapping(eq(addressToDelete));
    }

    @Test
    void deleteFireStation_whenMissingAddressParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert: Appel sans le paramètre 'address'
        mockMvc.perform(delete("/firestation"))
                .andExpect(status().isBadRequest()); // Spring devrait rejeter car param requis

        // Verify: Le service ne doit pas être appelé
        verify(fireStationService, never()).deleteFireStationMapping(anyString());
    }

    @Test
    void deleteFireStation_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        String addressToDelete = "1509 Culver St";
        when(fireStationService.deleteFireStationMapping(eq(addressToDelete))).thenThrow(new RuntimeException("DB connection lost"));

        // Act & Assert
        mockMvc.perform(delete("/firestation")
                        .param("address", addressToDelete))
                .andExpect(status().isInternalServerError());

        // Verify
        verify(fireStationService, times(1)).deleteFireStationMapping(eq(addressToDelete));
    }


    // --- Tests pour GET /firestation/all ---

    @Test
    void getAllFireStations_shouldReturnOkWithListOfStations() throws Exception {
        // Arrange
        List<FireStation> stationList = Arrays.asList(station1, station2);
        when(fireStationService.getAllFireStations()).thenReturn(stationList);

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2))) // Vérifie la taille de la liste JSON
                .andExpect(jsonPath("$[0].address", is(station1.getAddress())))
                .andExpect(jsonPath("$[1].address", is(station2.getAddress())));

        // Verify
        verify(fireStationService, times(1)).getAllFireStations();
    }

    @Test
    void getAllFireStations_whenNoStations_shouldReturnOkWithEmptyList() throws Exception {
        // Arrange
        when(fireStationService.getAllFireStations()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // Vérifie que la liste JSON est vide

        // Verify
        verify(fireStationService, times(1)).getAllFireStations();
    }

    @Test
    void getAllFireStations_whenServiceThrowsError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(fireStationService.getAllFireStations()).thenThrow(new RuntimeException("Failed to retrieve data"));

        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        // Verify
        verify(fireStationService, times(1)).getAllFireStations();
    }


    // --- Tests pour GET /firestation/all ---

    @Test
    void getAllFireStation() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/firestation/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Verify
        verify(fireStationService, times(1)).getAllFireStations();
    }
}