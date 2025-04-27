package com.openclassroom.safetynet.controller; // Adaptez le package

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.dto.*; // Importer tous les DTOs
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections; // Import manquant
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // Ajout import
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*; // Import pour Mockito
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is; // Import pour Hamcrest
import static org.hamcrest.Matchers.hasSize; // Import pour Hamcrest

/**
 * Classe de test unitaire pour {@link PersonController}.
 * <p>
 * Utilise {@link WebMvcTest} pour tester la couche contrôleur en isolation,
 * en simulant les requêtes HTTP avec {@link MockMvc} et en mockant la dépendance
 * {@link PersonService} avec {@link Mock}.
 * </p>
 */
@WebMvcTest(PersonController.class) // Cible le contrôleur à tester
@ExtendWith(SpringExtension.class)
public class PersonControllerTest {

    /**
     * Utilitaire Spring pour simuler les appels HTTP vers le contrôleur.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock de la dépendance PersonService, injecté par Spring Test dans le contexte.
     * Permet de définir le comportement du service pour chaque scénario de test.
     */
    @MockitoBean
    private PersonService personService;

    /**
     * Utilitaire Jackson (fourni par Spring) pour convertir les objets Java en JSON
     * pour les corps de requête et inversement.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Objets de test réutilisables
    private Person person1;
    private Person personToUpdate;

    /**
     * Méthode d'initialisation exécutée avant chaque test.
     * Prépare des objets Person de test.
     */
    @BeforeEach
    void setUp() {
        // Initialisation propre des objets Person
        person1 = new Person();
        person1.setFirstName("John");
        person1.setLastName("Doe");
        person1.setAddress("1 Main St");
        person1.setCity("City");
        person1.setZip("12345");
        person1.setPhone("555-111");
        person1.setEmail("j.doe@mail.com");

        personToUpdate = new Person();
        personToUpdate.setFirstName("John"); // Même identifiant
        personToUpdate.setLastName("Doe");
        personToUpdate.setAddress("2 New St"); // Champs mis à jour
        personToUpdate.setCity("NewCity");
        personToUpdate.setZip("67890");
        personToUpdate.setPhone("555-222");
        personToUpdate.setEmail("john.doe@mail.com");
    }

    // --- Tests pour GET /person ---

