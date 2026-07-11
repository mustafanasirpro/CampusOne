package com.campusone.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchQueryNormalizerTest {

    private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

    @Test
    void normalize_trimsCasePunctuationAndDuplicateSpaces() {
        assertThat(normalizer.normalize("  Machine---   Learning!!!  "))
                .isEqualTo("machine learning");
        assertThat(normalizer.display("  Machine---   Learning!!!  "))
                .isEqualTo("Machine Learning");
    }

    @Test
    void patterns_escapeLikeWildcards() {
        assertThat(normalizer.likePattern("cs_100%"))
                .isEqualTo("%cs\\_100\\%%");
        assertThat(normalizer.prefixPattern("cs_100%"))
                .isEqualTo("cs\\_100\\%%");
        assertThat(normalizer.wholeWordPattern("cs_100%"))
                .isEqualTo("% cs\\_100\\% %");
    }

    @Test
    void compact_removesNormalizedSpacesForCourseCodesAndInitials() {
        assertThat(normalizer.compact("csc 275"))
                .isEqualTo("csc275");
    }
}
