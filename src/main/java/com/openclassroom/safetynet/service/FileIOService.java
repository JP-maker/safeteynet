package com.openclassroom.safetynet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // Pour INDENT_OUTPUT
import com.openclassroom.safetynet.model.DataContainer;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects; // Import pour Objects.requireNonNullElse
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer; // Import pour Consumer

/**
 * Service responsable de la lecture, de l'écriture et de la mise en cache des données
 * de l'application (Personnes, Casernes, Dossiers Médicaux) à partir d'un fichier JSON.
 * <p>
 * Ce service gère un cache interne ({@link DataContainer}) pour un accès rapide aux données.
 * Il implémente une stratégie de chargement qui tente d'abord de lire depuis un fichier
 * de stockage sur le système de fichiers. Si ce fichier n'existe pas ou est invalide,
 * il se rabat sur le chargement d'un fichier initial depuis le classpath et le copie
 * vers l'emplacement de stockage.
 * </p><p>
 * Toutes les opérations de lecture et d'écriture sur le cache et le fichier sont
 * thread-safe grâce à l'utilisation d'un {@link ReentrantReadWriteLock}.
 * Les méthodes Getters retournent des copies immuables des listes pour protéger le cache.
 * </p>
 */
@Service
public class FileIOService implements ResourceLoaderAware {

    private static final Logger logger = LoggerFactory.getLogger(FileIOService.class);

    /** Le chemin vers le fichier de données de stockage sur le système de fichiers. */
    private final String dataFilePath;

    /** Verrou pour gérer l'accès concurrentiel au cache et au fichier. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** Mapper Jackson pour la sérialisation/désérialisation JSON. */
    private final ObjectMapper objectMapper;

    /** Cache en mémoire contenant les données désérialisées du fichier JSON. */
    private DataContainer dataCache;

    /** Chargeur de ressources Spring pour accéder aux fichiers du classpath. */
    private ResourceLoader resourceLoader;

    /**
     * Méthode de l'interface {@link ResourceLoaderAware} appelée par Spring pour injecter
     * le chargeur de ressources.
     *
     * @param resourceLoader Le ResourceLoader fourni par le contexte Spring.
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Construit une nouvelle instance de FileIOService.
     * Configure le chemin du fichier de données et initialise l'ObjectMapper Jackson.
     *
     * @param objectMapper L'ObjectMapper à utiliser pour la conversion JSON (sera configuré pour l'indentation).
     * @param dataFilePath Le chemin vers le fichier de données de stockage, injecté depuis les propriétés de l'application
     *                     (valeur de {@code safetynet.data.file}).
     */
    @Autowired
    public FileIOService(ObjectMapper objectMapper,
                         @Value("${safetynet.data.file}") String dataFilePath) {
        this.dataFilePath = dataFilePath;
        // Configure Jackson pour une sortie lisible (optionnel mais utile)
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        logger.info("Data file path configured (used for storage and potentially initial load): {}", dataFilePath);
    }

    /**
     * Méthode exécutée automatiquement après l'initialisation du bean par Spring.
     * Déclenche le chargement initial des données depuis le fichier.
     * Voir {@link #loadData()}.
     */
    @PostConstruct
    public void loadDataOnStartup() {
        loadData();
    }

    // --- Getters (avec Read Lock et Copie Immuable) ---

