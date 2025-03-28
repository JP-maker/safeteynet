package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.model.DataContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DataLoadingService {

    private final DataContainer dataContainer;

    public DataLoadingService() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data.json")) {
            this.dataContainer = mapper.readValue(inputStream, DataContainer.class);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier JSON", e);
        }
    }

    public DataContainer getData() {
        return dataContainer;
    }
}
