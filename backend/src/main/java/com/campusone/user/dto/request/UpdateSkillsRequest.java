package com.campusone.user.dto.request;

import com.campusone.user.entity.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UpdateSkillsRequest(
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(min = 2, max = 40) String> skills) {

    public UpdateSkillsRequest {
        if (skills != null) {
            Map<String, String> uniqueSkills = new LinkedHashMap<>();
            for (String skill : skills) {
                if (skill == null || skill.isBlank()) {
                    continue;
                }
                String trimmed = skill.trim();
                uniqueSkills.putIfAbsent(Skill.normalize(trimmed), trimmed);
            }
            skills = List.copyOf(uniqueSkills.values());
        }
    }
}
