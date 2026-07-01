package com.campusone.user.repository;

import com.campusone.user.entity.Skill;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    List<Skill> findAllByNormalizedNameIn(Collection<String> normalizedNames);
}
