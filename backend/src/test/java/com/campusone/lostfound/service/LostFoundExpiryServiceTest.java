package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostFoundExpiryServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-14T08:00:00Z");

    @Mock
    private LostFoundItemRepository itemRepository;

    private LostFoundExpiryService expiryService;
    private User user;
    private University university;

    @BeforeEach
    void setUp() {
        university = new University("CampusOne University", "COU", "Lahore");
        ReflectionTestUtils.setField(
                university,
                "id",
                UUID.fromString("40000000-0000-4000-8000-000000000001"));
        Department department = new Department(university, "CS", "CS");
        ReflectionTestUtils.setField(
                department,
                "id",
                UUID.fromString("40000000-0000-4000-8000-000000000002"));
        user = new User("student@example.com", "$2a$12$hash");
        ReflectionTestUtils.setField(
                user,
                "id",
                UUID.fromString("10000000-0000-4000-8000-000000000001"));
        user.setStudentProfile(new StudentProfile(
                user,
                university,
                department,
                "Student",
                4));
        expiryService = new LostFoundExpiryService(
                itemRepository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                100);
    }

    @Test
    void archiveExpiredBatch_archivesOnlyRepositorySelectedPublishedItems() {
        LostFoundItem expired = publishedItem(
                UUID.fromString("50000000-0000-4000-8000-000000000001"),
                NOW.minusSeconds(60));
        LostFoundItem stillPublished = publishedItem(
                UUID.fromString("50000000-0000-4000-8000-000000000002"),
                NOW.plusSeconds(60));
        when(itemRepository.findExpiredPublishedForUpdate(
                eq(LostFoundItemStatus.PUBLISHED),
                eq(NOW),
                any(Pageable.class)))
                .thenReturn(List.of(expired));

        int archived = expiryService.archiveExpiredBatch();

        assertThat(archived).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(LostFoundItemStatus.ARCHIVED);
        assertThat(stillPublished.getStatus()).isEqualTo(LostFoundItemStatus.PUBLISHED);
    }

    private LostFoundItem publishedItem(UUID id, Instant expiresAt) {
        LostFoundItem item = new LostFoundItem(
                user,
                university,
                LostFoundItemType.FOUND,
                LostFoundCategory.ELECTRONICS,
                "Black laptop bag",
                "A black laptop bag with a small front pocket.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "createdAt", NOW.minusSeconds(3600));
        ReflectionTestUtils.setField(item, "updatedAt", NOW.minusSeconds(3600));
        item.approve(user, NOW.minusSeconds(3600), expiresAt);
        return item;
    }
}
