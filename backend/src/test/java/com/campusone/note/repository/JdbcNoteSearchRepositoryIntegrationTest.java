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
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.StorageProvider;
import com.campusone.note.entity.Tag;
import com.campusone.search.service.SearchQueryNormalizer;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
class JdbcNoteSearchRepositoryIntegrationTest {

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
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

    private JdbcNoteSearchRepository searchRepository;
    private Note machineLearningNote;
    private Note descriptionOnlyNote;

    @BeforeEach
    void setUp() {
        searchRepository = new JdbcNoteSearchRepository(
                new NamedParameterJdbcTemplate(dataSource),
                normalizer);
        seedNotes();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void search_findsNotesBySingleTitleWordsAndRanksTitleFirst() {
        NoteSearchResult result = search("machine learning");

        assertThat(result.noteIds())
                .contains(machineLearningNote.getId(), descriptionOnlyNote.getId());
        assertThat(result.noteIds().getFirst())
                .isEqualTo(machineLearningNote.getId());
    }

    @Test
    void search_supportsTokenOrderAbbreviationAndTypoTolerance() {
        assertThat(search("learning machine").noteIds().getFirst())
                .isEqualTo(machineLearningNote.getId());
        assertThat(search("ML").noteIds())
                .contains(machineLearningNote.getId());
        assertThat(search("Machin Learning").noteIds())
                .contains(machineLearningNote.getId());
    }

    @Test
    void search_findsNotesByRichMetadataFields() {
        assertThat(search("learning").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("machine").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("sara").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("CSC275").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("neural").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("ml-final").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("Ayesha").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("Computer Science").noteIds()).contains(machineLearningNote.getId());
        assertThat(search("semester 6").noteIds()).contains(machineLearningNote.getId());
    }

    @Test
    void search_appliesCourseAndTagFiltersWithoutLosingRelevanceRanking() {
        NoteSearchResult result = searchRepository.searchPublicNotes(
                normalizer.normalize("machine"),
                null,
                normalizer.normalize("CSC275"),
                "neural networks",
                0,
                10);

        assertThat(result.noteIds()).containsExactly(machineLearningNote.getId());
    }

    @Test
    void search_isCaseInsensitiveWhitespaceTolerantAndReturnsNoResults() {
        assertThat(search("  MACHINE   LEARNING  ").noteIds().getFirst())
                .isEqualTo(machineLearningNote.getId());
        assertThat(search("thermodynamics").noteIds()).isEmpty();
    }

    private NoteSearchResult search(String query) {
        return searchRepository.searchPublicNotes(
                normalizer.normalize(query),
                null,
                null,
                null,
                0,
                10);
    }

    private void seedNotes() {
        University university = universityRepository.save(new University(
                "Note Search Integration University",
                "NSIU",
                "Islamabad"));
        Department department = departmentRepository.save(new Department(
                university,
                "Computer Science",
                "CS"));
        Course course = courseRepository.save(new Course(
                department,
                "CSC275",
                "Machine Learning"));

        User uploader = new User(
                "note.search.integration@example.com",
                "$2a$12$" + "a".repeat(53));
        uploader.setAccountStatus(AccountStatus.ACTIVE);
        uploader.setStudentProfile(new StudentProfile(
                uploader,
                university,
                department,
                "Ayesha Searcher",
                6));
        uploader = userRepository.saveAndFlush(uploader);

        Tag neuralTag = tagRepository.save(new Tag("Neural Networks"));
        machineLearningNote = approvedNote(
                uploader,
                course,
                "Machine Learning Course Notes",
                "Supervised learning, model evaluation, and feature engineering.",
                "Dr. Sara Noor",
                "notes/search/ml-final.pdf",
                "ml-final.pdf",
                List.of(neuralTag));

        Tag aiTag = tagRepository.save(new Tag("Artificial Intelligence"));
        descriptionOnlyNote = approvedNote(
                uploader,
                course,
                "AI Notes",
                "These notes mention machine learning in the description only.",
                "Dr. Bilal Khan",
                "notes/search/ai.pdf",
                "ai.pdf",
                List.of(aiTag));
    }

    private Note approvedNote(
            User uploader,
            Course course,
            String title,
            String description,
            String teacher,
            String objectKey,
            String filename,
            List<Tag> tags) {
        FileAsset fileAsset = fileAssetRepository.save(new FileAsset(
                uploader,
                StorageProvider.S3_COMPATIBLE,
                "campusone-note-search-test",
                objectKey,
                filename,
                "application/pdf",
                4096,
                "a".repeat(64),
                null));
        Note note = new Note(
                uploader,
                course,
                fileAsset,
                title,
                description,
                teacher,
                6,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC);
        note.replaceTags(tags);
        note.approve(uploader, Instant.now());
        return noteRepository.saveAndFlush(note);
    }
}
