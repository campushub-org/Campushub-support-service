package com.campushub.support.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Column(nullable=true, name = "personne_valide_ids")
    private String personneValideIds;

    @Column(nullable=true, name = "personne_reject_ids")
    private String personneRejectIds;

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

    public List<Long> getPersonneValide() {
        if (personneValideIds == null || personneValideIds.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(personneValideIds.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public void addPersonneValide(Long userId) {
        List<Long> ids = getPersonneValide();

        if (!ids.contains(userId)) {
            ids.add(userId);
        }

        this.personneValideIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getPersonneReject() {
        if (personneRejectIds == null || personneRejectIds.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(personneRejectIds.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public void addPersonneReject(Long userId) {
        List<Long> ids = getPersonneReject();

        if (!ids.contains(userId)) {
            ids.add(userId);
        }

        this.personneRejectIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
