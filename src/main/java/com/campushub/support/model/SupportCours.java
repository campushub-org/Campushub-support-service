package com.campushub.support.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "support_cours")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportCours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Lob
    private String description;

    @Column(nullable = false)
    private String fichierUrl;

    @Enumerated(EnumType.STRING)
    private Niveau niveau;

    private String matiere;

    @Column(nullable = false)
    private Long enseignantId;

    private LocalDate dateDepot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    private LocalDate dateValidation;

    @Lob
    private String remarqueDoyen;

    @PrePersist
    protected void onCreate() {
        if (dateDepot == null) {
            dateDepot = LocalDate.now();
        }
        if (statut == null) {
            statut = Statut.BROUILLON;
        }
    }
}
