package com.campushub.support.service;
import com.campushub.support.dto.SupportNotification;
import com.campushub.support.security.CustomUserDetails;
import com.campushub.support.model.Niveau;
import com.campushub.support.model.Statut; // Import mis à jour
import com.campushub.support.model.SupportCours;
import com.campushub.support.repository.SupportCoursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SupportCoursServiceImpl implements SupportCoursService {

    private static final Logger logger = LoggerFactory.getLogger(SupportCoursServiceImpl.class);

    private final SupportCoursRepository supportCoursRepository;
    private final NotificationProducer notificationProducer;

    @Autowired
    public SupportCoursServiceImpl(SupportCoursRepository supportCoursRepository, NotificationProducer notificationProducer) {
        this.supportCoursRepository = supportCoursRepository;
        this.notificationProducer = notificationProducer;
    }

    @Override
    public SupportCours createSupport(String titre, String description, String fichierUrl, Niveau niveau, String matiere) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long enseignantId;

        if (principal instanceof CustomUserDetails) {
            enseignantId = ((CustomUserDetails) principal).getId();
            logger.info("Authenticated user authorities: {}", ((CustomUserDetails) principal).getAuthorities());
        } else {
            logger.warn("Principal is not CustomUserDetails: {}", principal.getClass().getName());
            // Fallback for tests or other authentication types
            throw new RuntimeException("Authenticated user is not of type CustomUserDetails.");
        }

        if (enseignantId == null) {
            throw new RuntimeException("Authenticated user ID not found.");
        }
        
        SupportCours support = new SupportCours();
        support.setTitre(titre);
        support.setDescription(description);
        support.setFichierUrl(fichierUrl);
        support.setNiveau(niveau);
        support.setMatiere(matiere);
        support.setEnseignantId(enseignantId); // Set from authenticated user
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification
        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                savedSupport.getEnseignantId(),
                savedSupport.getStatut(),
                savedSupport.getNiveau(),
                savedSupport.getMatiere()
        );
        notificationProducer.sendNotification(notification);

        return savedSupport;
    }

    @Override
    public Optional<SupportCours> getSupportById(Long id) {
        return supportCoursRepository.findById(id);
    }

    @Override
    public List<SupportCours> getAllSupports() {
        return supportCoursRepository.findAll();
    }

    @Override
    public List<SupportCours> getSupportsByEnseignant(Long enseignantId) {
        return supportCoursRepository.findByEnseignantId(enseignantId);
    }
    
    @Override
    public List<SupportCours> getSupportsByStatut(Statut statut) {
        return supportCoursRepository.findByStatut(statut);
    }

    @Override
    public SupportCours updateSupport(Long id, String titre, String description, String fichierUrl) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setTitre(titre);
        support.setDescription(description);
        support.setFichierUrl(fichierUrl);
        // Ensure that the enseignantId is not changed during an update, only set at creation
        return supportCoursRepository.save(support);
    }

    @Override
    public SupportCours submitSupport(Long id) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.SOUMIS);
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification
        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                savedSupport.getEnseignantId(),
                savedSupport.getStatut(),
                savedSupport.getNiveau(),
                savedSupport.getMatiere()
        );
        notificationProducer.sendNotification(notification);

        return savedSupport;
    }

    @Override
    public SupportCours validateSupport(Long id, String remarque) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.VALIDÉ);
        support.setDateValidation(LocalDate.now());
        support.setRemarqueDoyen(remarque);
        return supportCoursRepository.save(support);
    }

    @Override
    public SupportCours rejectSupport(Long id, String remarque) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.REJETÉ);
        support.setRemarqueDoyen(remarque);
        return supportCoursRepository.save(support);
    }

    @Override
    public void deleteSupport(Long id) {
        supportCoursRepository.deleteById(id);
    }
}
