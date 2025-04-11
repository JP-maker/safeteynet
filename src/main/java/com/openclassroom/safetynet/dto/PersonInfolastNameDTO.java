package com.openclassroom.safetynet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfolastNameDTO {
    private String address;
    private int age;
    private String email;
    private List<String> medications;
    private List<String> allergies;
}
