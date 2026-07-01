package com.campusone.academic.mapper;

import com.campusone.academic.dto.response.DepartmentResponse;
import com.campusone.academic.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getUniversity().getId(),
                department.getName(),
                department.getCode(),
                department.isActive());
    }
}
