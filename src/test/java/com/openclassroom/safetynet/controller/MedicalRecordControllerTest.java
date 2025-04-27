package com.openclassroom.safetynet.controller; // Adaptez le package

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // Ajout import pour DisplayName
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Import corrigé
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension; // Optionnel
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List; // Ajout import manquant
import java.util.Optional;

// Static imports pour la lisibilité
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // Ajout import
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

/**
 * Classe de test unitaire pour {@link MedicalRecordController}.
 * <p>
 * Utilise {@link WebMvcTest} pour tester la couche contrôleur en isolation,
 * en simulant les requêtes HTTP avec {@link MockMvc} et en mockant la dépendance
 * {@link MedicalRecordService} avec {@link MockitoBean}.
 * </p>
 */
@WebMvcTest(MedicalRecordController.class) // Cible le contrôleur spécifique
@ExtendWith(SpringExtension.class)
public class MedicalRecordControllerTest {

    /**
     * Utilitaire Spring pour simuler les appels HTTP vers le contrôleur.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock de la dépendance MedicalRecordService, injecté par Spring Test.
     */
    @MockitoBean
    private MedicalRecordService medicalRecordService;

    /**
     * Utilitaire Jackson pour la conversion JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Données de test
    private MedicalRecord record1;
    private MedicalRecord recordToUpdate;
    private MedicalRecord invalidRecord;

    /**
     * Méthode d'initialisation exécutée avant chaque test (@Test).
     * Prépare des objets MedicalRecord de test réutilisables.
     */
    @BeforeEach
    void setUp() {
        record1 = new MedicalRecord();
        record1.setFirstName("John");
        record1.setLastName("Boyd");
        record1.setBirthdate("03/06/1984");
        record1.setMedications(Arrays.asList("aznol:350mg", "hydrapermazol:100mg"));
        record1.setAllergies(Collections.singletonList("nillacilan"));

        recordToUpdate = new MedicalRecord();
        recordToUpdate.setFirstName("John"); // Même identifiant
        recordToUpdate.setLastName("Boyd");
        recordToUpdate.setBirthdate("03/06/1985"); // Donnée mise à jour
        recordToUpdate.setMedications(Arrays.asList("aznol:350mg", "hydrapermazol:100mg")); // Peut être identique ou différent
        recordToUpdate.setAllergies(Collections.emptyList()); // Donnée mise à jour

        invalidRecord = new MedicalRecord();
        invalidRecord.setFirstName(null); // Prénom invalide
        invalidRecord.setLastName("Boyd");
        invalidRecord.setBirthdate("01/01/1990");
        invalidRecord.setMedications(Collections.emptyList());
        invalidRecord.setAllergies(Collections.emptyList());
    }

    // --- Tests pour POST /medicalRecord ---

