package com.campushub.support.dto;

import com.campushub.support.model.Niveau;
import com.campushub.support.model.Statut;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SupportCoursDto {
    private Long id;
    private String titre;
    private String description;
    private String fichierUrl;
    private Niveau niveau;
    private String matiere;
    private Long enseignantId;
    private LocalDate dateDepot;
    private Statut statut;
    private LocalDate dateValidation;
    private String remarqueDoyen;
}
