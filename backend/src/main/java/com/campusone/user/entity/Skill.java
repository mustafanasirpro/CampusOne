package com.campusone.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 40)
    @Column(nullable = false, length = 40, updatable = false)
    private String name;

    @NotBlank
    @Size(min = 2, max = 40)
    @Column(name = "normalized_name", nullable = false, unique = true, length = 40, updatable = false)
    private String normalizedName;

    protected Skill() {
    }

    public Skill(String name) {
        this.name = name.trim();
        this.normalizedName = normalize(name);
    }

    public static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }
}
