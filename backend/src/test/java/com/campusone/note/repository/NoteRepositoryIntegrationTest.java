package com.campusone.note.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.entity.Course;
import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.repository.CourseRepository;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.note.entity.FileAsset;
import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteBookmark;
import com.campusone.note.entity.NoteDownloadEvent;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteModerationAction;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteRating;
import com.campusone.note.entity.NoteVersion;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.StorageProvider;
import com.campusone.note.entity.Tag;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.security.servlet."
            + "UserDetailsServiceAutoConfiguration",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Transactional
class NoteRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileAssetRepository fileAssetRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private NoteVersionRepository noteVersionRepository;

    @Autowired
    private NoteRatingRepository noteRatingRepository;

    @Autowired
    private NoteBookmarkRepository noteBookmarkRepository;

    @Autowired
    private NoteDownloadEventRepository noteDownloadEventRepository;

    @Autowired
    private NoteModerationActionRepository noteModerationActionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void notesAggregate_v6Schema_persistsAndSupportsPublicQueries() {
        University university = universityRepository.save(
                new University(
                        "Notes Integration University",
                        "NIU",
                        "Islamabad"));
        Department department = departmentRepository.save(
                new Department(
                        university,
                        "Computer Science",
                        "CS"));
        Course course = courseRepository.save(
                new Course(
                        department,
                        "CS-201",
                        "Object Oriented Programming"));

        User user = new User(
                "notes.integration@example.com",
                "$2a$12$" + "a".repeat(53));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setStudentProfile(new StudentProfile(
                user,
                university,
                department,
                "Ayesha Malik",
                4));
        user = userRepository.saveAndFlush(user);

        FileAsset fileAsset = fileAssetRepository.save(new FileAsset(
                user,
                StorageProvider.MINIO,
                "campusone-notes",
                "notes/integration/oop.pdf",
                "oop-notes.pdf",
                "application/pdf",
                4096,
                "a".repeat(64),
                null));
        Tag tag = tagRepository.save(new Tag("Object Oriented Programming"));

        Note note = new Note(
                user,
                course,
                fileAsset,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC);
        note.replaceTags(List.of(tag));
        note.approve(user, Instant.now());
        note = noteRepository.saveAndFlush(note);

        noteVersionRepository.save(NoteVersion.initial(note));
        noteRatingRepository.save(new NoteRating(note, user, 5));
        note.applyRating(null, 5);
        noteBookmarkRepository.save(new NoteBookmark(note, user));
        noteDownloadEventRepository.save(new NoteDownloadEvent(
                note,
                user,
                "b".repeat(64),
                "c".repeat(64)));
        note.recordDownload();
        noteModerationActionRepository.save(
                NoteModerationAction.submitted(note, null));
        noteRepository.flush();
        entityManager.clear();

        Page<Note> publicNotes = noteRepository.findPublicNotes(
                NoteModerationStatus.APPROVED,
                NoteVisibility.PUBLIC,
                course.getId(),
                Tag.normalize("Object Oriented Programming"),
                PageRequest.of(0, 20));

        assertThat(publicNotes.getContent()).hasSize(1);
        Note stored = publicNotes.getContent().getFirst();
        assertThat(stored.getAverageRating())
                .isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(stored.getDownloadCount()).isEqualTo(1);
        assertThat(stored.getTags())
                .extracting(Tag::getNormalizedName)
                .containsExactly("object oriented programming");
    }
}
