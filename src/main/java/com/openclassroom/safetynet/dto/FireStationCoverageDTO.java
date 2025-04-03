package com.openclassroom.safetynet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FireStationCoverageDTO {
    private List<PersonInfoDTO> people;
    private int adultCount;
    private int childCount;
}