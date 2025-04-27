package com.openclassroom.safetynet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.MedicalRecordService;
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
import java.util.Optional;

// Static imports pour la lisibilité
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize; // Pour vérifier la taille des listes JSON

@WebMvcTest(controllers = {MedicalRecordController.class}) // Cible le contrôleur spécifique
@ExtendWith(SpringExtension.class)
public class MedicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc; // Pour simuler les requêtes HTTP

    @MockitoBean // Mock le service injecté
    private MedicalRecordService medicalRecordService;

    @Autowired
    private ObjectMapper objectMapper; // Pour le JSON

    // Données de test
    private MedicalRecord record1;
    private MedicalRecord recordToUpdate;
    private MedicalRecord invalidRecord;

    @BeforeEach
    void setUp() {
        record1 = new MedicalRecord();
        record1.setFirstName("John");
        record1.setLastName("Boyd");
        record1.setBirthdate("03/06/1984");
        record1.setMedications(Arrays.asList("aznol:350mg", "hydrapermazol:100mg"));
        record1.setAllergies(Collections.singletonList("nillacilan"));
        recordToUpdate = new MedicalRecord();
        recordToUpdate.setFirstName("John");
        recordToUpdate.setLastName("Boyd");
        recordToUpdate.setBirthdate("03/06/1985");
        recordToUpdate.setMedications(Arrays.asList("aznol:350mg", "hydrapermazol:100mg"));
        recordToUpdate.setAllergies(Collections.emptyList());
        invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName(null);
        invalidRecord.setLastName("Boyd");
        invalidRecord.setBirthdate("01/01/1990");
        invalidRecord.setMedications(Collections.emptyList());
        invalidRecord.setAllergies(Collections.emptyList());
    }

    // --- Tests pour POST /medicalRecord ---

    @Test
    void addMedicalRecord_whenValidInput_shouldReturnCreated() throws Exception {
        // Arrange: Configurer le mock pour retourner l'enregistrement créé
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class))).thenReturn(record1);

        // Act & Assert: Exécuter la requête et vérifier la réponse
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isCreated()) // Statut 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Boyd")))
                .andExpect(jsonPath("$.medications", hasSize(2)));

        // Verify: S'assurer que la méthode du service a été appelée
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void addMedicalRecord_whenRecordExists_shouldReturnConflict() throws Exception {
        // Arrange: Simuler le cas où l'enregistrement existe déjà (le service lance une exception)
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("Un dossier médical existe déjà pour John Boyd"));

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isConflict()); // Statut 409

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void addMedicalRecord_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange: Utiliser l'enregistrement invalide (prénom null)
        // Le contrôleur devrait attraper l'IllegalArgumentException AVANT le message "existe déjà"
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("Le prénom et le nom sont requis"));

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRecord)))
                .andExpect(status().isBadRequest()); // Statut 400

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void addMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange: Simuler une erreur interne
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isInternalServerError()); // Statut 500

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    // --- Tests pour PUT /medicalRecord ---

    @Test
    void updateMedicalRecord_whenRecordExists_shouldReturnOk() throws Exception {
        // Arrange: Configurer le mock pour retourner l'enregistrement mis à jour
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class))).thenReturn(Optional.of(recordToUpdate));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate))) // Envoyer les nouvelles données
                .andExpect(status().isOk()) // Statut 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("John"))) // Nom/Prénom inchangés
                .andExpect(jsonPath("$.lastName", is("Boyd")))
                .andExpect(jsonPath("$.birthdate", is("03/06/1985"))) // Champ mis à jour
                .andExpect(jsonPath("$.medications", hasSize(2))) // Champ mis à jour
                .andExpect(jsonPath("$.allergies", hasSize(0))); // Champ mis à jour

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void updateMedicalRecord_whenRecordNotFound_shouldReturnNotFound() throws Exception {
        // Arrange: Simuler le cas où l'enregistrement n'est pas trouvé
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate)))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void updateMedicalRecord_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange: Utiliser l'enregistrement invalide
        // Simuler l'exception lancée par le service pour données invalides
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("Le prénom et le nom sont requis"));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRecord)))
                .andExpect(status().isBadRequest()); // Statut 400

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    @Test
    void updateMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange: Simuler une erreur interne
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new RuntimeException("Unexpected error during update"));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate)))
                .andExpect(status().isInternalServerError()); // Statut 500

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    // --- Tests pour DELETE /medicalRecord ---

    @Test
    void deleteMedicalRecord_whenRecordExists_shouldReturnNoContent() throws Exception {
        // Arrange: Configurer le mock pour simuler une suppression réussie
        when(medicalRecordService.deleteMedicalRecord(eq("John"), eq("Boyd"))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John")
                        .param("lastName", "Boyd"))
                .andExpect(status().isNoContent()); // Statut 204

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("John"), eq("Boyd"));
    }

    @Test
    void deleteMedicalRecord_whenRecordNotFound_shouldReturnNotFound() throws Exception {
        // Arrange: Configurer le mock pour simuler un enregistrement non trouvé
        when(medicalRecordService.deleteMedicalRecord(eq("Jane"), eq("Doe"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "Jane")
                        .param("lastName", "Doe"))
                .andExpect(status().isNotFound()); // Statut 404

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("Jane"), eq("Doe"));
    }

    @Test
    void deleteMedicalRecord_whenMissingFirstNameParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert: Paramètre 'firstName' manquant
        mockMvc.perform(delete("/medicalRecord")
                        .param("lastName", "Boyd"))
                .andExpect(status().isBadRequest()); // Spring rejette car param requis

        // Verify: Le service ne doit pas être appelé
        verify(medicalRecordService, never()).deleteMedicalRecord(anyString(), anyString());
    }

    @Test
    void deleteMedicalRecord_whenMissingLastNameParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert: Paramètre 'lastName' manquant
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John"))
                .andExpect(status().isBadRequest());

        // Verify
        verify(medicalRecordService, never()).deleteMedicalRecord(anyString(), anyString());
    }


    @Test
    void deleteMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange: Simuler une erreur interne
        when(medicalRecordService.deleteMedicalRecord(eq("John"), eq("Boyd")))
                .thenThrow(new RuntimeException("Failed to delete"));

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John")
                        .param("lastName", "Boyd"))
                .andExpect(status().isInternalServerError()); // Statut 500

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("John"), eq("Boyd"));
    }
}