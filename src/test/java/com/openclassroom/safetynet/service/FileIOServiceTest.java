package com.openclassroom.safetynet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassroom.safetynet.model.DataContainer;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import org.junit.jupiter.api.AfterEach; // Pour le nettoyage après chaque test
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // Pour la lisibilité
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir; // Pour créer un répertoire temporaire
import org.mockito.Mock; // Utiliser l'annotation Mockito
import org.mockito.junit.jupiter.MockitoExtension; // Utiliser l'extension Mockito
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
// SpringExtension n'est pas nécessaire ici car ce n'est pas un test d'intégration Spring
// import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException; // Import pour IOException
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList; // Utiliser ArrayList pour les setters
import java.util.Collections; // Pour les listes vides
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat; // Utiliser AssertJ pour des assertions plus fluides

/**
 * Classe de test unitaire pour {@link FileIOService}.
 * <p>
 * Teste la logique de chargement, de sauvegarde et d'accès aux données en cache,
 * en utilisant un répertoire temporaire pour simuler le fichier de stockage et
 * Mockito pour simuler le {@link ResourceLoader} lors du chargement initial depuis le classpath.
 * </p>
 */
@ExtendWith(MockitoExtension.class) // Utiliser l'extension Mockito pour JUnit 5
class FileIOServiceTest {

    // Dépendances mockées ou réelles nécessaires
    private ObjectMapper objectMapper; // Utiliser un ObjectMapper réel
    @Mock // Mocker le ResourceLoader
    private ResourceLoader resourceLoader;
    @Mock // Mocker la ressource du classpath
    private Resource mockClasspathResource;

    // Instance de la classe sous test
    private FileIOService fileIOService;

    /**
     * Répertoire temporaire injecté par JUnit 5 pour les tests.
     * Utilisé pour créer et manipuler le fichier de stockage simulé.
     * Ce répertoire est nettoyé automatiquement après chaque test.
     */
    @TempDir
    Path tempDir;

    /**
     * Chemin vers le fichier de stockage simulé dans le répertoire temporaire.
     */
    private Path testStorageFilePath;

    /**
     * Nom du fichier utilisé pour le classpath (doit correspondre au nom de fichier dans dataFilePath).
     */
    private final String CLASSPATH_FILENAME = "test-data.json";

    /**
     * Méthode d'initialisation exécutée avant chaque test (@Test).
     * Configure l'ObjectMapper, définit le chemin du fichier de test dans le
     * répertoire temporaire, et initialise l'instance de {@link FileIOService}.
     *
     * @throws IOException Si une erreur survient lors de la création du chemin.
     */
    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper(); // ObjectMapper réel pour les tests de sérialisation/désérialisation
        // Définir le chemin du fichier de stockage dans le répertoire temporaire
        testStorageFilePath = tempDir.resolve(CLASSPATH_FILENAME);

