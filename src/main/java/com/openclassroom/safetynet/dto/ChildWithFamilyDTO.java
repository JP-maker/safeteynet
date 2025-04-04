package com.openclassroom.safetynet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildWithFamilyDTO {
    private List<ChildInfoDTO> children;
    private List<PersonInfoDTO> familyMembers;
}
