package com.campushub.support;

import com.campushub.support.client.UserServiceClient;
import com.campushub.support.dto.CreateSupportCoursDto;
import com.campushub.support.model.Statut;
import com.campushub.support.model.SupportCours;
import com.campushub.support.repository.SupportCoursRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SupportCoursIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupportCoursRepository supportCoursRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;
    


    private final Long TEACHER_ID = 1L;
    private final Long OTHER_TEACHER_ID = 2L;
    private final Long DEAN_ID = 3L;
    private final Long ADMIN_ID = 4L;

    @BeforeEach
    void setUp() {
        supportCoursRepository.deleteAll();
        // Mock the UserServiceClient to return IDs for our test users
        Mockito.when(userServiceClient.getUserIdByUsername("teacher")).thenReturn(TEACHER_ID);
        Mockito.when(userServiceClient.getUserIdByUsername("other_teacher")).thenReturn(OTHER_TEACHER_ID);
        Mockito.when(userServiceClient.getUserIdByUsername("dean")).thenReturn(DEAN_ID);
        Mockito.when(userServiceClient.getUserIdByUsername("admin")).thenReturn(ADMIN_ID);
    }

    @Test
    @WithMockUser(username = "teacher", authorities = {"ROLE_TEACHER"})
    void shouldCreateSupportWhenUserIsTeacher() throws Exception {
        CreateSupportCoursDto createDto = new CreateSupportCoursDto();
        createDto.setTitre("Test Cours");
        createDto.setDescription("Description");
        createDto.setFichierUrl("http://example.com/file.pdf");

        mockMvc.perform(post("/api/supports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enseignantId").value(TEACHER_ID))
                .andExpect(jsonPath("$.fichierUrl").value("http://example.com/file.pdf"));
    }

    @Test
    @WithMockUser(username = "student", authorities = {"ROLE_STUDENT"})
    void shouldForbidCreateSupportWhenUserIsNotTeacher() throws Exception {
        CreateSupportCoursDto createDto = new CreateSupportCoursDto();
        createDto.setTitre("Test Cours");
        createDto.setFichierUrl("http://example.com/file.pdf"); // Added this line

        mockMvc.perform(post("/api/supports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(username = "teacher", authorities = {"ROLE_TEACHER"})
    void shouldAllowOwnerToSubmitSupport() throws Exception {
        SupportCours support = new SupportCours();
        support.setTitre("Draft");
        support.setEnseignantId(TEACHER_ID);
        support.setStatut(Statut.BROUILLON);
        support.setFichierUrl("http://test.url/draft.pdf"); // Added this line
        support = supportCoursRepository.save(support);

        mockMvc.perform(post("/api/supports/" + support.getId() + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SOUMIS"));
    }

    @Test
    @WithMockUser(username = "other_teacher", authorities = {"ROLE_TEACHER"})
    void shouldForbidNonOwnerFromSubmittingSupport() throws Exception {
        SupportCours support = new SupportCours();
        support.setTitre("Draft");
        support.setEnseignantId(TEACHER_ID); // Owned by 'teacher'
        support.setStatut(Statut.BROUILLON);
        support.setFichierUrl("http://test.url/other_draft.pdf"); // Added this line
        support = supportCoursRepository.save(support);

        mockMvc.perform(post("/api/supports/" + support.getId() + "/submit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", authorities = {"ROLE_DEAN"})
    void shouldAllowDeanToSeePendingSupports() throws Exception {
        SupportCours support = new SupportCours();
        support.setTitre("Pending Support");
        support.setEnseignantId(TEACHER_ID);
        support.setFichierUrl("http://test.url/pending.pdf");
        support.setStatut(Statut.SOUMIS); // Explicitly set status before saving
        supportCoursRepository.save(support);

        mockMvc.perform(get("/api/supports/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Pending Support"));
    }

    @Test
    @WithMockUser(username = "teacher", authorities = {"ROLE_TEACHER"})
    void shouldForbidTeacherFromSeeingPendingSupports() throws Exception {
        mockMvc.perform(get("/api/supports/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", authorities = {"ROLE_DEAN"})
    void shouldAllowDeanToValidateSupport() throws Exception {
        SupportCours support = new SupportCours();
        support.setTitre("To Validate");
        support.setEnseignantId(TEACHER_ID);
        support.setStatut(Statut.SOUMIS);
        support.setFichierUrl("http://test.url/tovalidate.pdf"); // Added this line
        support = supportCoursRepository.save(support);

        mockMvc.perform(post("/api/supports/" + support.getId() + "/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Looks good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDÉ"));
    }
}
