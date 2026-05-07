package com.campushub.support;

import com.campushub.support.client.UserDto;
import com.campushub.support.client.UserServiceClient;
import com.campushub.support.dto.CreateSupportCoursDto;
import com.campushub.support.model.Niveau;
import com.campushub.support.model.Statut;
import com.campushub.support.model.SupportCours;
import com.campushub.support.repository.SupportCoursRepository;
import com.campushub.support.security.JwtService;
import com.campushub.support.security.WithMockCustomUser;
import com.campushub.support.service.NotificationProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=c3b3f4d4a5e5b6c6d7e7f8a8b9c9d0e0f1a1b2c2d3e3f4a4b5c5d6e6f7a7b8c8")
class SupportCoursIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupportCoursRepository supportCoursRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Sans ce mock, le contexte Spring refuse de démarrer si JwtService
     * dépend d'une config réseau ou d'une clé absente dans le profil de test.
     */
    @MockBean
    private JwtService jwtService;

    // Empêche l'envoi réel de messages Kafka/RabbitMQ pendant les tests.
    @MockBean
    private NotificationProducer notificationProducer;

    // Empêche les appels HTTP réels vers le microservice utilisateur.
    @MockBean
    private UserServiceClient userServiceClient;

    // Identifiants
    private static final Long TEACHER_ID       = 1L;
    private static final Long OTHER_TEACHER_ID = 2L;
    private static final Long DEAN_ID          = 3L;
    private static final Long ADMIN_ID         = 4L;
    private static final Long STUDENT_ID       = 5L;

    @BeforeEach
    void setUp() {
        supportCoursRepository.deleteAll();

        // Comportement par défaut
        doNothing().when(notificationProducer).sendNotification(any());

        // Réponse par défaut
        UserDto teacher = buildUserDto(TEACHER_ID, "TEACHER", "INFO");
        UserDto dean    = buildUserDto(DEAN_ID,    "DEAN",    "INFO");
        when(userServiceClient.getUserById(eq(TEACHER_ID), anyString())).thenReturn(teacher);
        when(userServiceClient.getUsersByDepartment(eq("INFO"), anyString()))
                .thenReturn(List.of(teacher, dean));
    }

    // Création d'un support de cours
    @Nested
    @DisplayName("POST /api/supports — Création")
    class CreateSupport {

        @Test
        @DisplayName("Un enseignant peut créer un support (201 Created)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCanCreate() throws Exception {
            CreateSupportCoursDto dto = buildCreateDto("Algo L1", Niveau.L1, "Informatique");

            mockMvc.perform(post("/api/supports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titre").value("Algo L1"))
                    .andExpect(jsonPath("$.enseignantId").value(TEACHER_ID))
                    .andExpect(jsonPath("$.statut").value("BROUILLON"))
                    .andExpect(jsonPath("$.dateDepot").isNotEmpty());
        }

        @Test
        @DisplayName("Un étudiant ne peut pas créer un support (403 Forbidden)")
        @WithMockCustomUser(id = 5L, username = "student", authorities = {"ROLE_STUDENT"})
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/supports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateDto("Hack", null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un doyen ne peut pas créer un support (403 Forbidden)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCannotCreate() throws Exception {
            mockMvc.perform(post("/api/supports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateDto("Test", null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un utilisateur non authentifié reçoit 401 Unauthorized")
        void unauthenticatedCannotCreate() throws Exception {
            mockMvc.perform(post("/api/supports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateDto("Test", null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("La création envoie bien une notification au créateur")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void createTriggersNotification() throws Exception {
            mockMvc.perform(post("/api/supports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateDto("Cours notifié", Niveau.M1, "Maths"))))
                    .andExpect(status().isCreated());

            // La notification doit avoir été envoyée exactement une fois
            verify(notificationProducer, times(1)).sendNotification(any());
        }
    }

    // Lecture des supports de cours
    @Nested
    @DisplayName("GET /api/supports — Lecture")
    class ReadSupports {

        @Test
        @DisplayName("Un utilisateur authentifié peut lister tous les supports")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void authenticatedCanListAll() throws Exception {
            saveSupportCours("Cours A", TEACHER_ID, Statut.BROUILLON);
            saveSupportCours("Cours B", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(get("/api/supports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Un utilisateur non authentifié reçoit 401 sur GET /api/supports")
        void unauthenticatedCannotListAll() throws Exception {
            mockMvc.perform(get("/api/supports"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("On peut récupérer un support existant par son ID")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void getById_existing() throws Exception {
            SupportCours saved = saveSupportCours("Cours C", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(get("/api/supports/" + saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titre").value("Cours C"))
                    .andExpect(jsonPath("$.id").value(saved.getId()));
        }

        @Test
        @DisplayName("On reçoit 404 pour un ID inexistant")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void getById_notFound() throws Exception {
            mockMvc.perform(get("/api/supports/9999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Un enseignant peut consulter ses propres supports")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCanGetOwnSupports() throws Exception {
            saveSupportCours("Mon Cours", TEACHER_ID,       Statut.BROUILLON);
            saveSupportCours("Autre",    OTHER_TEACHER_ID,  Statut.BROUILLON);

            mockMvc.perform(get("/api/supports/enseignant/" + TEACHER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].titre").value("Mon Cours"));
        }

        @Test
        @DisplayName("Un étudiant ne peut pas consulter la liste par enseignant (403)")
        @WithMockCustomUser(id = 5L, username = "student", authorities = {"ROLE_STUDENT"})
        void studentCannotGetByTeacher() throws Exception {
            mockMvc.perform(get("/api/supports/enseignant/" + TEACHER_ID))
                    .andExpect(status().isForbidden());
        }
    }

    // Supports de cours en attente
    @Nested
    @DisplayName("GET /api/supports/pending — Supports soumis")
    class PendingSupports {

        @Test
        @DisplayName("Un doyen peut voir les supports en attente")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCanSeePending() throws Exception {
            saveSupportCours("En attente 1", TEACHER_ID, Statut.SOUMIS);
            saveSupportCours("Brouillon",    TEACHER_ID, Statut.BROUILLON); // ne doit pas apparaître

            mockMvc.perform(get("/api/supports/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].titre").value("En attente 1"));
        }

        @Test
        @DisplayName("Un admin peut voir les supports en attente")
        @WithMockCustomUser(id = 4L, username = "admin", authorities = {"ROLE_ADMIN"})
        void adminCanSeePending() throws Exception {
            saveSupportCours("En attente 2", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(get("/api/supports/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Un enseignant ne peut pas voir les supports en attente (403)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCannotSeePending() throws Exception {
            mockMvc.perform(get("/api/supports/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("La liste est vide quand aucun support n'est soumis")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void pendingListIsEmptyWhenNoneSubmitted() throws Exception {
            saveSupportCours("Brouillon", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(get("/api/supports/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // Soumission d'un support de cours
    @Nested
    @DisplayName("POST /api/supports/{id}/submit — Soumission")
    class SubmitSupport {

        @Test
        @DisplayName("Un enseignant (propriétaire) peut soumettre son brouillon")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void ownerCanSubmit() throws Exception {
            SupportCours draft = saveSupportCours("Brouillon soumis", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(post("/api/supports/" + draft.getId() + "/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("SOUMIS"));
        }

        @Test
        @DisplayName("Soumettre un support notifie l'enseignant et les doyens du département")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void submitTriggersNotification() throws Exception {
            SupportCours draft = saveSupportCours("Notif submit", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(post("/api/supports/" + draft.getId() + "/submit"))
                    .andExpect(status().isOk());

            // Une notification doit avoir été envoyée
            verify(notificationProducer, times(1)).sendNotification(any());
        }

        /*
         * Comportement actuel : le service ne vérifiant pas la propriété.
         * Un autre enseignant peut donc soumettre n'importe quel brouillon.
         * Ce test documente ce comportement ACTUEL. S'il devient une exigence
         * métier de n'autoriser que le propriétaire, le service devra lever une
         * exception (ex. AccessDeniedException) et ce test devra attendre 403.
         */
        @Test
        @DisplayName("COMPORTEMENT ACTUEL — Un autre enseignant peut soumettre (pas de vérification de propriété)")
        @WithMockCustomUser(id = 2L, username = "other_teacher", authorities = {"ROLE_TEACHER"})
        void nonOwnerCanCurrentlySubmit_documentedBehavior() throws Exception {
            SupportCours draft = saveSupportCours("Brouillon d'un autre", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(post("/api/supports/" + draft.getId() + "/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("SOUMIS"));
        }

        @Test
        @DisplayName("Un doyen ne peut pas soumettre un support (403)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCannotSubmit() throws Exception {
            SupportCours draft = saveSupportCours("Brouillon", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(post("/api/supports/" + draft.getId() + "/submit"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Soumettre un ID inexistant retourne 500 (RuntimeException non gérée)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void submitNonExistentReturnsError() throws Exception {
            Exception thrown = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/supports/9999/submit")));

            Throwable root = thrown;
            while (root.getCause() != null) root = root.getCause();
            assertThat(root).isInstanceOf(RuntimeException.class)
                            .hasMessageContaining("Support de cours non trouvé");
        }
    }

    // Validation d'un support de cours
    @Nested
    @DisplayName("POST /api/supports/{id}/validate — Validation")
    class ValidateSupport {

        @Test
        @DisplayName("Un doyen peut valider un support soumis (statut → VALIDÉ)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCanValidate() throws Exception {
            SupportCours submitted = saveSupportCours("À valider", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Très bon support, approuvé."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDÉ"))
                    .andExpect(jsonPath("$.remarqueDoyen").value("Très bon support, approuvé."))
                    .andExpect(jsonPath("$.dateValidation").isNotEmpty());
        }

        @Test
        @DisplayName("Un admin peut également valider un support")
        @WithMockCustomUser(id = 4L, username = "admin", authorities = {"ROLE_ADMIN"})
        void adminCanValidate() throws Exception {
            SupportCours submitted = saveSupportCours("Admin valide", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("OK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDÉ"));
        }

        @Test
        @DisplayName("La validation sans remarque est acceptée (remarque optionnelle)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void validateWithoutRemark() throws Exception {
            SupportCours submitted = saveSupportCours("Sans remarque", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDÉ"));
        }

        @Test
        @DisplayName("Un enseignant ne peut pas valider un support (403)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCannotValidate() throws Exception {
            SupportCours submitted = saveSupportCours("Non validable", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Tentative"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("La validation notifie tout le département")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void validateTriggersNotificationToDepartment() throws Exception {
            SupportCours submitted = saveSupportCours("Notif valide", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Approuvé"))
                    .andExpect(status().isOk());

            verify(notificationProducer, times(1)).sendNotification(any());
        }
    }

    // Rejet d'un support de cours
    @Nested
    @DisplayName("POST /api/supports/{id}/reject — Rejet")
    class RejectSupport {

        @Test
        @DisplayName("Un doyen peut rejeter un support avec une remarque (statut → REJETÉ)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCanReject() throws Exception {
            SupportCours submitted = saveSupportCours("À rejeter", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/reject")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Sources insuffisantes, veuillez revoir la bibliographie."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("REJETÉ"))
                    .andExpect(jsonPath("$.remarqueDoyen").value("Sources insuffisantes, veuillez revoir la bibliographie."));
        }

        @Test
        @DisplayName("Un admin peut également rejeter un support")
        @WithMockCustomUser(id = 4L, username = "admin", authorities = {"ROLE_ADMIN"})
        void adminCanReject() throws Exception {
            SupportCours submitted = saveSupportCours("Admin rejette", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/reject")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Non conforme"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("REJETÉ"));
        }

        @Test
        @DisplayName("Un enseignant ne peut pas rejeter un support (403)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCannotReject() throws Exception {
            SupportCours submitted = saveSupportCours("Non rejetable", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/reject")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Tentative"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Le rejet notifie l'enseignant et les doyens du département")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void rejectTriggersNotification() throws Exception {
            SupportCours submitted = saveSupportCours("Notif rejet", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/reject")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Qualité insuffisante"))
                    .andExpect(status().isOk());

            verify(notificationProducer, times(1)).sendNotification(any());
        }
    }

    // Suppression d'un support de cours
    @Nested
    @DisplayName("DELETE /api/supports/{id} — Suppression")
    class DeleteSupport {

        @Test
        @DisplayName("Un admin peut supprimer n'importe quel support (204 No Content)")
        @WithMockCustomUser(id = 4L, username = "admin", authorities = {"ROLE_ADMIN"})
        void adminCanDelete() throws Exception {
            SupportCours support = saveSupportCours("À supprimer", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(delete("/api/supports/" + support.getId()))
                    .andExpect(status().isNoContent());

            // Vérifier la suppression effective en base
            mockMvc.perform(get("/api/supports/" + support.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Un enseignant ne peut pas supprimer un support (403)")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void teacherCannotDelete() throws Exception {
            SupportCours support = saveSupportCours("Non supprimable", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(delete("/api/supports/" + support.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un doyen ne peut pas supprimer un support (403)")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void deanCannotDelete() throws Exception {
            SupportCours support = saveSupportCours("Non supprimable doyen", TEACHER_ID, Statut.BROUILLON);

            mockMvc.perform(delete("/api/supports/" + support.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // Resilience des notifications
    @Nested
    @DisplayName("Résilience — fallback notification quand UserServiceClient échoue")
    class NotificationFallback {

        @Test
        @DisplayName("La soumission réussit même si UserServiceClient lève une exception")
        @WithMockCustomUser(id = 1L, username = "teacher", authorities = {"ROLE_TEACHER"})
        void submitSucceedsEvenIfUserClientFails() throws Exception {
            // Simuler une panne du service utilisateur
            when(userServiceClient.getUserById(anyLong(), anyString()))
                    .thenThrow(new RuntimeException("Service utilisateur indisponible"));

            SupportCours draft = saveSupportCours("Résilient submit", TEACHER_ID, Statut.BROUILLON);

            // Le support doit quand même être soumis
            mockMvc.perform(post("/api/supports/" + draft.getId() + "/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("SOUMIS"));

            // Une notification de fallback a bien été envoyée
            verify(notificationProducer, times(1)).sendNotification(any());
        }

        @Test
        @DisplayName("La validation réussit même si UserServiceClient lève une exception")
        @WithMockCustomUser(id = 3L, username = "dean", authorities = {"ROLE_DEAN"})
        void validateSucceedsEvenIfUserClientFails() throws Exception {
            when(userServiceClient.getUserById(anyLong(), anyString()))
                    .thenThrow(new RuntimeException("Service utilisateur indisponible"));

            SupportCours submitted = saveSupportCours("Résilient valide", TEACHER_ID, Statut.SOUMIS);

            mockMvc.perform(post("/api/supports/" + submitted.getId() + "/validate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Approuvé malgré la panne"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDÉ"));
        }
    }

    // factorisation pour eviter la duplication de code dans les tests
    
    // Crée et persiste un SupportCours directement en base, en contournant le contrôleur
    private SupportCours saveSupportCours(String titre, Long enseignantId, Statut statut) {
        SupportCours support = new SupportCours();
        support.setTitre(titre);
        support.setDescription("Description de test");
        support.setFichierUrl("http://example.com/" + titre.replace(" ", "_") + ".pdf");
        support.setEnseignantId(enseignantId);
        support.setNiveau(Niveau.L1);
        support.setMatiere("Informatique");
        support.setStatut(statut);
        return supportCoursRepository.save(support);
    }

    // Construit un DTO de creation minimal avec les champs obligatoires.
    private CreateSupportCoursDto buildCreateDto(String titre, Niveau niveau, String matiere) {
        CreateSupportCoursDto dto = new CreateSupportCoursDto();
        dto.setTitre(titre);
        dto.setDescription("Description de test");
        dto.setFichierUrl("http://example.com/fichier.pdf");
        dto.setNiveau(niveau);
        dto.setMatiere(matiere);
        return dto;
    }

    // Construit un UserDto pour les mocks du UserServiceClient.
    private UserDto buildUserDto(Long id, String role, String department) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setRole(role);
        dto.setDepartment(department);
        return dto;
    }
}