package com.campushub.support.repository;

import com.campushub.support.model.Statut;
import com.campushub.support.model.SupportCours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportCoursRepository extends JpaRepository<SupportCours, Long> {
    List<SupportCours> findByEnseignantId(Long enseignantId);
    List<SupportCours> findByStatut(Statut statut);
}
