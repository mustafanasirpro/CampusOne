package com.campusone.academic.entity;

import com.campusone.user.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<StudentProfile> studentProfiles = new ArrayList<>();

    protected Department() {
    }

    public Department(University university, String name, String code) {
        this.university = university;
        this.name = name;
        this.code = code;
    }

    public void addCourse(Course course) {
        courses.add(course);
        if (course.getDepartment() != this) {
            course.setDepartment(this);
        }
    }

    public UUID getId() {
        return id;
    }

    public University getUniversity() {
        return university;
    }

    public void setUniversity(University university) {
        this.university = university;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<StudentProfile> getStudentProfiles() {
        return studentProfiles;
    }
}