    /**
     * Récupère la liste des personnes actuellement chargées dans le cache.
     * <p>
     * Retourne une copie immuable de la liste pour empêcher les modifications externes du cache.
     * L'accès est thread-safe (protégé par un verrou en lecture).
     * </p>
     *
     * @return Une {@code List<Person>} immuable contenant les personnes, ou une liste vide immuable
     *         si le cache est vide ou non initialisé.
     */
    public List<Person> getPersons() {
        lock.readLock().lock();
        try {
            return dataCache != null && dataCache.getPersons() != null
                    ? List.copyOf(dataCache.getPersons()) // Retourne une copie immuable
                    : List.of(); // Retourne une liste vide immuable
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Récupère la liste des casernes de pompiers actuellement chargées dans le cache.
     * <p>
     * Retourne une copie immuable de la liste pour empêcher les modifications externes du cache.
     * L'accès est thread-safe (protégé par un verrou en lecture).
     * </p>
     *
     * @return Une {@code List<FireStation>} immuable contenant les casernes, ou une liste vide immuable
     *         si le cache est vide ou non initialisé.
     */
    public List<FireStation> getFireStations() {
        lock.readLock().lock();
        try {
            return dataCache != null && dataCache.getFirestations() != null
                    ? List.copyOf(dataCache.getFirestations())
                    : List.of();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Récupère la liste des dossiers médicaux actuellement chargés dans le cache.
     * <p>
     * Retourne une copie immuable de la liste pour empêcher les modifications externes du cache.
     * L'accès est thread-safe (protégé par un verrou en lecture).
     * </p>
     *
     * @return Une {@code List<MedicalRecord>} immuable contenant les dossiers médicaux, ou une liste vide immuable
     *         si le cache est vide ou non initialisé.
     */
    public List<MedicalRecord> getMedicalRecords() {
        lock.readLock().lock();
        try {
            return dataCache != null && dataCache.getMedicalrecords() != null
                    ? List.copyOf(dataCache.getMedicalrecords())
                    : List.of();
        } finally {
            lock.readLock().unlock();
        }
    }

    // --- Setters (avec Write Lock et Sauvegarde) ---

    /**
     * Remplace la liste complète des personnes dans le cache en mémoire
     * et déclenche la sauvegarde de l'intégralité des données ({@link DataContainer}) dans le fichier de stockage.
     * <p>
     * L'opération est thread-safe (protégée par un verrou en écriture exclusif).
     * Une copie interne de la liste fournie est utilisée pour mettre à jour le cache.
     * </p>
     *
     * @param persons La nouvelle liste complète des objets {@link Person} à utiliser. Si null, une liste vide sera utilisée.
     */
    public void setPersons(List<Person> persons) {
        // Utilise une copie pour plus de sécurité et gère le cas null
        List<Person> safeList = Objects.requireNonNullElse(persons, Collections.<Person>emptyList());
        updateCacheAndSave(container -> container.setPersons(new ArrayList<>(safeList)));
    }

    /**
     * Remplace la liste complète des casernes dans le cache en mémoire
     * et déclenche la sauvegarde de l'intégralité des données ({@link DataContainer}) dans le fichier de stockage.
     * <p>
     * L'opération est thread-safe (protégée par un verrou en écriture exclusif).
     * Une copie interne de la liste fournie est utilisée pour mettre à jour le cache.
     * </p>
     *
     * @param fireStations La nouvelle liste complète des objets {@link FireStation} à utiliser. Si null, une liste vide sera utilisée.
     */
    public void setFireStations(List<FireStation> fireStations) {
        List<FireStation> safeList = Objects.requireNonNullElse(fireStations, Collections.<FireStation>emptyList());
        updateCacheAndSave(container -> container.setFirestations(new ArrayList<>(safeList)));
    }

    /**
     * Remplace la liste complète des dossiers médicaux dans le cache en mémoire
     * et déclenche la sauvegarde de l'intégralité des données ({@link DataContainer}) dans le fichier de stockage.
     * <p>
     * L'opération est thread-safe (protégée par un verrou en écriture exclusif).
     * Une copie interne de la liste fournie est utilisée pour mettre à jour le cache.
     * </p>
     *
     * @param medicalRecords La nouvelle liste complète des objets {@link MedicalRecord} à utiliser. Si null, une liste vide sera utilisée.
     */
    public void setMedicalRecords(List<MedicalRecord> medicalRecords) {
        List<MedicalRecord> safeList = Objects.requireNonNullElse(medicalRecords, Collections.<MedicalRecord>emptyList());
        updateCacheAndSave(container -> container.setMedicalrecords(new ArrayList<>(safeList)));
    }

    // --- Méthodes privées principales ---

    /**
     * Méthode interne et thread-safe pour appliquer une modification au cache de données
     * et ensuite sauvegarder l'état complet du cache dans le fichier de stockage.
     * Utilise un verrou en écriture pour garantir l'atomicité de l'opération (modification + sauvegarde).
     * Logue une erreur si la sauvegarde échoue.
     *
     * @param updateAction Une fonction (Consumer) qui prend le {@link DataContainer} du cache
     *                     et applique les modifications nécessaires.
     */
    private void updateCacheAndSave(Consumer<DataContainer> updateAction) {
        lock.writeLock().lock();
        try {
            if (this.dataCache == null) {
                logger.error("Tentative de modification sur un cache non initialisé ! Impossible de sauvegarder.");
                // Il serait peut-être préférable de lancer une IllegalStateException ici
                return;
            }
            // Appliquer la modification au cache
            updateAction.accept(this.dataCache);
            // Sauvegarder l'état complet du cache dans le fichier
            saveDataToFileInternal();
        } catch (IOException e) {
            // Logguer l'erreur mais l'application continue (le cache est modifié mais pas le fichier)
            logger.error("Erreur lors de la sauvegarde des données après modification dans {}", dataFilePath, e);
            // Selon les besoins, on pourrait relancer une exception ici ou mettre en place une stratégie de retry.
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Charge les données de l'application.
     * <p>
     * Tente d'abord de charger depuis le fichier spécifié par {@code dataFilePath} sur le système de fichiers.
     * Si ce fichier n'existe pas, est vide, ou si une erreur de lecture se produit,
     * tente de charger les données initiales depuis un fichier du même nom situé dans le classpath.
     * </p><p>
     * Si les données sont chargées depuis le classpath (premier démarrage ou récupération après erreur),
     * elles sont immédiatement sauvegardées dans le fichier de stockage sur le système de fichiers.
     * </p><p>
     * S'assure que les listes dans le cache (persons, firestations, medicalrecords) ne sont jamais nulles
     * après le chargement.
     * </p><p>
     * Utilise un verrou en écriture pour la durée du chargement et de la sauvegarde initiale éventuelle.
     * Lance une {@link RuntimeException} en cas d'erreur fatale (fichier initial introuvable, erreur de parsing majeure).
     * </p>
     */
    private void loadData() {
        lock.writeLock().lock(); // Verrou en écriture pour modifier le cache et potentiellement écrire
        try {
            Path storagePath = Paths.get(dataFilePath).toAbsolutePath();
            logger.info("Chemin de stockage cible (filesystem) : {}", storagePath);

            boolean loadedFromStorage = false;

            // 1. Essayer de charger depuis le fichier de stockage (filesystem)
            if (Files.exists(storagePath) && Files.size(storagePath) > 0) {
                logger.info("Tentative de chargement depuis le fichier de stockage existant: {}", storagePath);
                try (InputStream inputStream = Files.newInputStream(storagePath)) {
                    this.dataCache = objectMapper.readValue(inputStream, DataContainer.class);
                    validateCacheLists(); // S'assurer que les listes internes ne sont pas null
                    logger.info("Données chargées avec succès depuis le fichier de stockage.");
                    loadedFromStorage = true;
                } catch (IOException e) {
                    logger.warn("Échec du chargement depuis le fichier de stockage '{}'. Il est peut-être corrompu. Tentative de chargement depuis le classpath. Erreur: {}", storagePath.toString(), e.getMessage());
                    // Ne pas re-lancer ici, on va essayer le classpath
                }
            } else {
                logger.info("Fichier de stockage '{}' non trouvé ou vide. Tentative de chargement depuis le classpath.", storagePath.toString());
            }

            // 2. Si échec ou non trouvé, charger depuis le classpath et copier vers stockage
            if (!loadedFromStorage) {
                String classpathFileName = Paths.get(dataFilePath).getFileName().toString();
                String classpathResourcePath = "classpath:" + classpathFileName;
                logger.info("Chargement des données initiales depuis le classpath: {}", classpathResourcePath);

                Resource initialResource = resourceLoader.getResource(classpathResourcePath);

                if (!initialResource.exists()) {
                    logger.error("FATAL: Le fichier de données initial '{}' n'a pas été trouvé dans le classpath.", classpathResourcePath);
                    throw new RuntimeException("Fichier de données initiales introuvable dans le classpath : " + classpathResourcePath + ". Impossible d'initialiser.");
                } else {
                    // Charger depuis le classpath
                    try (InputStream inputStream = initialResource.getInputStream()) {
                        this.dataCache = objectMapper.readValue(inputStream, DataContainer.class);
                        validateCacheLists(); // S'assurer que les listes internes ne sont pas null
                        logger.info("Données initiales chargées avec succès depuis le classpath ({}).", classpathResourcePath);

                        // 3. Sauvegarder immédiatement vers le chemin de stockage
                        try {
                            logger.info("Sauvegarde initiale des données dans le fichier de stockage: {}", storagePath);
                            saveDataToFileInternal(); // Écrit dans dataFilePath (externe)
                        } catch (IOException saveEx) {
                            // C'est une erreur assez grave car l'application ne pourra pas sauvegarder
                            logger.error("FATAL: Impossible d'écrire les données initiales dans le fichier de stockage '{}'. L'application risque de ne pas sauvegarder les modifications. Erreur: {}", storagePath, saveEx.getMessage(), saveEx);
                            // On pourrait lancer une RuntimeException ici pour arrêter l'application
                            // throw new RuntimeException("Impossible d'initialiser le fichier de stockage.", saveEx);
                        }
                    } catch (IOException e) {
                        logger.error("FATAL: Erreur lors de la lecture ou du parsing du fichier initial du classpath '{}': {}", classpathResourcePath, e.getMessage(), e);
                        throw new RuntimeException("Impossible de charger les données initiales depuis le classpath " + classpathResourcePath, e);
                    }
                }
            }
            logCacheCounts(); // Logguer le nombre d'éléments chargés

        } catch (IOException e) { // Attrape l'IOException de Files.size() ou Files.exists()
            logger.error("FATAL: Erreur d'IO lors de l'accès initial au fichier de stockage '{}': {}", dataFilePath, e.getMessage(), e);
            throw new RuntimeException("Erreur d'IO lors de l'accès au fichier de stockage", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Méthode interne pour écrire l'état actuel du {@link #dataCache} dans le fichier
     * de stockage spécifié par {@link #dataFilePath}.
     * Crée les répertoires parents si nécessaire.
     * Cette méthode suppose que le verrou en écriture ({@code lock.writeLock()}) est déjà détenu
     * par le thread appelant.
     *
     * @throws IOException Si une erreur survient lors de la création des répertoires
     *                     ou de l'écriture dans le fichier.
     */
    private void saveDataToFileInternal() throws IOException {
        // Utiliser le chemin absolu pour être clair dans les logs et éviter les ambiguïtés
        Path storagePath = Paths.get(dataFilePath).toAbsolutePath();
        logger.debug("Préparation de l'écriture dans le fichier de stockage: {}", storagePath);

        // Créer les répertoires parents si nécessaire
        Path parentDir = storagePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            logger.info("Création des répertoires parents pour {}", parentDir);
            // Lève une IOException si la création échoue
            Files.createDirectories(parentDir);
        }

        // Écrire dans le fichier
        try {
            // Utiliser writeValueAsBytes pour éviter les problèmes d'encodage potentiels avec writeValue(File,...)
            byte[] jsonBytes = objectMapper.writeValueAsBytes(this.dataCache);
            Files.write(storagePath, jsonBytes); // Écrase le fichier s'il existe
            logger.info("Données sauvegardées avec succès dans {}", storagePath);
        } catch (IOException e) {
            // Logguer l'erreur avant de la relancer pour que l'appelant (updateCacheAndSave) puisse la gérer
            logger.error("Erreur lors de l'écriture dans le fichier {}", storagePath, e);
            throw e;
        }
    }

    /**
     * Méthode utilitaire privée pour s'assurer que les listes à l'intérieur de l'objet
     * {@link #dataCache} ne sont pas nulles après le chargement. Si elles sont nulles,
     * elles sont initialisées avec des {@link ArrayList} vides.
     * Initialise également {@code dataCache} lui-même s'il est null.
     */
    private void validateCacheLists() {
        if (this.dataCache == null) {
            logger.warn("DataCache est null après le chargement, initialisation avec un conteneur vide.");
            this.dataCache = new DataContainer(); // Initialiser pour éviter NPE
        }
        // Utiliser Objects.requireNonNullElseGet pour une initialisation concise
        this.dataCache.setPersons(Objects.requireNonNullElseGet(this.dataCache.getPersons(), ArrayList::new));
        this.dataCache.setFirestations(Objects.requireNonNullElseGet(this.dataCache.getFirestations(), ArrayList::new));
        this.dataCache.setMedicalrecords(Objects.requireNonNullElseGet(this.dataCache.getMedicalrecords(), ArrayList::new));
    }

    /**
     * Méthode utilitaire privée pour logger le nombre d'enregistrements de chaque type
     * présents dans le cache après le chargement.
     */
    private void logCacheCounts() {
        if (this.dataCache != null) {
            // Utiliser null-safe size check
            logger.info("{} enregistrements de personnes dans le cache.",
                    this.dataCache.getPersons() != null ? this.dataCache.getPersons().size() : 0);
            logger.info("{} enregistrements de fire station dans le cache.",
                    this.dataCache.getFirestations() != null ? this.dataCache.getFirestations().size() : 0);
            logger.info("{} enregistrements de dossier médical dans le cache.",
                    this.dataCache.getMedicalrecords() != null ? this.dataCache.getMedicalrecords().size() : 0);
        } else {
            logger.warn("DataCache est null lors du logging des compteurs.");
        }
    }
}