package com.campusone.search.repository;

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
import com.campusone.note.repository.FileAssetRepository;
import com.campusone.note.repository.NoteRepository;
import com.campusone.note.repository.TagRepository;
import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.service.SearchQueryNormalizer;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
class JdbcGlobalSearchRepositoryIntegrationTest {

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

    private JdbcGlobalSearchRepository searchRepository;
    private Note machineLearningNote;
    private Note descriptionOnlyNote;

    @BeforeEach
    void setUp() {
        searchRepository = new JdbcGlobalSearchRepository(
                new NamedParameterJdbcTemplate(dataSource),
                new SearchQueryNormalizer());
        seedNotes();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void search_findsNotesBySingleTitleWordsAndRanksTitleFirst() {
        SearchRepositoryResult result = searchNotes("machine learning");

        assertThat(result.documents())
                .extracting(SearchDocument::title)
                .contains(
                        "Machine Learning Course Notes",
                        "AI Notes");
        assertThat(result.documents().getFirst().id())
                .isEqualTo(machineLearningNote.getId());
    }

    @Test
    void search_supportsTokenOrderIndependenceAndAbbreviation() {
        assertThat(searchNotes("learning machine").documents().getFirst().id())
                .isEqualTo(machineLearningNote.getId());
        assertThat(searchNotes("ML").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
    }

    @Test
    void search_findsNoteByRichMetadataFields() {
        assertThat(searchNotes("learning").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("machine").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("sara").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("CSC275").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("neural").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("ml-final").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("Ayesha").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("Computer Science").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
    }

    @Test
    void search_isCaseInsensitiveAndReturnsNoResultsForUnrelatedQuery() {
        assertThat(searchNotes("MACHINE").documents())
                .extracting(SearchDocument::id)
                .contains(machineLearningNote.getId());
        assertThat(searchNotes("thermodynamics").documents()).isEmpty();
    }

    private SearchRepositoryResult searchNotes(String query) {
        return searchRepository.search(
                normalize(query),
                Set.of(SearchType.NOTE),
                0,
                10,
                SearchSort.RELEVANCE);
    }

    private String normalize(String query) {
        return query.trim()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void seedNotes() {
        University university = universityRepository.save(new University(
                "Search Integration University",
                "SIU",
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
                "search.integration@example.com",
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
                "campusone-search-test",
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