        // Initialiser le service AVEC le chemin du fichier temporaire
        fileIOService = new FileIOService(objectMapper, testStorageFilePath.toString());
        fileIOService.setResourceLoader(resourceLoader); // Injecter le mock ResourceLoader
    }

    /**
     * Méthode de nettoyage exécutée après chaque test (@Test).
     * Assure la suppression du fichier de test s'il a été créé.
     * (Note: @TempDir gère normalement le nettoyage du répertoire, mais c'est une sécurité supplémentaire).
     *
     * @throws IOException Si une erreur survient lors de la suppression.
     */
    @AfterEach
    void tearDown() throws IOException {
        // Nettoyer le fichier de test après chaque exécution (sécurité)
        Files.deleteIfExists(testStorageFilePath);
    }


    // --- Tests de chargement initial (loadDataOnStartup / loadData) ---

    /**
     * Teste le scénario où le fichier de stockage n'existe pas au démarrage.
     * Le service doit charger les données depuis le classpath simulé,
     * initialiser le cache et créer/sauvegarder le fichier de stockage.
     *
     * @throws Exception Si une erreur survient durant le test.
     */
    @Test
    @DisplayName("Chargement: Doit charger depuis classpath et créer fichier si stockage inexistant")
    void loadDataOnStartup_ShouldLoadFromClasspathAndCreateFile_WhenStorageFileDoesNotExist() throws Exception {
        // --- Arrange ---
        // 1. S'assurer que le fichier de stockage n'existe PAS
        assertThat(Files.exists(testStorageFilePath)).isFalse();

        // 2. Préparer les données de test pour le classpath
        DataContainer classpathData = new DataContainer();
        Person person = new Person();
        person.setFirstName("Classpath");
        person.setLastName("User");
        person.setAddress("CP Addr");
        person.setCity("CP City");
        person.setZip("CP Zip");
        person.setPhone("CP Phone");
        person.setEmail("cp@mail.com");
        classpathData.setPersons(List.of(person));
        FireStation fireStation = new FireStation();
        fireStation.setAddress("CP Addr");
        fireStation.setStation("CP1");
        classpathData.setFirestations(List.of(fireStation));
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setFirstName("Classpath");
        medicalRecord.setLastName("User");
        medicalRecord.setBirthdate("01/01/2000");
        medicalRecord.setMedications(null);
        medicalRecord.setAllergies(null);
        classpathData.setMedicalrecords(List.of(medicalRecord));
        byte[] classpathJsonData = objectMapper.writeValueAsBytes(classpathData);

        // 3. Configurer le mock ResourceLoader et Resource
        when(resourceLoader.getResource(eq("classpath:" + CLASSPATH_FILENAME))).thenReturn(mockClasspathResource);
        when(mockClasspathResource.exists()).thenReturn(true); // Simuler l'existence de la ressource classpath
        // Simuler le retour du contenu JSON depuis le classpath
        when(mockClasspathResource.getInputStream()).thenReturn(new ByteArrayInputStream(classpathJsonData));

        // --- Act ---
        // Appeler la méthode qui déclenche le chargement (@PostConstruct)
        fileIOService.loadDataOnStartup(); // Appelle loadData()

        // --- Assert ---
        // 1. Vérifier le contenu du cache
        assertThat(fileIOService.getPersons()).hasSize(1);
        assertThat(fileIOService.getPersons().get(0).getFirstName()).isEqualTo("Classpath");
        assertThat(fileIOService.getFireStations()).hasSize(1);
        assertThat(fileIOService.getFireStations().get(0).getStation()).isEqualTo("CP1");
        assertThat(fileIOService.getMedicalRecords()).hasSize(1);
        assertThat(fileIOService.getMedicalRecords().get(0).getBirthdate()).isEqualTo("01/01/2000");

        // 2. Vérifier que le fichier de stockage a été créé
        assertThat(Files.exists(testStorageFilePath)).isTrue();

        // 3. Vérifier que le contenu du fichier de stockage correspond aux données du classpath
        DataContainer savedData = objectMapper.readValue(testStorageFilePath.toFile(), DataContainer.class);
        assertThat(savedData.getPersons()).hasSize(1);
        assertThat(savedData.getPersons().get(0).getFirstName()).isEqualTo("Classpath");

        // --- Verify ---
        // Vérifier les interactions avec les mocks
        verify(resourceLoader).getResource(eq("classpath:" + CLASSPATH_FILENAME));
        verify(mockClasspathResource).exists();
        verify(mockClasspathResource).getInputStream();
    }

    /**
     * Teste le scénario où le fichier de stockage existe déjà au démarrage.
     * Le service doit charger les données depuis ce fichier de stockage
     * et ne PAS tenter de lire depuis le classpath.
     *
     * @throws Exception Si une erreur survient durant le test.
     */
    @Test
    @DisplayName("Chargement: Doit charger depuis fichier stockage si existant")
    void loadDataOnStartup_ShouldLoadFromStorageFile_WhenStorageFileExists() throws Exception {
        // --- Arrange ---
        // 1. Créer un fichier de stockage préexistant avec des données spécifiques
        DataContainer storageData = new DataContainer();
        Person person = new Person();
        person.setFirstName("Storage");
        person.setLastName("User");
        person.setAddress("Store Addr");
        person.setCity("Store City");
        person.setZip("Store Zip");
        person.setPhone("Store Phone");
        person.setEmail("store@mail.com");
        storageData.setPersons(List.of(person));
        storageData.setFirestations(Collections.emptyList()); // Pas de station dans ce fichier
        storageData.setMedicalrecords(Collections.emptyList()); // Pas de MR
        objectMapper.writeValue(testStorageFilePath.toFile(), storageData); // Écrire le fichier

        assertThat(Files.exists(testStorageFilePath)).isTrue(); // Confirmer l'existence

        // --- Act ---
        // Appeler la méthode qui déclenche le chargement
        fileIOService.loadDataOnStartup();

        // --- Assert ---
        // 1. Vérifier le contenu du cache (doit correspondre au fichier de stockage)
        assertThat(fileIOService.getPersons()).hasSize(1);
        assertThat(fileIOService.getPersons().get(0).getFirstName()).isEqualTo("Storage");
        assertThat(fileIOService.getFireStations()).isEmpty(); // Doit être vide
        assertThat(fileIOService.getMedicalRecords()).isEmpty(); // Doit être vide

        // --- Verify ---
        // S'assurer que le ResourceLoader n'a PAS été sollicité
        verify(resourceLoader, never()).getResource(anyString());
        verify(mockClasspathResource, never()).exists();
        verify(mockClasspathResource, never()).getInputStream();
    }

    /**
     * Teste le scénario où le fichier de stockage existe mais est corrompu (JSON invalide).
     * Le service doit échouer à lire le fichier de stockage, puis charger depuis le classpath
     * et écraser le fichier corrompu avec les données du classpath.
     *
     * @throws Exception Si une erreur survient durant le test.
     */
    @Test
    @DisplayName("Chargement: Doit charger depuis classpath si fichier stockage corrompu")
    void loadDataOnStartup_ShouldLoadFromClasspath_WhenStorageFileIsCorrupt() throws Exception {
        // --- Arrange ---
        // 1. Créer un fichier de stockage corrompu
        Files.writeString(testStorageFilePath, "{invalid json", StandardOpenOption.CREATE);
        assertThat(Files.exists(testStorageFilePath)).isTrue();

        // 2. Préparer les données de test pour le classpath (comme dans le premier test)
        DataContainer classpathData = new DataContainer();
        Person person = new Person();
        person.setFirstName("Classpath");
        person.setLastName("Good");
        person.setAddress("CP Addr");
        person.setCity("CP City");
        person.setZip("CP Zip");
        person.setPhone("CP Phone");
        person.setEmail("cp@mail.com");
        classpathData.setPersons(List.of(person));
        byte[] classpathJsonData = objectMapper.writeValueAsBytes(classpathData);

        // 3. Configurer les mocks pour le chargement classpath
        when(resourceLoader.getResource(eq("classpath:" + CLASSPATH_FILENAME))).thenReturn(mockClasspathResource);
        when(mockClasspathResource.exists()).thenReturn(true);
        when(mockClasspathResource.getInputStream()).thenReturn(new ByteArrayInputStream(classpathJsonData));

        // --- Act ---
        fileIOService.loadDataOnStartup();

        // --- Assert ---
        // 1. Vérifier le contenu du cache (doit être celui du classpath)
        assertThat(fileIOService.getPersons()).hasSize(1);
        assertThat(fileIOService.getPersons().get(0).getFirstName()).isEqualTo("Classpath");

        // 2. Vérifier que le fichier de stockage a été réécrit (et n'est plus corrompu)
        assertThat(Files.exists(testStorageFilePath)).isTrue();
        DataContainer savedData = objectMapper.readValue(testStorageFilePath.toFile(), DataContainer.class);
        assertThat(savedData.getPersons()).hasSize(1);
        assertThat(savedData.getPersons().get(0).getFirstName()).isEqualTo("Classpath");

        // --- Verify ---
        verify(resourceLoader).getResource(eq("classpath:" + CLASSPATH_FILENAME)); // Classpath a été utilisé
    }

    /**
     * Teste le scénario où le fichier de stockage n'existe pas ET la ressource classpath n'existe pas.
     * Le service doit lancer une RuntimeException.
     */
    @Test
    @DisplayName("Chargement: Doit lancer RuntimeException si stockage et classpath introuvables")
    void loadDataOnStartup_ShouldThrowRuntimeException_WhenBothFilesMissing() {
        // --- Arrange ---
        // 1. S'assurer que le fichier de stockage n'existe pas
        assertThat(Files.exists(testStorageFilePath)).isFalse();

        // 2. Configurer le mock pour que la ressource classpath n'existe pas
        when(resourceLoader.getResource(eq("classpath:" + CLASSPATH_FILENAME))).thenReturn(mockClasspathResource);
        when(mockClasspathResource.exists()).thenReturn(false); // Classpath non trouvé

        // --- Act & Assert ---
        // Vérifier que l'appel à loadDataOnStartup lance l'exception attendue
        assertThrows(RuntimeException.class, () -> {
            fileIOService.loadDataOnStartup();
        }, "Une RuntimeException aurait dû être lancée car aucun fichier source n'est disponible.");

        // --- Verify ---
        verify(resourceLoader).getResource(eq("classpath:" + CLASSPATH_FILENAME));
        verify(mockClasspathResource).exists();
    }

    // --- Tests des Getters (après initialisation) ---

    // Pour tester les getters, on a besoin d'un état initial du cache.
    // On peut le faire en appelant loadDataOnStartup dans un @BeforeEach
    // ou en chargeant manuellement des données dans un fichier temporaire avant chaque test de getter.
    // L'approche la plus simple est d'avoir un setup qui charge des données initiales.

    /**
     * Classe interne pour configurer un état initial du cache avant les tests des getters/setters.
     */
    static class BaseSetupForGetterSetterTests {
        // Utiliser les mêmes mocks et instance de service que la classe externe
        ObjectMapper objectMapper = new ObjectMapper();
        @Mock ResourceLoader resourceLoader = mock(ResourceLoader.class); // Nouveau mock pour setup isolé si besoin
        FileIOService fileIOServiceInstance;
        Path storageFilePath;

        void initializeServiceWithData(Path tempStoragePath, DataContainer initialData) throws IOException {
            storageFilePath = tempStoragePath;
            objectMapper.writeValue(storageFilePath.toFile(), initialData); // Créer fichier initial
            fileIOServiceInstance = new FileIOService(objectMapper, storageFilePath.toString());
            fileIOServiceInstance.setResourceLoader(resourceLoader); // Pas besoin de configurer le mock ici
            fileIOServiceInstance.loadDataOnStartup(); // Charger depuis le fichier créé
        }
    }

    /**
     * Teste {@code getPersons} après chargement initial.
     * Doit retourner une copie immuable des personnes chargées.
     * @throws IOException si erreur IO dans setup.
     */
    @Test
    @DisplayName("Getter: getPersons Doit retourner copie immuable des personnes")
    void getPersons_ShouldReturnImmutableCopyOfPersons() throws IOException {
        // Arrange: Préparer données et service via helper
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        Person p1 = new Person();
        p1.setFirstName("Getter");
        p1.setLastName("Test");
        p1.setAddress("Addr");
        p1.setCity("City");
        p1.setZip("Zip");
        p1.setPhone("Phone");
        p1.setEmail("g@t.com");
        initialData.setPersons(List.of(p1));
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        // Act
        List<Person> persons = service.getPersons();

        // Assert
        assertThat(persons).isNotNull().hasSize(1);
        assertThat(persons.get(0).getFirstName()).isEqualTo("Getter");
        // Vérifier l'immutabilité (devrait lancer UnsupportedOperationException)
        assertThrows(UnsupportedOperationException.class, () -> persons.add(new Person()));

        // Verify: S'assurer qu'aucun appel de chargement/sauvegarde n'a eu lieu pendant le get
        verify(setup.resourceLoader, never()).getResource(anyString());
    }

    // (Tests similaires pour getFireStations et getMedicalRecords)

    @Test
    @DisplayName("Getter: getFireStations Doit retourner copie immuable des casernes")
    void getFireStations_ShouldReturnImmutableCopyOfFireStations() throws IOException {
        // Arrange
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        FireStation fs1 = new FireStation();
        fs1.setStation("FS1");
        fs1.setAddress("AddrFS");
        initialData.setFirestations(List.of(fs1));
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        // Act
        List<FireStation> stations = service.getFireStations();

        // Assert
        assertThat(stations).isNotNull().hasSize(1);
        assertThat(stations.get(0).getStation()).isEqualTo("FS1");
        assertThrows(UnsupportedOperationException.class, () -> stations.add(new FireStation()));
    }

    @Test
    @DisplayName("Getter: getMedicalRecords Doit retourner copie immuable des dossiers")
    void getMedicalRecords_ShouldReturnImmutableCopyOfMedicalRecords() throws IOException {
        // Arrange
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        MedicalRecord mr1 = new MedicalRecord();
        mr1.setFirstName("MR");
        mr1.setLastName("Test");
        mr1.setBirthdate("01/01/2001");
        mr1.setMedications(null);
        mr1.setAllergies(null);

        initialData.setMedicalrecords(List.of(mr1));
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        // Act
        List<MedicalRecord> records = service.getMedicalRecords();

        // Assert
        assertThat(records).isNotNull().hasSize(1);
        assertThat(records.get(0).getFirstName()).isEqualTo("MR");
        assertThrows(UnsupportedOperationException.class, () -> records.add(new MedicalRecord()));
    }


    // --- Tests des Setters ---

    /**
     * Teste {@code setPersons}. Doit mettre à jour le cache interne et sauvegarder
     * l'état complet dans le fichier de stockage.
     * @throws Exception si erreur IO.
     */
    @Test
    @DisplayName("Setter: setPersons Doit mettre à jour cache et sauvegarder fichier")
    void setPersons_ShouldUpdateCacheAndSaveToFile() throws Exception {
        // Arrange: Charger des données initiales
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        Person initialPerson = new Person();
        initialPerson.setFirstName("Initial");
        initialPerson.setLastName("Person");
        initialPerson.setAddress("Old");
        initialPerson.setCity("Old");
        initialPerson.setZip("Old");
        initialPerson.setPhone("Old");
        initialPerson.setEmail("old@mail.com");
        initialData.setPersons(List.of(initialPerson)); // Charger une personne initiale
        FireStation initialFireStation = new FireStation();
        initialFireStation.setAddress("FSAddr");
        initialFireStation.setStation("1");
        initialData.setFirestations(List.of(initialFireStation)); // Garder d'autres données
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        // Préparer la nouvelle liste
        List<Person> newPersons = new ArrayList<>(); // Utiliser ArrayList pour être sûr que c'est mutable
        Person newPerson = new Person();
        newPerson.setFirstName("New");
        newPerson.setLastName("Person1");
        newPerson.setAddress("Addr1");
        newPerson.setCity("City1");
        newPerson.setZip("Zip1");
        newPerson.setPhone("Ph1");
        newPerson.setEmail("new1@mail.com");
        newPersons.add(newPerson);
        Person newPerson2 = new Person();
        newPerson2.setFirstName("New");
        newPerson2.setLastName("Person2");
        newPerson2.setAddress("Addr2");
        newPerson2.setCity("City2");
        newPerson2.setZip("Zip2");
        newPerson2.setPhone("Ph2");
        newPerson2.setEmail("new2@mail.com");
        newPersons.add(newPerson2);

        // Act: Appeler la méthode setter
        service.setPersons(newPersons);

        // Assert
        // 1. Vérifier le cache via le getter
        List<Person> personsInCache = service.getPersons();
        assertThat(personsInCache).hasSize(2);
        assertThat(personsInCache.get(0).getFirstName()).isEqualTo("New");
        assertThat(personsInCache.get(0).getLastName()).isEqualTo("Person1");
        assertThat(personsInCache.get(1).getLastName()).isEqualTo("Person2");

        // 2. Vérifier le contenu du fichier sauvegardé
        assertThat(Files.exists(setup.storageFilePath)).isTrue();
        DataContainer savedData = objectMapper.readValue(setup.storageFilePath.toFile(), DataContainer.class);
        // Vérifier que la liste des personnes dans le fichier est la nouvelle liste
        assertThat(savedData.getPersons()).hasSize(2);
        assertThat(savedData.getPersons().get(0).getFirstName()).isEqualTo("New");
        assertThat(savedData.getPersons().get(0).getLastName()).isEqualTo("Person1");
        // Vérifier que les autres données (firestations) sont toujours présentes
        assertThat(savedData.getFirestations()).hasSize(1);
        assertThat(savedData.getFirestations().get(0).getStation()).isEqualTo("1");
    }

    /**
     * Teste {@code setPersons} avec une liste nulle.
     * Doit vider la liste des personnes dans le cache et sauvegarder.
     * @throws Exception si erreur IO.
     */
    @Test
    @DisplayName("Setter: setPersons avec null Doit vider la liste et sauvegarder")
    void setPersons_withNullList_ShouldClearListAndSave() throws Exception {
        // Arrange
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        Person initialPerson = new Person();
        initialPerson.setFirstName("Initial");
        initialPerson.setLastName("Person");
        initialPerson.setAddress("Old");
        initialPerson.setCity("Old");
        initialPerson.setZip("Old");
        initialPerson.setPhone("Old");
        initialPerson.setEmail("old@mail.com");
        initialData.setPersons(List.of(initialPerson)); // Charger une personne initiale
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;
        assertThat(service.getPersons()).isNotEmpty(); // Vérifier état initial

        // Act
        service.setPersons(null); // Appeler avec null

        // Assert
        // 1. Vérifier le cache
        assertThat(service.getPersons()).isEmpty();

        // 2. Vérifier le fichier
        assertThat(Files.exists(setup.storageFilePath)).isTrue();
        DataContainer savedData = objectMapper.readValue(setup.storageFilePath.toFile(), DataContainer.class);
        assertThat(savedData.getPersons()).isNotNull().isEmpty(); // La liste doit être vide mais non nulle
    }


    // (Tests similaires pour setFireStations et setMedicalRecords)

    @Test
    @DisplayName("Setter: setFireStations Doit mettre à jour cache et sauvegarder")
    void setFireStations_ShouldUpdateCacheAndSaveToFile() throws Exception {
        // Arrange
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        FireStation initialFireStation = new FireStation();
        initialFireStation.setAddress("OldAddr");
        initialFireStation.setStation("OldFS");
        initialData.setFirestations(List.of(initialFireStation)); // Charger une station initiale
        initialData.setPersons(List.of(new Person())); // Garder d'autres données
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        List<FireStation> newStations = new ArrayList<>();
        FireStation newStation = new FireStation();
        newStation.setAddress("NewAddr");
        newStation.setStation("NewFS");
        newStations.add(newStation);

        // Act
        service.setFireStations(newStations);

        // Assert
        // 1. Cache
        List<FireStation> stationsInCache = service.getFireStations();
        assertThat(stationsInCache).hasSize(1);
        assertThat(stationsInCache.get(0).getAddress()).isEqualTo("NewAddr");

        // 2. Fichier
        assertThat(Files.exists(setup.storageFilePath)).isTrue();
        DataContainer savedData = objectMapper.readValue(setup.storageFilePath.toFile(), DataContainer.class);
        assertThat(savedData.getFirestations()).hasSize(1);
        assertThat(savedData.getFirestations().get(0).getAddress()).isEqualTo("NewAddr");
        assertThat(savedData.getPersons()).hasSize(1); // Les autres données sont préservées
    }

    @Test
    @DisplayName("Setter: setMedicalRecords Doit mettre à jour cache et sauvegarder")
    void setMedicalRecords_ShouldUpdateCacheAndSaveToFile() throws Exception {
        // Arrange
        BaseSetupForGetterSetterTests setup = new BaseSetupForGetterSetterTests();
        DataContainer initialData = new DataContainer();
        MedicalRecord initialRecord = new MedicalRecord();
        initialRecord.setFirstName("Old");
        initialRecord.setLastName("Rec");
        initialRecord.setBirthdate("01/01/2000");
        initialRecord.setMedications(null);
        initialRecord.setAllergies(null);
        initialData.setMedicalrecords(List.of(initialRecord));
        setup.initializeServiceWithData(testStorageFilePath, initialData);
        FileIOService service = setup.fileIOServiceInstance;

        List<MedicalRecord> newRecords = new ArrayList<>();
        MedicalRecord newRecord = new MedicalRecord();
        newRecord.setFirstName("New");
        newRecord.setLastName("Rec");
        newRecord.setBirthdate("02/02/2002");
        newRecord.setMedications(null);
        newRecord.setAllergies(null);
        newRecords.add(newRecord);

        // Act
        service.setMedicalRecords(newRecords);

        // Assert
        // 1. Cache
        List<MedicalRecord> recordsInCache = service.getMedicalRecords();
        assertThat(recordsInCache).hasSize(1);
        assertThat(recordsInCache.get(0).getFirstName()).isEqualTo("New");

        // 2. Fichier
        assertThat(Files.exists(setup.storageFilePath)).isTrue();
        DataContainer savedData = objectMapper.readValue(setup.storageFilePath.toFile(), DataContainer.class);
        assertThat(savedData.getMedicalrecords()).hasSize(1);
        assertThat(savedData.getMedicalrecords().get(0).getFirstName()).isEqualTo("New");
    }
}