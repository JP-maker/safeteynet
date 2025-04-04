package com.openclassroom.safetynet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirePersonDTO {
    private List<PersonWithMedicalRecordDTO> persons;
}
