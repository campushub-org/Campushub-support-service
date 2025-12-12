package com.campushub.support.dto;

import com.campushub.support.model.Niveau;
import com.campushub.support.model.Statut;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List; // Import List

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportNotification implements Serializable {
    private Long supportId;
    private String titre;
    private List<Long> recipientUserIds; // Changed from single enseignantId to a list of recipientUserIds
    private Long enseignantId; // Added enseignantId
    private Statut statut;
    private Niveau niveau;
    private String matiere;
}
