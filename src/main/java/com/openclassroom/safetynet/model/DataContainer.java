package com.openclassroom.safetynet.model;

import lombok.Data;

import java.util.List;

@Data
public class DataContainer {
    private List<Person> persons;
    private List<Firestation> firestations;
    private List<MedicalRecord> medicalrecords;
}
