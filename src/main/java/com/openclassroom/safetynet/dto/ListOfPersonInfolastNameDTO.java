package com.openclassroom.safetynet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListOfPersonInfolastNameDTO {
    private String lastName;
    private List<PersonInfolastNameDTO> personInfoList;
}
