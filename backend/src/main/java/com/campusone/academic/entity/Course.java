package com.campusone.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @NotBlank
    @Size(min = 2, max = 30)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._ -]*$")
    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @NotBlank
    @Size(min = 2, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @Min(1)
    @Max(8)
    @Column(name = "recommended_semester")
    private Integer recommendedSemester;

    @Column(nullable = false)
    private boolean active = true;

    protected Course() {
    }

    public Course(Department department, String courseCode, String title) {
        this.department = department;
        this.courseCode = courseCode;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getRecommendedSemester() {
        return recommendedSemester;
    }

    public void setRecommendedSemester(Integer recommendedSemester) {
        this.recommendedSemester = recommendedSemester;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
