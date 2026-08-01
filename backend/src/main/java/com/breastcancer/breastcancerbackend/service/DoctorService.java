package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorService(DoctorProfileRepository doctorProfileRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
    }

    public DoctorProfileResponseDto getById(UUID doctorProfileId) {
        DoctorProfile dp = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable."));
        return toResponse(dp);
    }

    public DoctorProfileResponseDto getByUserId(UUID userId) {
        DoctorProfile dp = doctorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable pour userId."));
        return toResponse(dp);
    }

    @Transactional
    public DoctorProfileResponseDto update(UUID doctorProfileId, DoctorProfileUpdateRequestDto dto) {
        DoctorProfile dp = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable."));

        if (dto.getDoctorType() != null) dp.setDoctorType(dto.getDoctorType());
        if (dto.getSpeciality() != null) dp.setSpeciality(dto.getSpeciality());
        if (dto.getClinicName() != null) dp.setClinicName(dto.getClinicName());
        if (dto.getBio() != null) dp.setBio(dto.getBio());
        if (dto.getYearsOfExperience() != null) dp.setYearsOfExperience(dto.getYearsOfExperience());
        if (dto.getLanguages() != null) dp.setLanguages(dto.getLanguages());
        if (dto.getConsultationMode() != null) dp.setConsultationMode(dto.getConsultationMode());
        if (dto.getConsultationFee() != null) dp.setConsultationFee(dto.getConsultationFee());
        if (dto.getAddressText() != null) dp.setAddressText(dto.getAddressText());
        if (dto.getLatitude() != null) dp.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) dp.setLongitude(dto.getLongitude());
        if (dto.getTimezone() != null) {
            validateTimezone(dto.getTimezone());
            dp.setTimezone(dto.getTimezone());
        }

        if (dto.getVerified() != null) {
            boolean v = dto.getVerified();
            dp.setVerified(v);
            dp.setVerifiedAt(v ? Instant.now() : null);
        }

        dp = doctorProfileRepository.save(dp);
        return toResponse(dp);
    }

    @Transactional
    public DoctorProfileResponseDto updateLocation(UUID doctorProfileId, String addressText, Double lat, Double lon) {
        DoctorProfile dp = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable."));

        if (lat != null || lon != null) {
            if (!GeoUtils.isValidLatLon(lat, lon)) throw new BadRequestException("Coordonnées invalides.");
            dp.setLatitude(lat);
            dp.setLongitude(lon);
        }
        if (addressText != null) dp.setAddressText(addressText);

        dp = doctorProfileRepository.save(dp);
        return toResponse(dp);
    }

    @Transactional
    public DoctorProfileResponseDto setVerified(UUID doctorProfileId, boolean verified) {
        DoctorProfile dp = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable."));
        dp.setVerified(verified);
        dp.setVerifiedAt(verified ? Instant.now() : null);
        dp = doctorProfileRepository.save(dp);
        return toResponse(dp);
    }

    // ====== Recherche / filtrage simple (in-memory) ======
    // Pour du solide à grande échelle -> Specifications (plus tard).
    public List<DoctorProfileResponseDto> search(DoctorSearchFilter filter) {
        List<DoctorProfile> all = doctorProfileRepository.findAll();
        List<DoctorProfileResponseDto> out = new ArrayList<>();

        for (DoctorProfile dp : all) {
            if (filter != null) {
                if (filter.getDoctorType() != null && dp.getDoctorType() != filter.getDoctorType()) continue;
                if (filter.getVerifiedOnly() != null && filter.getVerifiedOnly() && !dp.isVerified()) continue;
                if (filter.getHasLocation() != null && filter.getHasLocation() && !dp.hasLocation()) continue;
                if (filter.getConsultationMode() != null && dp.getConsultationMode() != filter.getConsultationMode()) continue;
                if (filter.getMinFee() != null && dp.getConsultationFee() != null
                        && dp.getConsultationFee().doubleValue() < filter.getMinFee()) continue;
                if (filter.getMaxFee() != null && dp.getConsultationFee() != null
                        && dp.getConsultationFee().doubleValue() > filter.getMaxFee()) continue;
            }
            out.add(toResponse(dp));
        }
        return out;
    }

    // ====== Filter DTO (simple) ======
    public static class DoctorSearchFilter {
        private DoctorProfile.DoctorType doctorType;
        private Boolean verifiedOnly;
        private Boolean hasLocation;
        private DoctorProfile.ConsultationMode consultationMode;
        private Double minFee;
        private Double maxFee;

        public DoctorProfile.DoctorType getDoctorType() { return doctorType; }
        public void setDoctorType(DoctorProfile.DoctorType doctorType) { this.doctorType = doctorType; }

        public Boolean getVerifiedOnly() { return verifiedOnly; }
        public void setVerifiedOnly(Boolean verifiedOnly) { this.verifiedOnly = verifiedOnly; }

        public Boolean getHasLocation() { return hasLocation; }
        public void setHasLocation(Boolean hasLocation) { this.hasLocation = hasLocation; }

        public DoctorProfile.ConsultationMode getConsultationMode() { return consultationMode; }
        public void setConsultationMode(DoctorProfile.ConsultationMode consultationMode) { this.consultationMode = consultationMode; }

        public Double getMinFee() { return minFee; }
        public void setMinFee(Double minFee) { this.minFee = minFee; }

        public Double getMaxFee() { return maxFee; }
        public void setMaxFee(Double maxFee) { this.maxFee = maxFee; }
    }

    private DoctorProfileResponseDto toResponse(DoctorProfile dp) {
        DoctorProfileResponseDto dto = new DoctorProfileResponseDto();
        dto.setId(dp.getId());
        dto.setUserId(dp.getUser() != null ? dp.getUser().getId() : null);
        dto.setDoctorType(dp.getDoctorType());
        dto.setSpeciality(dp.getSpeciality());
        dto.setLicenseNumber(dp.getLicenseNumber());
        dto.setClinicName(dp.getClinicName());
        dto.setBio(dp.getBio());
        dto.setYearsOfExperience(dp.getYearsOfExperience());
        dto.setLanguages(dp.getLanguages());
        dto.setConsultationMode(dp.getConsultationMode());
        dto.setConsultationFee(dp.getConsultationFee());
        dto.setVerified(dp.isVerified());
        dto.setVerifiedAt(dp.getVerifiedAt());
        dto.setAddressText(dp.getAddressText());
        dto.setLatitude(dp.getLatitude());
        dto.setLongitude(dp.getLongitude());
        dto.setTimezone(dp.getTimezone());
        return dto;
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new BadRequestException("Timezone invalide.");
        }
    }
}
