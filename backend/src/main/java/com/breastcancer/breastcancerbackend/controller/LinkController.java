package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.LinkActionRequestDto;
import com.breastcancer.breastcancerbackend.dto.LinkRequestCreateDto;
import com.breastcancer.breastcancerbackend.dto.LinkResponseDto;
import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import com.breastcancer.breastcancerbackend.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    // ✅ POST /api/links/request
    @PostMapping("/request")
    public LinkResponseDto requestLink(@Valid @RequestBody LinkRequestCreateDto dto) {
        return linkService.requestLink(dto);
    }

    // ✅ POST /api/links/create-request
    @PostMapping("/create-request")
    public LinkResponseDto createRequest(@Valid @RequestBody LinkRequestCreateDto dto) {
        return linkService.requestLink(dto);
    }

    // ✅ POST /api/links/{linkId}/accept
    @PostMapping("/{linkId}/accept")
    public LinkResponseDto accept(@PathVariable UUID linkId) {
        return linkService.accept(linkId);
    }

    // ✅ POST /api/links/approve
    @PostMapping("/approve")
    public LinkResponseDto approve(@Valid @RequestBody LinkActionRequestDto dto) {
        return linkService.approve(dto);
    }

    // ✅ POST /api/links/{linkId}/reject
    @PostMapping("/{linkId}/reject")
    public LinkResponseDto reject(@PathVariable UUID linkId) {
        return linkService.reject(linkId);
    }

    // ✅ POST /api/links/refuse
    @PostMapping("/refuse")
    public LinkResponseDto refuse(@Valid @RequestBody LinkActionRequestDto dto) {
        return linkService.refuse(dto);
    }

    // ✅ POST /api/links/{linkId}/block
    @PostMapping("/{linkId}/block")
    public LinkResponseDto block(@PathVariable UUID linkId) {
        return linkService.block(linkId);
    }

    // ✅ POST /api/links/{linkId}/end
    @PostMapping("/{linkId}/end")
    public LinkResponseDto end(@PathVariable UUID linkId) {
        return linkService.end(linkId);
    }

    // ✅ GET /api/links/is-active?patientId=...&doctorId=...
    @GetMapping("/is-active")
    public boolean isLinkActive(@RequestParam UUID patientId, @RequestParam UUID doctorId) {
        return linkService.isLinkActive(patientId, doctorId);
    }

    // ✅ GET /api/links/by-pair?patientId=...&doctorId=...
    @GetMapping("/by-pair")
    public LinkResponseDto getByPair(@RequestParam UUID patientId, @RequestParam UUID doctorId) {
        return linkService.getByPair(patientId, doctorId);
    }

    // ✅ GET /api/links/doctor/{doctorId}?status=REQUESTED
    @GetMapping("/doctor/{doctorId}")
    public List<LinkResponseDto> listDoctorLinks(
            @PathVariable UUID doctorId,
            @RequestParam PatientDoctorLink.Status status
    ) {
        return linkService.listDoctorLinks(doctorId, status);
    }

    // ✅ GET /api/links/patient/{patientId}?status=REQUESTED
    @GetMapping("/patient/{patientId}")
    public List<LinkResponseDto> listPatientLinks(
            @PathVariable UUID patientId,
            @RequestParam PatientDoctorLink.Status status
    ) {
        return linkService.listPatientLinks(patientId, status);
    }

    // ✅ GET /api/links/pending?actorType=patient|doctor&actorId=...
    @GetMapping("/pending")
    public List<LinkResponseDto> pending(
            @RequestParam String actorType,
            @RequestParam UUID actorId
    ) {
        return linkService.listPending(actorType, actorId);
    }

    // ✅ GET /api/links/connected?actorType=patient|doctor&actorId=...
    @GetMapping("/connected")
    public List<LinkResponseDto> connected(
            @RequestParam String actorType,
            @RequestParam UUID actorId
    ) {
        return linkService.listConnected(actorType, actorId);
    }
}
