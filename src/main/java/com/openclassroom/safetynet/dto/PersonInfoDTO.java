package com.openclassroom.safetynet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfoDTO {
    private String firstName;
    private String lastName;
    private String address;
    private String phone;
}