package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.entity.LostFoundMatch;
import com.campusone.lostfound.entity.LostFoundMatchStatus;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.repository.LostFoundMatchRepository;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostFoundMatchingServiceTest {

    private static final UUID UNIVERSITY_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final UUID LOST_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID FOUND_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000002");
    private static final Instant NOW =
            Instant.parse("2026-07-14T08:00:00Z");

    @Mock
    private LostFoundItemRepository itemRepository;

    @Mock
    private LostFoundMatchRepository matchRepository;

    private LostFoundMatchingService matchingService;
    private University university;
    private User user;

    @BeforeEach
    void setUp() {
        university = new University(
                "CampusOne University",
                "COU",
                "Lahore");
        ReflectionTestUtils.setField(university, "id", UNIVERSITY_ID);
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
        matchingService = new LostFoundMatchingService(
                itemRepository,
                matchRepository,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void suggestMatchesFor_strongMatchCreatesExplainableSuggestion() {
        LostFoundItem lost = approvedItem(
                LOST_ID,
                LostFoundItemType.LOST,
                "Black Dell laptop bag",
                "A black laptop bag with a blue zipper keychain.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        LostFoundItem found = approvedItem(
                FOUND_ID,
                LostFoundItemType.FOUND,
                "black dell laptop bag",
                "Found black laptop bag with blue zipper keychain.",
                "Library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        when(itemRepository.findMatchCandidates(
                eq(LOST_ID),
                eq(UNIVERSITY_ID),
                eq(LostFoundItemType.FOUND),
                eq(LostFoundItemStatus.PUBLISHED),
                eq(NOW),
                eq(LocalDate.of(2026, 6, 13)),
                eq(LocalDate.of(2026, 8, 12)),
                any(Pageable.class)))
                .thenReturn(List.of(found));
        when(matchRepository.findByLostItemIdAndFoundItemId(LOST_ID, FOUND_ID))
                .thenReturn(Optional.empty());

        matchingService.suggestMatchesFor(lost);

        ArgumentCaptor<LostFoundMatch> captor =
                ArgumentCaptor.forClass(LostFoundMatch.class);
        verify(matchRepository).save(captor.capture());
        LostFoundMatch match = captor.getValue();
        assertThat(match.getScore()).isGreaterThanOrEqualTo(90);
        assertThat(match.getStatus()).isEqualTo(LostFoundMatchStatus.SUGGESTED);
        assertThat(match.getReasons().toString())
                .contains("Title similarity", "Same category", "Close item date");
    }

    @Test
    void suggestMatchesFor_weakCandidateDoesNotCreateMatch() {
        LostFoundItem lost = approvedItem(
                LOST_ID,
                LostFoundItemType.LOST,
                "Red water bottle",
                "A red bottle missing from cafeteria.",
                "Cafeteria",
                LocalDate.of(2026, 7, 13),
                null,
                "Red");
        LostFoundItem found = approvedItem(
                FOUND_ID,
                LostFoundItemType.FOUND,
                "Silver laptop charger",
                "USB-C charger found inside lab.",
                "Engineering lab",
                LocalDate.of(2026, 7, 20),
                "Anker",
                "Silver");
        when(itemRepository.findMatchCandidates(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(List.of(found));

        matchingService.suggestMatchesFor(lost);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void suggestMatchesFor_rejectedMatchIsNotOverwrittenByRecalculation() {
        LostFoundItem lost = approvedItem(
                LOST_ID,
                LostFoundItemType.LOST,
                "Black Dell laptop bag",
                "A black laptop bag with a blue zipper keychain.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        LostFoundItem found = approvedItem(
                FOUND_ID,
                LostFoundItemType.FOUND,
                "Black Dell laptop bag",
                "Found black laptop bag with blue zipper keychain.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        LostFoundMatch rejected = new LostFoundMatch(
                lost,
                found,
                100,
                new ObjectMapper().createArrayNode());
        rejected.reject(user, NOW);
        when(itemRepository.findMatchCandidates(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(List.of(found));
        when(matchRepository.findByLostItemIdAndFoundItemId(LOST_ID, FOUND_ID))
                .thenReturn(Optional.of(rejected));

        matchingService.suggestMatchesFor(lost);

        verify(matchRepository, never()).save(any());
        assertThat(rejected.getStatus()).isEqualTo(LostFoundMatchStatus.REJECTED);
    }

    private LostFoundItem approvedItem(
            UUID id,
            LostFoundItemType type,
            String title,
            String description,
            String location,
            LocalDate itemDate,
            String brand,
            String color) {
        LostFoundItem item = new LostFoundItem(
                user,
                university,
                type,
                LostFoundCategory.ELECTRONICS,
                title,
                description,
                location,
                itemDate,
                brand,
                color);
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "createdAt", NOW);
        ReflectionTestUtils.setField(item, "updatedAt", NOW);
        item.approve(user, NOW, NOW.plusSeconds(90L * 24 * 60 * 60));
        return item;
    }
}