    /**
     * Teste l'ajout d'un dossier médical avec des données valides.
     * Doit retourner le statut Created (201) et le dossier créé.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /medicalRecord - Doit retourner Created si données valides")
    void addMedicalRecord_whenValidInput_shouldReturnCreated() throws Exception {
        // Arrange
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class))).thenReturn(record1);

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Boyd")))
                .andExpect(jsonPath("$.medications", hasSize(2)));

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste l'ajout d'un dossier médical lorsqu'un dossier pour cette personne existe déjà.
     * Doit retourner le statut Conflict (409).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /medicalRecord - Doit retourner Conflict si dossier existe déjà")
    void addMedicalRecord_whenRecordExists_shouldReturnConflict() throws Exception {
        // Arrange: Simuler l'exception de conflit du service
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("existe déjà")); // Le message doit contenir "existe déjà"

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isConflict()); // 409

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste l'ajout d'un dossier médical avec des données invalides (prénom null).
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /medicalRecord - Doit retourner Bad Request si données invalides")
    void addMedicalRecord_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange: Simuler l'exception pour données invalides
        // Note: Le contrôleur gère différemment les exceptions selon le message.
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("Prénom et nom requis")); // Message différent de "existe déjà"

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRecord)))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste l'ajout d'un dossier médical lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("POST /medicalRecord - Doit retourner Internal Server Error en cas d'erreur service")
    void addMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(medicalRecordService.addMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(post("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(medicalRecordService).addMedicalRecord(any(MedicalRecord.class));
    }

    // --- Tests pour PUT /medicalRecord ---

    /**
     * Teste la mise à jour d'un dossier médical existant avec des données valides.
     * Doit retourner le statut OK (200) et le dossier mis à jour.
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /medicalRecord - Doit retourner OK si dossier existe et données valides")
    void updateMedicalRecord_whenRecordExists_shouldReturnOk() throws Exception {
        // Arrange
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class))).thenReturn(Optional.of(recordToUpdate));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate)))
                .andExpect(status().isOk()) // 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Boyd")))
                .andExpect(jsonPath("$.birthdate", is("03/06/1985"))) // Vérifier champ mis à jour
                .andExpect(jsonPath("$.allergies", hasSize(0))); // Vérifier champ mis à jour

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste la mise à jour d'un dossier médical qui n'existe pas.
     * Doit retourner le statut Not Found (404).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /medicalRecord - Doit retourner Not Found si dossier n'existe pas")
    void updateMedicalRecord_whenRecordNotFound_shouldReturnNotFound() throws Exception {
        // Arrange: Simuler service retournant vide
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate)))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste la mise à jour d'un dossier médical avec des données invalides (prénom null).
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /medicalRecord - Doit retourner Bad Request si données invalides")
    void updateMedicalRecord_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange: Simuler l'exception du service pour données invalides
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new IllegalArgumentException("Prénom et nom requis"));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRecord)))
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    /**
     * Teste la mise à jour d'un dossier médical lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("PUT /medicalRecord - Doit retourner Internal Server Error en cas d'erreur service")
    void updateMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(medicalRecordService.updateMedicalRecord(any(MedicalRecord.class)))
                .thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(put("/medicalRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordToUpdate)))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(medicalRecordService).updateMedicalRecord(any(MedicalRecord.class));
    }

    // --- Tests pour DELETE /medicalRecord ---

    /**
     * Teste la suppression d'un dossier médical existant.
     * Doit retourner le statut No Content (204).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /medicalRecord - Doit retourner No Content si dossier existe")
    void deleteMedicalRecord_whenRecordExists_shouldReturnNoContent() throws Exception {
        // Arrange: Simuler suppression réussie
        when(medicalRecordService.deleteMedicalRecord(eq("John"), eq("Boyd"))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John")
                        .param("lastName", "Boyd"))
                .andExpect(status().isNoContent()); // 204

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("John"), eq("Boyd"));
    }

    /**
     * Teste la suppression d'un dossier médical qui n'existe pas.
     * Doit retourner le statut Not Found (404).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /medicalRecord - Doit retourner Not Found si dossier n'existe pas")
    void deleteMedicalRecord_whenRecordNotFound_shouldReturnNotFound() throws Exception {
        // Arrange: Simuler dossier non trouvé
        when(medicalRecordService.deleteMedicalRecord(eq("Jane"), eq("Doe"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "Jane")
                        .param("lastName", "Doe"))
                .andExpect(status().isNotFound()); // 404

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("Jane"), eq("Doe"));
    }

    /**
     * Teste la suppression avec un paramètre 'firstName' manquant.
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /medicalRecord - Doit retourner Bad Request si firstName manquant")
    void deleteMedicalRecord_whenMissingFirstNameParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("lastName", "Boyd")) // firstName manquant
                .andExpect(status().isBadRequest()); // 400 (Spring gère @RequestParam requis)

        // Verify: Le service ne doit pas être appelé
        verify(medicalRecordService, never()).deleteMedicalRecord(anyString(), anyString());
    }

    /**
     * Teste la suppression avec un paramètre 'lastName' manquant.
     * Doit retourner le statut Bad Request (400).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /medicalRecord - Doit retourner Bad Request si lastName manquant")
    void deleteMedicalRecord_whenMissingLastNameParam_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John")) // lastName manquant
                .andExpect(status().isBadRequest()); // 400

        // Verify
        verify(medicalRecordService, never()).deleteMedicalRecord(anyString(), anyString());
    }

    /**
     * Teste la suppression lorsque le service lance une erreur interne.
     * Doit retourner le statut Internal Server Error (500).
     * @throws Exception si MockMvc échoue.
     */
    @Test
    @DisplayName("DELETE /medicalRecord - Doit retourner Internal Server Error en cas d'erreur service")
    void deleteMedicalRecord_whenServiceThrowsUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(medicalRecordService.deleteMedicalRecord(eq("John"), eq("Boyd")))
                .thenThrow(new RuntimeException("Erreur interne"));

        // Act & Assert
        mockMvc.perform(delete("/medicalRecord")
                        .param("firstName", "John")
                        .param("lastName", "Boyd"))
                .andExpect(status().isInternalServerError()); // 500

        // Verify
        verify(medicalRecordService).deleteMedicalRecord(eq("John"), eq("Boyd"));
    }
}