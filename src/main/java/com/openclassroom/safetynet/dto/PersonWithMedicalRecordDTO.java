package com.openclassroom.safetynet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithMedicalRecordDTO {
    private String lastName;
    private String phone;
    private String fireStation;
    private int age;
    private List<String> medications;
    private List<String> allergies;
}
