package com.openclassroom.safetynet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressWithListOfPersonWithMedicalRecordDTO {
    private String address;
    private List<PersonWithMedicalRecordDTO> persons;
}
