package com.campusone.internship.repository;

import com.campusone.internship.entity.SavedInternship;
import com.campusone.internship.entity.SavedInternshipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedInternshipRepository
        extends JpaRepository<SavedInternship, SavedInternshipId> {

    @Query("""
            select saved.id.internshipId
            from SavedInternship saved
            where saved.id.userId = :userId
              and saved.id.internshipId in :internshipIds
            """)
    List<UUID> findSavedInternshipIds(
            @Param("userId") UUID userId,
            @Param("internshipIds") List<UUID> internshipIds);
}
