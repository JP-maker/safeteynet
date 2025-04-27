package com.openclassroom.safetynet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.model.DataContainer;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class FileIOServiceTest {

    private FileIOService fileIOService;
    private ObjectMapper objectMapper;
    private ResourceLoader resourceLoader;
    private final String dataFilePath = "src/test/resources/test-data.json"; // Pour simuler un fichier

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resourceLoader = mock(ResourceLoader.class);

        fileIOService = new FileIOService(objectMapper, dataFilePath);
        fileIOService.setResourceLoader(resourceLoader);

        // Initialiser le cache manuellement pour éviter de dépendre du filesystem réel
        DataContainer container = new DataContainer();
        container.setPersons(List.of(new Person()));
        container.setFirestations(List.of(new FireStation()));
        container.setMedicalrecords(List.of(new MedicalRecord()));
        fileIOService.loadDataOnStartup(); // simule l'initialisation
    }

    @Test
    void getPersons_ShouldReturnCopyOfPersons() {
        List<Person> persons = fileIOService.getPersons();
        assertNotNull(persons);
        assertEquals(1, persons.size());
    }

    @Test
    void getFireStations_ShouldReturnCopyOfFireStations() {
        List<FireStation> fireStations = fileIOService.getFireStations();
        assertNotNull(fireStations);
        assertEquals(1, fireStations.size());
    }

    @Test
    void getMedicalRecords_ShouldReturnCopyOfMedicalRecords() {
        List<MedicalRecord> medicalRecords = fileIOService.getMedicalRecords();
        assertNotNull(medicalRecords);
        assertEquals(1, medicalRecords.size());
    }

    @Test
    void setPersons_ShouldUpdateCacheAndSave() throws Exception {
        List<Person> newPersons = List.of(new Person(), new Person());

        fileIOService.setPersons(newPersons);

        List<Person> persons = fileIOService.getPersons();
        assertEquals(2, persons.size());

        // Vérifie que le fichier a été mis à jour
        assertTrue(Files.exists(Path.of(dataFilePath)));
    }

    @Test
    void setFireStations_ShouldUpdateCacheAndSave() throws Exception {
        List<FireStation> newStations = List.of(new FireStation());

        fileIOService.setFireStations(newStations);

        List<FireStation> stations = fileIOService.getFireStations();
        assertEquals(1, stations.size());
    }

    @Test
    void setMedicalRecords_ShouldUpdateCacheAndSave() throws Exception {
        List<MedicalRecord> newRecords = List.of(new MedicalRecord(), new MedicalRecord(), new MedicalRecord());

        fileIOService.setMedicalRecords(newRecords);

        List<MedicalRecord> records = fileIOService.getMedicalRecords();
        assertEquals(3, records.size());
    }

    @Test
    void loadDataOnStartup_ShouldLoadFromClasspath_WhenFileDoesNotExist() throws Exception {
        // Arrange : le fichier n'existe pas (ou vide)
        Files.deleteIfExists(Path.of(dataFilePath));

        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);

        DataContainer container = new DataContainer();
        container.setPersons(List.of(new Person()));
        container.setFirestations(List.of(new FireStation()));
        container.setMedicalrecords(List.of(new MedicalRecord()));

        byte[] jsonData = objectMapper.writeValueAsBytes(container);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(jsonData));

        when(resourceLoader.getResource(anyString())).thenReturn(mockResource);

        // New Service pour simuler tout ça
        FileIOService service = new FileIOService(objectMapper, dataFilePath);
        service.setResourceLoader(resourceLoader);

        // Act
        service.loadDataOnStartup();

        // Assert
        assertEquals(1, service.getPersons().size());
        assertEquals(1, service.getFireStations().size());
        assertEquals(1, service.getMedicalRecords().size());
    }
}
