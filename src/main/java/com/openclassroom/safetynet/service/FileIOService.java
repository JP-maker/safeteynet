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
import org.springframework.context.ResourceLoaderAware; // Nécessaire
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader; // Nécessaire
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;      // Utiliser Path
import java.nio.file.Paths;    // Utiliser Paths
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class FileIOService implements ResourceLoaderAware { // N'oubliez pas ResourceLoaderAware

    private static final Logger logger = LoggerFactory.getLogger(FileIOService.class);

    private final String dataFilePath;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    private DataContainer dataCache;
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Autowired
    public FileIOService(ObjectMapper objectMapper,
                         @Value("${safetynet.data.file}") String dataFilePath) { // Injecte la valeur unique
        this.dataFilePath = dataFilePath;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        logger.info("Data file path configured (used for storage and potentially initial load): {}", dataFilePath);
    }

    @PostConstruct
    public void loadDataOnStartup() {
        loadData();
    }

    // --- Getters (avec Read Lock et Copie Immuable) ---
    public List<Person> getPersons() {
        lock.readLock().lock();
        try {
            return dataCache != null && dataCache.getPersons() != null
                    ? List.copyOf(dataCache.getPersons())
                    : List.of();
        } finally {
            lock.readLock().unlock();
        }
    }
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
    public void setPersons(List<Person> persons) {
        updateCacheAndSave(container -> container.setPersons(new ArrayList<>(persons)));
    }
    public void setFireStations(List<FireStation> fireStations) {
        updateCacheAndSave(container -> container.setFirestations(new ArrayList<>(fireStations)));
    }
    public void setMedicalRecords(List<MedicalRecord> medicalRecords) {
        updateCacheAndSave(container -> container.setMedicalrecords(new ArrayList<>(medicalRecords)));
    }

    // --- Méthode générique pour mettre à jour le cache et sauvegarder) ---
    private void updateCacheAndSave(java.util.function.Consumer<DataContainer> updateAction) {
        lock.writeLock().lock();
        try {
            if (this.dataCache == null) {
                logger.error("Tentative de modification sur un cache non initialisé !");
                return;
            }
            updateAction.accept(this.dataCache);
            saveDataToFileInternal();
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde des données après modification dans {}", dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            Path storagePath = Paths.get(dataFilePath).toAbsolutePath();
            logger.info("Chemin de stockage cible (filesystem) : {}", storagePath);

            boolean loadedFromStorage = false;

            // 1. Essayer de charger depuis le fichier de stockage (filesystem)
            if (Files.exists(storagePath) && Files.size(storagePath) > 0) {
                logger.info("Tentative de chargement depuis le fichier de stockage existant: {}", storagePath);
                try (InputStream inputStream = Files.newInputStream(storagePath)) {
                    this.dataCache = objectMapper.readValue(inputStream, DataContainer.class);
                    validateCacheLists();
                    logger.info("Données chargées avec succès depuis le fichier de stockage.");
                    loadedFromStorage = true;
                } catch (IOException e) {
                    logger.warn("Échec du chargement depuis le fichier de stockage '{}'. Il est peut-être corrompu. Tentative de chargement depuis le classpath. Erreur: {}", storagePath, e.getMessage());
                }
            } else {
                logger.info("Fichier de stockage '{}' non trouvé ou vide. Tentative de chargement depuis le classpath.", storagePath);
            }

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
                        validateCacheLists();
                        logger.info("Données initiales chargées avec succès depuis le classpath ({}).", classpathResourcePath);

                        // 3. IMPORTANT: Sauvegarder immédiatement vers le chemin de stockage
                        try {
                            logger.info("Sauvegarde initiale des données dans le fichier de stockage: {}", storagePath);
                            saveDataToFileInternal(); // Écrit dans dataFilePath (externe)
                        } catch (IOException saveEx) {
                            logger.error("FATAL: Impossible d'écrire les données initiales dans le fichier de stockage '{}'. L'application risque de ne pas sauvegarder les modifications. Erreur: {}", storagePath, saveEx.getMessage(), saveEx);
                            // Gérer cette erreur critique - l'application ne pourra pas écrire
                            // throw new RuntimeException("Impossible d'initialiser le fichier de stockage.", saveEx);
                        }
                    } catch (IOException e) {
                        logger.error("FATAL: Erreur lors de la lecture ou du parsing du fichier initial du classpath '{}': {}", classpathResourcePath, e.getMessage(), e);
                        throw new RuntimeException("Impossible de charger les données initiales depuis le classpath " + classpathResourcePath, e);
                    }
                }
            }
            logCacheCounts();

        } catch (IOException e) {
            logger.error("FATAL: Erreur d'IO lors de la vérification du fichier de stockage '{}': {}", dataFilePath, e.getMessage(), e);
            throw new RuntimeException("Erreur d'IO lors de l'accès au fichier de stockage", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Logique de Sauvegarde ---
    private void saveDataToFileInternal() throws IOException {
        Path storagePath = Paths.get(dataFilePath); // Toujours le chemin externe
        logger.debug("Préparation de l'écriture dans le fichier de stockage: {}", storagePath);
        // Créer les répertoires parents si nécessaire (important pour ex: ./data/data.json)
        Path parentDir = storagePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            logger.info("Création des répertoires parents pour {}", parentDir);
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                logger.error("Impossible de créer les répertoires parents: {}", parentDir, e);
                throw new IOException("Impossible de créer les répertoires parents : " + parentDir, e);
            }
        }

        // Écrire dans le fichier (en utilisant Files pour potentiellement plus d'options futures)
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(this.dataCache);
            Files.write(storagePath, jsonBytes); // Écrase le fichier s'il existe
            logger.info("Données sauvegardées avec succès dans {}", storagePath);
        } catch (IOException e) {
            logger.error("Erreur lors de l'écriture dans le fichier {}", storagePath, e);
            throw e; // Relancer
        }
    }

    // --- Helpers (validateCacheLists, logCacheCounts ) ---
    private void validateCacheLists() {
        if (this.dataCache == null) {
            logger.warn("DataCache est null après le chargement, initialisation avec un conteneur vide.");
            this.dataCache = new DataContainer();
        }
        if (this.dataCache.getPersons() == null) this.dataCache.setPersons(new ArrayList<>());
        if (this.dataCache.getFirestations() == null) this.dataCache.setFirestations(new ArrayList<>());
        if (this.dataCache.getMedicalrecords() == null) this.dataCache.setMedicalrecords(new ArrayList<>());
    }
    private void logCacheCounts() {
        if (this.dataCache != null) {
            logger.info("{} enregistrements de personnes dans le cache.", this.dataCache.getPersons() != null ? this.dataCache.getPersons().size() : 0);
            logger.info("{} enregistrements de fire station dans le cache.", this.dataCache.getFirestations() != null ? this.dataCache.getFirestations().size() : 0);
            logger.info("{} enregistrements de dossier médical dans le cache.", this.dataCache.getMedicalrecords() != null ? this.dataCache.getMedicalrecords().size() : 0);
        } else {
            logger.warn("DataCache est null lors du logging des compteurs.");
        }
    }
}