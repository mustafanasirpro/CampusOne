package com.campusone.note.repository;

import com.campusone.note.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByNormalizedNameIn(Collection<String> normalizedNames);
}