    /**
     * Teste GET /person lorsque des personnes existent.
     * Doit retourner le statut OK (200) et la liste des personnes en JSON.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /person - Doit retourner OK et la liste si des personnes existent")
    void getPersonsWhenPersonsExistShouldReturnOk() throws Exception {
        // Arrange: Simuler le service retournant une liste non vide
        when(personService.getAllPersons()).thenReturn(List.of(person1));

        // Act & Assert: Effectuer l'appel et vérifier les attentes
        mockMvc.perform(get("/person")
                        .accept(MediaType.APPLICATION_JSON)) // Préciser qu'on attend du JSON
                .andExpect(status().isOk()) // Statut 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // Type de contenu JSON
                .andExpect(jsonPath("$", hasSize(1))) // Vérifier la taille de la liste
                .andExpect(jsonPath("$[0].firstName", is("John"))); // Vérifier un champ du premier élément

        // Verify: S'assurer que le service a été appelé
        verify(personService).getAllPersons();
    }

    /**
     * Teste GET /person lorsque aucune personne n'existe.
     * Doit retourner le statut No Content (204).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /person - Doit retourner No Content si aucune personne n'existe")
    void getPersonsWhenNoPersonsShouldReturnNoContent() throws Exception {
        // Arrange: Simuler le service retournant une liste vide
        when(personService.getAllPersons()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/person")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // Statut 204

        // Verify
        verify(personService).getAllPersons();
    }

    // --- Tests pour GET /childAlert?address=... ---

    /**
     * Teste GET /childAlert avec une adresse valide et des enfants trouvés.
     * Doit retourner OK (200) et le DTO correspondant.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /childAlert - Doit retourner OK si adresse valide et enfants trouvés")
    void getChildrenAndFamilyByAddressWhenAddressValidShouldReturnOk() throws Exception {
        // Arrange: Simuler le service retournant un DTO
        String addressParam = "1 Main St";
        // Créer un DTO exemple (peut être plus détaillé si nécessaire pour vérifier les champs)
        ChildWithFamilyDTO dto = new ChildWithFamilyDTO(Collections.emptyList(), Collections.emptyList());
        when(personService.getChildAndFamilyByAddress(eq(addressParam))).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/childAlert")
                        .param("address", addressParam)) // Paramètre URL
                .andExpect(status().isOk()); // Statut 200
        // Ajouter .andExpect(jsonPath(...)) si on veut vérifier le contenu du DTO

        // Verify
        verify(personService).getChildAndFamilyByAddress(eq(addressParam));
    }

    /**
     * Teste GET /childAlert avec une adresse vide.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /childAlert - Doit retourner Bad Request si adresse vide")
    void getChildrenAndFamilyByAddressWhenAddressEmptyShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/childAlert")
                        .param("address", "")) // Adresse vide
                .andExpect(status().isBadRequest()); // Statut 400

        // Verify: Le service ne doit pas être appelé
        verify(personService, never()).getChildAndFamilyByAddress(anyString());
    }

    /**
     * Teste GET /childAlert lorsque le service ne trouve pas d'enfants pour l'adresse.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /childAlert - Doit retourner Not Found si aucun enfant trouvé")
    void getChildrenAndFamilyByAddressWhenNotFoundShouldReturnNotFound() throws Exception {
        // Arrange: Simuler le service retournant Optional vide
        String addressParam = "Unknown Address";
        when(personService.getChildAndFamilyByAddress(eq(addressParam))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/childAlert")
                        .param("address", addressParam))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).getChildAndFamilyByAddress(eq(addressParam));
    }

    // --- Tests pour GET /fire?address=... ---

    /**
     * Teste GET /fire avec une adresse valide et des données trouvées.
     * Doit retourner OK (200).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /fire - Doit retourner OK si adresse valide et données trouvées")
    void getPersonWithMedicalReportByAddressWhenAddressValidShouldReturnOk() throws Exception {
        // Arrange
        String addressParam = "1 Main St";
        FirePersonDTO dto = new FirePersonDTO(Collections.emptyList()); // DTO exemple
        when(personService.getPersonFireStationAndMedicalReportByAddress(eq(addressParam))).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/fire")
                        .param("address", addressParam))
                .andExpect(status().isOk()); // Statut 200

        // Verify
        verify(personService).getPersonFireStationAndMedicalReportByAddress(eq(addressParam));
    }

    /**
     * Teste GET /fire avec une adresse vide.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /fire - Doit retourner Bad Request si adresse vide")
    void getPersonWithMedicalReportByAddressWhenAddressEmptyShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/fire")
                        .param("address", ""))
                .andExpect(status().isBadRequest()); // Statut 400

        // Verify
        verify(personService, never()).getPersonFireStationAndMedicalReportByAddress(anyString());
    }

    /**
     * Teste GET /fire lorsque le service ne trouve pas de données pour l'adresse.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /fire - Doit retourner Not Found si données non trouvées")
    void getPersonWithMedicalReportByAddressWhenNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        String addressParam = "Unknown Address";
        when(personService.getPersonFireStationAndMedicalReportByAddress(eq(addressParam))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/fire")
                        .param("address", addressParam))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).getPersonFireStationAndMedicalReportByAddress(eq(addressParam));
    }


    // --- Tests pour GET /personInfo?lastName=... --- (Utilisation de l'URL corrigée)

    /**
     * Teste GET /personInfo avec un nom valide et des données trouvées.
     * Doit retourner OK (200).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /personInfo - Doit retourner OK si nom valide et données trouvées")
    void getPersonInfoByLastNameWhenLastNameValidShouldReturnOk() throws Exception {
        // Arrange
        String lastNameParam = "Doe";
        ListOfPersonInfolastNameDTO dto = new ListOfPersonInfolastNameDTO(lastNameParam, Collections.emptyList()); // DTO exemple
        when(personService.getPersonInfoByLastName(eq(lastNameParam))).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/personInfo") // Utilisation de l'URL corrigée
                        .param("lastName", lastNameParam)) // Utilisation du paramètre corrigé
                .andExpect(status().isOk()); // Statut 200

        // Verify
        verify(personService).getPersonInfoByLastName(eq(lastNameParam));
    }

    /**
     * Teste GET /personInfolastName avec un nom vide.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /personInfolastName - Doit retourner Bad Request si nom vide")
    void getPersonInfoByLastNameWhenLastNameEmptyShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/personInfolastName")
                        .param("lastName", ""))
                .andExpect(status().isBadRequest());

        // Verify
        verify(personService, never()).getPersonInfoByLastName(anyString());
    }


    /**
     * Teste GET /personInfolastName lorsque le service ne trouve pas de données pour le nom.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /personInfolastName - Doit retourner Not Found si données non trouvées")
    void getPersonInfoByLastNameWhenNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        String lastNameParam = "Unknown";
        when(personService.getPersonInfoByLastName(eq(lastNameParam))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/personInfolastName") // URL corrigée
                        .param("lastName", lastNameParam)) // Paramètre corrigé
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).getPersonInfoByLastName(eq(lastNameParam));
    }

    // --- Tests pour GET /communityEmail?city=... ---

    /**
     * Teste GET /communityEmail avec une ville valide et des données trouvées.
     * Doit retourner OK (200).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /communityEmail - Doit retourner OK si ville valide et données trouvées")
    void getCommunityEmailWhenCityValidShouldReturnOk() throws Exception {
        // Arrange
        String cityParam = "City";
        CommunityEmailDTO dto = new CommunityEmailDTO(cityParam, Collections.emptyList()); // DTO exemple
        when(personService.getCommunityEmailByCity(eq(cityParam))).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/communityEmail")
                        .param("city", cityParam))
                .andExpect(status().isOk()); // Statut 200

        // Verify
        verify(personService).getCommunityEmailByCity(eq(cityParam));
    }

    /**
     * Teste GET /communityEmail avec une ville vide.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /communityEmail - Doit retourner Bad Request si ville vide")
    void getCommunityEmailWhenCityEmptyShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/communityEmail")
                        .param("city", ""))
                .andExpect(status().isBadRequest());

        // Verify
        verify(personService, never()).getCommunityEmailByCity(anyString());
    }


    /**
     * Teste GET /communityEmail lorsque le service ne trouve pas de données pour la ville.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("GET /communityEmail - Doit retourner Not Found si données non trouvées")
    void getCommunityEmailWhenNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        String cityParam = "Unknown City";
        when(personService.getCommunityEmailByCity(eq(cityParam))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/communityEmail")
                        .param("city", cityParam))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).getCommunityEmailByCity(eq(cityParam));
    }


    // --- Tests pour POST /person ---

    /**
     * Teste POST /person avec des données valides.
     * Doit retourner Created (201) et la personne créée.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("POST /person - Doit retourner Created si données valides")
    void addPersonWhenValidInputShouldReturnCreated() throws Exception {
        // Arrange
        when(personService.addPerson(any(Person.class))).thenReturn(person1);

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person1)))
                .andExpect(status().isCreated()) // 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));

        // Verify
        verify(personService).addPerson(any(Person.class));
    }

    /**
     * Teste POST /person lorsqu'une personne avec le même nom/prénom existe déjà.
     * Doit retourner Conflict (409).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("POST /person - Doit retourner Conflict si personne existe déjà")
    void addPersonWhenPersonExistsShouldReturnConflict() throws Exception {
        // Arrange: Simuler l'exception lancée par le service
        when(personService.addPerson(any(Person.class)))
                .thenThrow(new IllegalArgumentException("existe déjà")); // Le message doit contenir "existe déjà"

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person1)))
                .andExpect(status().isConflict()); // 409

        // Verify
        verify(personService).addPerson(any(Person.class));
    }

    /**
     * Teste POST /person avec un prénom manquant dans le corps de la requête.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("POST /person - Doit retourner Bad Request si prénom manquant")
    void addPersonWhenMissingFirstNameShouldReturnBadRequest() throws Exception {
        // Arrange: Créer un objet invalide
        Person invalidPerson = new Person();
        // invalidPerson.setFirstName(null); // Déjà null par défaut
        invalidPerson.setLastName("Doe");
        // ... (pas besoin de setter les autres champs pour ce test)

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest()); // 400

        // Verify: Le service ne doit pas être appelé
        verify(personService, never()).addPerson(any(Person.class));
    }

    /**
     * Teste POST /person avec un nom vide dans le corps de la requête.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("POST /person - Doit retourner Bad Request si nom vide")
    void addPersonWhenBlankLastNameShouldReturnBadRequest() throws Exception {
        // Arrange
        Person invalidPerson = new Person();
        invalidPerson.setFirstName("John");
        invalidPerson.setLastName(" "); // Nom vide

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(personService, never()).addPerson(any(Person.class));
    }


    // --- Tests pour PUT /person ---

    /**
     * Teste PUT /person pour une personne existante avec des données valides.
     * Doit retourner OK (200) et la personne mise à jour.
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("PUT /person - Doit retourner OK si personne existe et données valides")
    void updatePersonWhenPersonExistsShouldReturnOk() throws Exception {
        // Arrange
        when(personService.updatePerson(any(Person.class))).thenReturn(Optional.of(personToUpdate));

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personToUpdate)))
                .andExpect(status().isOk()) // 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.address", is("2 New St"))) // Vérifier un champ mis à jour
                .andExpect(jsonPath("$.email", is("john.doe@mail.com")));

        // Verify
        verify(personService).updatePerson(any(Person.class));
    }

    /**
     * Teste PUT /person pour une personne qui n'existe pas.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("PUT /person - Doit retourner Not Found si personne n'existe pas")
    void updatePersonWhenPersonNotFoundShouldReturnNotFound() throws Exception {
        // Arrange: Simuler le service retournant Optional vide
        when(personService.updatePerson(any(Person.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personToUpdate)))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(personService).updatePerson(any(Person.class));
    }

    /**
     * Teste PUT /person avec un nom manquant dans le corps de la requête.
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("PUT /person - Doit retourner Bad Request si nom manquant")
    void updatePersonWhenInvalidInputShouldReturnBadRequest() throws Exception {
        // Arrange: Objet invalide
        Person invalidPerson = new Person();
        invalidPerson.setFirstName("John");
        // invalidPerson.setLastName(null); // Déjà null

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest()); // 400

        // Verify: Le service ne doit pas être appelé
        verify(personService, never()).updatePerson(any(Person.class));
    }

    // --- Tests pour DELETE /person ---

    /**
     * Teste DELETE /person pour une personne existante.
     * Doit retourner No Content (204).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("DELETE /person - Doit retourner No Content si personne existe")
    void deletePersonWhenPersonExistsShouldReturnNoContent() throws Exception {
        // Arrange
        when(personService.deletePerson(eq("John"), eq("Doe"))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isNoContent()); // 204

        // Verify
        verify(personService).deletePerson(eq("John"), eq("Doe"));
    }

    /**
     * Teste DELETE /person pour une personne qui n'existe pas.
     * Doit retourner Not Found (404).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("DELETE /person - Doit retourner Not Found si personne n'existe pas")
    void deletePersonWhenPersonNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        when(personService.deletePerson(eq("Jane"), eq("Smith"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", "Jane")
                        .param("lastName", "Smith"))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(personService).deletePerson(eq("Jane"), eq("Smith"));
    }

    /**
     * Teste DELETE /person avec un paramètre manquant (lastName).
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("DELETE /person - Doit retourner Bad Request si paramètre manquant")
    void deletePersonWhenMissingParamsShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", "John")) // lastName manquant
                .andExpect(status().isBadRequest()); // 400

        // Verify: Le service ne doit pas être appelé
        verify(personService, never()).deletePerson(anyString(), anyString());
    }

    /**
     * Teste DELETE /person avec un paramètre vide (firstName).
     * Doit retourner Bad Request (400).
     * @throws Exception si une erreur survient lors de l'appel MockMvc.
     */
    @Test
    @DisplayName("DELETE /person - Doit retourner Bad Request si paramètre vide")
    void deletePersonWhenBlankParamsShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", " ")
                        .param("lastName", "Doe"))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(personService, never()).deletePerson(anyString(), anyString());
    }
}