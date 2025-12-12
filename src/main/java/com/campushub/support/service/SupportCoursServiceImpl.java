package com.campushub.support.service;
import com.campushub.support.client.UserDto;
import com.campushub.support.client.UserServiceClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SupportCoursServiceImpl implements SupportCoursService {

    private static final Logger logger = LoggerFactory.getLogger(SupportCoursServiceImpl.class);

    private final SupportCoursRepository supportCoursRepository;
    private final NotificationProducer notificationProducer;
    private final UserServiceClient userServiceClient; // Inject UserServiceClient

    @Autowired
    public SupportCoursServiceImpl(SupportCoursRepository supportCoursRepository, NotificationProducer notificationProducer, UserServiceClient userServiceClient) {
        this.supportCoursRepository = supportCoursRepository;
        this.notificationProducer = notificationProducer;
        this.userServiceClient = userServiceClient;
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
        support.setEnseignantId(enseignantId);
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification to the teacher who created the support
        List<Long> recipientUserIds = Collections.singletonList(enseignantId);
        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                recipientUserIds, // Use the list of recipient IDs
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
        return supportCoursRepository.save(support);
    }

    @Override
    public SupportCours submitSupport(Long id) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.SOUMIS);
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification to the teacher and deans of the department
        List<Long> recipientUserIds;
        try {
            String token = getJwtFromSecurityContext();
            UserDto teacher = userServiceClient.getUserById(savedSupport.getEnseignantId(), token);
            String department = teacher.getDepartment();
            List<UserDto> usersInDepartment = userServiceClient.getUsersByDepartment(department, token);

            recipientUserIds = usersInDepartment.stream()
                    .filter(user -> "DEAN".equals(user.getRole()))
                    .map(UserDto::getId)
                    .collect(Collectors.toList());
            if (!recipientUserIds.contains(savedSupport.getEnseignantId())) {
                 recipientUserIds.add(savedSupport.getEnseignantId());
            }

        } catch (Exception e) {
            logger.error("Error fetching users for notification in submitSupport: {}", e.getMessage());
            recipientUserIds = Collections.singletonList(savedSupport.getEnseignantId()); // Fallback to notifying only the teacher
        }

        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                recipientUserIds, // Use the list of recipient IDs
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
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification to all users in the department
        List<Long> recipientUserIds;
        try {
            String token = getJwtFromSecurityContext();
            UserDto teacher = userServiceClient.getUserById(savedSupport.getEnseignantId(), token);
            String department = teacher.getDepartment();
            List<UserDto> usersInDepartment = userServiceClient.getUsersByDepartment(department, token);

            recipientUserIds = usersInDepartment.stream()
                    .map(UserDto::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching users for notification in validateSupport: {}", e.getMessage());
            recipientUserIds = Collections.singletonList(savedSupport.getEnseignantId()); // Fallback to notifying only the teacher
        }

        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                recipientUserIds, // Use the list of recipient IDs
                savedSupport.getEnseignantId(),
                savedSupport.getStatut(),
                savedSupport.getNiveau(),
                savedSupport.getMatiere()
        );
        notificationProducer.sendNotification(notification);

        return savedSupport;
    }

    @Override
    public SupportCours rejectSupport(Long id, String remarque) {
        SupportCours support = supportCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support de cours non trouvé"));
        support.setStatut(Statut.REJETÉ);
        support.setRemarqueDoyen(remarque);
        SupportCours savedSupport = supportCoursRepository.save(support);

        // Send notification to the teacher and deans of the department
        List<Long> recipientUserIds;
        try {
            String token = getJwtFromSecurityContext();
            UserDto teacher = userServiceClient.getUserById(savedSupport.getEnseignantId(), token);
            String department = teacher.getDepartment();
            List<UserDto> usersInDepartment = userServiceClient.getUsersByDepartment(department, token);

            recipientUserIds = usersInDepartment.stream()
                    .filter(user -> "DEAN".equals(user.getRole()))
                    .map(UserDto::getId)
                    .collect(Collectors.toList());
            if (!recipientUserIds.contains(savedSupport.getEnseignantId())) {
                recipientUserIds.add(savedSupport.getEnseignantId());
            }
        } catch (Exception e) {
            logger.error("Error fetching users for notification in rejectSupport: {}", e.getMessage());
            recipientUserIds = Collections.singletonList(savedSupport.getEnseignantId()); // Fallback
        }
        
        SupportNotification notification = new SupportNotification(
                savedSupport.getId(),
                savedSupport.getTitre(),
                recipientUserIds,
                savedSupport.getEnseignantId(),
                savedSupport.getStatut(),
                savedSupport.getNiveau(),
                savedSupport.getMatiere()
        );
        notificationProducer.sendNotification(notification);

        return savedSupport;
    }

    @Override
    public void deleteSupport(Long id) {
        supportCoursRepository.deleteById(id);
    }

    private String getJwtFromSecurityContext() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getToken();
        }
        throw new RuntimeException("Could not retrieve JWT token from SecurityContext.");
    }
}
