package com.campushub.support.controller;

import com.campushub.support.dto.CreateSupportCoursDto;
import com.campushub.support.dto.SupportCoursDto;
import com.campushub.support.model.Statut;
import com.campushub.support.model.SupportCours;
import com.campushub.support.service.SupportCoursService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supports")
public class SupportCoursController {

    private final SupportCoursService supportCoursService;

    public SupportCoursController(SupportCoursService supportCoursService) {
        this.supportCoursService = supportCoursService;
    }

    private SupportCoursDto convertToDto(SupportCours supportCours) {
        SupportCoursDto dto = new SupportCoursDto();
        dto.setId(supportCours.getId());
        dto.setTitre(supportCours.getTitre());
        dto.setDescription(supportCours.getDescription());
        dto.setFichierUrl(supportCours.getFichierUrl());
        dto.setEnseignantId(supportCours.getEnseignantId());
        dto.setDateDepot(supportCours.getDateDepot());
        dto.setStatut(supportCours.getStatut());
        dto.setDateValidation(supportCours.getDateValidation());
        dto.setRemarqueDoyen(supportCours.getRemarqueDoyen());
        return dto;
    }

    // Endpoint for teachers to create a new course material
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<SupportCoursDto> createSupport(@RequestBody CreateSupportCoursDto createDto) {
        SupportCours createdSupport = supportCoursService.createSupport(
                createDto.getTitre(),
                createDto.getDescription(),
                createDto.getFichierUrl()
        );
        return new ResponseEntity<>(convertToDto(createdSupport), HttpStatus.CREATED);
    }

    // Endpoint to get all supports, could be restricted
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SupportCoursDto> getAllSupports() {
        return supportCoursService.getAllSupports().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Endpoint to get a support by ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportCoursDto> getSupportById(@PathVariable Long id) {
        return supportCoursService.getSupportById(id)
                .map(support -> ResponseEntity.ok(convertToDto(support)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint for a teacher to get their own supports
    @GetMapping("/enseignant/{enseignantId}")
    @PreAuthorize("@supportSecurity.isUser(authentication, #enseignantId) or hasAuthority('ROLE_ADMIN')")
    public List<SupportCoursDto> getSupportsByEnseignant(@PathVariable Long enseignantId) {
        return supportCoursService.getSupportsByEnseignant(enseignantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Endpoint for the dean to see pending supports
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('ROLE_DEAN', 'ROLE_ADMIN')")
    public List<SupportCoursDto> getPendingSupports() {
        return supportCoursService.getSupportsByStatut(Statut.SOUMIS).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Endpoint for a teacher to submit their draft
    @PostMapping("/{id}/submit")
    @PreAuthorize("@supportSecurity.isOwner(authentication, #id) and hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<SupportCoursDto> submitSupport(@PathVariable Long id) {
        return ResponseEntity.ok(convertToDto(supportCoursService.submitSupport(id)));
    }

    // Endpoint for the dean to validate a support
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyAuthority('ROLE_DEAN', 'ROLE_ADMIN')")
    public ResponseEntity<SupportCoursDto> validateSupport(@PathVariable Long id, @RequestBody(required = false) String remarque) {
        return ResponseEntity.ok(convertToDto(supportCoursService.validateSupport(id, remarque)));
    }

    // Endpoint for the dean to reject a support
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_DEAN', 'ROLE_ADMIN')")
    public ResponseEntity<SupportCoursDto> rejectSupport(@PathVariable Long id, @RequestBody String remarque) {
        return ResponseEntity.ok(convertToDto(supportCoursService.rejectSupport(id, remarque)));
    }
    
    // Endpoint for a teacher to delete a draft
    @DeleteMapping("/{id}")
    @PreAuthorize("@supportSecurity.isOwner(authentication, #id) or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteSupport(@PathVariable Long id) {
        // Business logic should ensure only drafts can be deleted, for example
        supportCoursService.deleteSupport(id);
        return ResponseEntity.noContent().build();
    }
}
