package com.openclassroom.safetynet.controller; // Adaptez le package

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.dto.ChildWithFamilyDTO;
import com.openclassroom.safetynet.dto.CommunityEmailDTO;
import com.openclassroom.safetynet.dto.FirePersonDTO;
import com.openclassroom.safetynet.dto.ListOfPersonInfolastNameDTO;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.PersonService;
import org.junit.jupiter.api.BeforeEach; // Ou juste utiliser @Test
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any; // Pour les arguments de mock
import static org.mockito.ArgumentMatchers.eq;  // Pour les arguments spécifiques
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never; // Si une méthode ne doit pas être appelée
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; // get, post, put, delete
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // status, content, jsonPath

@WebMvcTest(PersonController.class) // Teste PersonController en isolation
@ExtendWith(SpringExtension.class)
public class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc; // Pour simuler les requêtes HTTP

    @MockitoBean // Crée un mock du service et l'injecte dans le contexte du test
    private PersonService personService;

    @Autowired
    private ObjectMapper objectMapper; // Pour convertir les objets en JSON
    private Person person1;
    private Person personToUpdate;

    @BeforeEach
    void setUp() {
        person1 = new Person();
        person1.setFirstName("John");
        person1.setLastName("Doe");
        person1.setAddress("1 Main St");
        person1.setCity("City");
        person1.setZip("12345");
        person1.setPhone("555-111");
        person1.setEmail("j.doe@mail.com");
        personToUpdate = new Person();
        personToUpdate.setFirstName("John");
        personToUpdate.setLastName("Doe");
        personToUpdate.setAddress("2 New St");
        personToUpdate.setCity("NewCity");
        personToUpdate.setZip("67890");
        personToUpdate.setPhone("555-222");
        personToUpdate.setEmail("john.doe@mail.com");
    }

    // --- Tests pour GET /person ---
    @Test
    void getPersonsWhenPersonsExistShouldReturnOk() throws Exception {
        when(personService.getAllPersons()).thenReturn(List.of(person1));

        mockMvc.perform(get("/person"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        verify(personService).getAllPersons();
    }

    @Test
    void getPersonsWhenNoPersonsShouldReturnNoContent() throws Exception {
        when(personService.getAllPersons()).thenReturn(List.of());

        mockMvc.perform(get("/person"))
                .andExpect(status().isNoContent());

        verify(personService).getAllPersons();
    }

    // --- Tests pour GET /childAlert ---
    @Test
    void getChildrenAndFamilyByAddressWhenAddressValidShouldReturnOk() throws Exception {
        when(personService.getChildAndFamilyByAddress(eq("1 MAIN ST")))
                .thenReturn(Optional.of(new ChildWithFamilyDTO()));

        mockMvc.perform(get("/childAlert")
                        .param("address", "1 Main St"))
                .andExpect(status().isOk());

        verify(personService).getChildAndFamilyByAddress("1 MAIN ST");
    }

    @Test
    void getChildrenAndFamilyByAddressWhenAddressEmptyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/childAlert")
                        .param("address", ""))
                .andExpect(status().isBadRequest());

        verify(personService, never()).getChildAndFamilyByAddress(any());
    }

    @Test
    void getChildrenAndFamilyByAddressWhenNotFoundShouldReturnNotFound() throws Exception {
        when(personService.getChildAndFamilyByAddress(eq("UNKNOWN ADDRESS")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/childAlert")
                        .param("address", "Unknown Address"))
                .andExpect(status().isNotFound());

        verify(personService).getChildAndFamilyByAddress("UNKNOWN ADDRESS");
    }

    // --- Tests pour GET /fire ---
    @Test
    void getFireWhenAddressValidShouldReturnOk() throws Exception {
        when(personService.getPersonFireStationAndMedicalReportByAddress(eq("1 MAIN ST")))
                .thenReturn(Optional.of(new FirePersonDTO()));

        mockMvc.perform(get("/fire")
                        .param("address", "1 Main St"))
                .andExpect(status().isOk());

        verify(personService).getPersonFireStationAndMedicalReportByAddress("1 MAIN ST");
    }

    @Test
    void getFireWhenAddressEmptyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/fire")
                        .param("address", ""))
                .andExpect(status().isBadRequest());

        verify(personService, never()).getPersonFireStationAndMedicalReportByAddress(any());
    }

    @Test
    void getFireWhenNotFoundShouldReturnNotFound() throws Exception {
        when(personService.getPersonFireStationAndMedicalReportByAddress(eq("UNKNOWN ADDRESS")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fire")
                        .param("address", "Unknown Address"))
                .andExpect(status().isNotFound());

        verify(personService).getPersonFireStationAndMedicalReportByAddress("UNKNOWN ADDRESS");
    }

    // --- Tests pour GET /personInfolastName ---
    @Test
    void getPersonInfolastNameWhenLastNameValidShouldReturnOk() throws Exception {
        when(personService.getPersonInfoByLastName(eq("DOE")))
                .thenReturn(Optional.of(new ListOfPersonInfolastNameDTO()));

        mockMvc.perform(get("/personInfolastName")
                        .param("lastname", "Doe"))
                .andExpect(status().isOk());

        verify(personService).getPersonInfoByLastName("DOE");
    }

    @Test
    void getPersonInfolastNameWhenNotFoundShouldReturnNotFound() throws Exception {
        when(personService.getPersonInfoByLastName(eq("UNKNOWN")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/personInfolastName")
                        .param("lastname", "Unknown"))
                .andExpect(status().isNotFound());

        verify(personService).getPersonInfoByLastName("UNKNOWN");
    }

    // --- Tests pour GET /communityEmail ---
    @Test
    void getCommunityEmailWhenCityValidShouldReturnOk() throws Exception {
        when(personService.getCommunityEmailByCity(eq("CITY")))
                .thenReturn(Optional.of(new CommunityEmailDTO()));

        mockMvc.perform(get("/communityEmail")
                        .param("city", "City"))
                .andExpect(status().isOk());

        verify(personService).getCommunityEmailByCity("CITY");
    }

    @Test
    void getCommunityEmailWhenNotFoundShouldReturnNotFound() throws Exception {
        when(personService.getCommunityEmailByCity(eq("UNKNOWN CITY")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/communityEmail")
                        .param("city", "Unknown City"))
                .andExpect(status().isNotFound());

        verify(personService).getCommunityEmailByCity("UNKNOWN CITY");
    }


    // --- Tests pour POST /person ---
    @Test
    void addPersonWhenValidInputShouldReturnCreated() throws Exception {
        // Arrange: Configure le mock pour retourner la personne quand addPerson est appelé
        when(personService.addPerson(any(Person.class))).thenReturn(person1);

        // Act & Assert: Simule la requête POST et vérifie la réponse
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person1))) // Corps de la requête en JSON
                .andExpect(status().isCreated()) // Statut 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("John")) // Vérifie les champs JSON
                .andExpect(jsonPath("$.lastName").value("Doe"));

        // Verify: Vérifie que la méthode du service a bien été appelée
        verify(personService).addPerson(any(Person.class));
    }

    @Test
    void addPersonWhenPersonExistsShouldReturnConflict() throws Exception {
        // Arrange: Configure le mock pour lancer une exception (simulant un conflit)
        when(personService.addPerson(any(Person.class)))
                .thenThrow(new IllegalArgumentException("Person with name John Doe already exists."));

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person1)))
                .andExpect(status().isConflict()); // Statut 409

        // Verify
        verify(personService).addPerson(any(Person.class));
    }

    @Test
    void addPersonWhenMissingFirstNameShouldReturnBadRequest() throws Exception {
        // Arrange
        Person invalidPerson = new Person();
        invalidPerson.setFirstName(null); // Simule un prénom manquant
        invalidPerson.setLastName("Doe");
        invalidPerson.setAddress("Addr");
        invalidPerson.setCity("City");
        invalidPerson.setZip("Zip");
        invalidPerson.setPhone("Phone");
        invalidPerson.setEmail("Email");

        // Act & Assert
        mockMvc.perform(post("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest()); // Statut 400

        // Verify: Le service ne doit pas être appelé si la validation échoue dans le contrôleur
        verify(personService, never()).addPerson(any(Person.class));
    }


    // --- Tests pour PUT /person ---
    @Test
    void updatePersonWhenPersonExistsShouldReturnOk() throws Exception {
        // Arrange
        when(personService.updatePerson(any(Person.class))).thenReturn(Optional.of(personToUpdate));

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personToUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.address").value("2 New St"))
                .andExpect(jsonPath("$.email").value("john.doe@mail.com"));

        // Verify
        verify(personService).updatePerson(any(Person.class));
    }

    @Test
    void updatePersonWhenPersonNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        when(personService.updatePerson(any(Person.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personToUpdate)))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).updatePerson(any(Person.class));
    }

    @Test
    void updatePersonWhenInvalidInputShouldReturnBadRequest() throws Exception {
        // Arrange
        Person invalidPerson = new Person();
        invalidPerson.setFirstName("John");
        invalidPerson.setLastName(null);
        invalidPerson.setAddress("Addr");
        invalidPerson.setCity("City");
        invalidPerson.setZip("Zip");
        invalidPerson.setPhone("Phone");
        invalidPerson.setEmail("Email");

        // Act & Assert
        mockMvc.perform(put("/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest());

        // Verify
        verify(personService, never()).updatePerson(any(Person.class));
    }

    // --- Tests pour DELETE /person ---
    @Test
    void deletePersonWhenPersonExistsShouldReturnNoContent() throws Exception {
        // Arrange
        when(personService.deletePerson(eq("John"), eq("Doe"))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isNoContent()); // Statut 204

        // Verify
        verify(personService).deletePerson(eq("John"), eq("Doe"));
    }

    @Test
    void deletePersonWhenPersonNotFoundShouldReturnNotFound() throws Exception {
        // Arrange
        when(personService.deletePerson(eq("Jane"), eq("Smith"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/person")
                        .param("firstName", "Jane")
                        .param("lastName", "Smith"))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(personService).deletePerson(eq("Jane"), eq("Smith"));
    }

    @Test
    void deletePersonWhenMissingParamsShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/person") // Manque les paramètres
                        .param("firstName", "John")) // lastName est manquant
                .andExpect(status().isBadRequest());

        // Verify
        verify(personService, never()).deletePerson(any(), any());
    }
}