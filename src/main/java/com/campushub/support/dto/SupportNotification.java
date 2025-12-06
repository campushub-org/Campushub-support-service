package com.campushub.support.dto;

import com.campushub.support.model.Niveau;
import com.campushub.support.model.Statut;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportNotification implements Serializable {
    private Long supportId;
    private String titre;
    private Long enseignantId;
    private Statut statut;
    private Niveau niveau;
    private String matiere;
}
