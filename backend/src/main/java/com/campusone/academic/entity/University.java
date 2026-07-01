package com.campusone.academic.entity;

import com.campusone.user.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "universities")
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(name = "short_name", nullable = false, length = 20)
    private String shortName;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @Size(max = 2048)
    @Column(length = 2048)
    private String website;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "university", fetch = FetchType.LAZY)
    private List<Department> departments = new ArrayList<>();

    @OneToMany(mappedBy = "university", fetch = FetchType.LAZY)
    private List<StudentProfile> studentProfiles = new ArrayList<>();

    protected University() {
    }

    public University(String name, String shortName, String city) {
        this.name = name;
        this.shortName = shortName;
        this.city = city;
    }

    public void addDepartment(Department department) {
        departments.add(department);
        if (department.getUniversity() != this) {
            department.setUniversity(this);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public List<StudentProfile> getStudentProfiles() {
        return studentProfiles;
    }
}
