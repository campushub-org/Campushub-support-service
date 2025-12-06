package com.campushub.support.dto;

import com.campushub.support.model.Niveau;
import lombok.Data;

@Data
public class CreateSupportCoursDto {
    private String titre;
    private String description;
    private String fichierUrl;
    private Niveau niveau;
    private String matiere;
}
