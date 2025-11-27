package com.campushub.support.service;

import com.campushub.support.model.SupportCours;
import com.campushub.support.model.Statut;

import java.util.List;
import java.util.Optional;

public interface SupportCoursService {
    SupportCours createSupport(String titre, String description, String fichierUrl);
    Optional<SupportCours> getSupportById(Long id);
    List<SupportCours> getAllSupports();
    List<SupportCours> getSupportsByEnseignant(Long enseignantId);
    List<SupportCours> getSupportsByStatut(Statut statut);
    SupportCours updateSupport(Long id, String titre, String description, String fichierUrl);
    SupportCours submitSupport(Long id);
    SupportCours validateSupport(Long id, String remarque);
    SupportCours rejectSupport(Long id, String remarque);
    void deleteSupport(Long id);
}
