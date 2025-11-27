package com.campushub.support.service;

import com.campushub.support.client.UserServiceClient;
import com.campushub.support.model.Statut;
import com.campushub.support.model.SupportCours;
import com.campushub.support.repository.SupportCoursRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SupportCoursServiceImpl implements SupportCoursService {

    private final SupportCoursRepository supportCoursRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;

    @Autowired
    public SupportCoursServiceImpl(SupportCoursRepository supportCoursRepository, RabbitTemplate rabbitTemplate, UserServiceClient userServiceClient) {
        this.supportCoursRepository = supportCoursRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public SupportCours createSupport(String titre, String description, String fichierUrl) {
        String authenticatedUsername = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        Long enseignantId = userServiceClient.getUserIdByUsername(authenticatedUsername);

        if (enseignantId == null) {
            throw new RuntimeException("Authenticated user ID not found or invalid.");
        }

        SupportCours support = new SupportCours();
        support.setTitre(titre);
        support.setDescription(description);
        support.setFichierUrl(fichierUrl);
        support.setEnseignantId(enseignantId); // Set from authenticated user
        return supportCoursRepository.save(support);
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
        // Envoyer un événement pour notifier le service de workflow/validation
        rabbitTemplate.convertAndSend("support.submitted", support.getId());
        return supportCoursRepository.save(support);
    }

    @Override
    public SupportCours validateSupport(Long id, String remarque) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.VALIDÉ);
        support.setDateValidation(LocalDate.now());
        support.setRemarqueDoyen(remarque);
        // Envoyer un événement
        rabbitTemplate.convertAndSend("support.validated", support.getId());
        return supportCoursRepository.save(support);
    }

    @Override
    public SupportCours rejectSupport(Long id, String remarque) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.REJETÉ);
        support.setRemarqueDoyen(remarque);
        // Envoyer un événement
        rabbitTemplate.convertAndSend("support.rejected", support.getId());
        return supportCoursRepository.save(support);
    }

    @Override
    public void deleteSupport(Long id) {
        supportCoursRepository.deleteById(id);
    }
}
