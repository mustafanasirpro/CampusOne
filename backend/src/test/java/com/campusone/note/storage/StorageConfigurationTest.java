package com.campusone.note.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusone.common.exception.StorageNotConfiguredException;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StorageConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(Clock.class, Clock::systemUTC)
                    .withUserConfiguration(StorageConfiguration.class);

    @Test
    void missingR2Environment_startsWithDisabledStorage() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            StorageService storageService = context.getBean(StorageService.class);
            assertThat(storageService).isInstanceOf(DisabledStorageService.class);
            assertThatThrownBy(() -> storageService.upload(
                    UUID.randomUUID(),
                    new ValidatedNoteFile(
                            "notes.pdf",
                            "application/pdf",
                            "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                            "a".repeat(64))))
                    .isInstanceOf(StorageNotConfiguredException.class)
                    .hasMessage("Storage is temporarily unavailable.");
        });
    }

    @Test
    void incompleteR2Environment_doesNotBreakStartup() {
        contextRunner
                .withPropertyValues(
                        "app.storage.provider=r2",
                        "app.storage.r2.endpoint=https://example.r2.cloudflarestorage.com")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StorageService.class))
                            .isInstanceOf(DisabledStorageService.class);
                });
    }
}
