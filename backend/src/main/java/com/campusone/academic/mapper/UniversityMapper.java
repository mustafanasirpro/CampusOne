package com.campusone.academic.mapper;

import com.campusone.academic.dto.response.UniversityResponse;
import com.campusone.academic.entity.University;
import org.springframework.stereotype.Component;

@Component
public class UniversityMapper {

    public UniversityResponse toResponse(University university) {
        return new UniversityResponse(
                university.getId(),
                university.getName(),
                university.getShortName(),
                university.getCity(),
                university.getWebsite(),
                university.isActive());
    }
}
