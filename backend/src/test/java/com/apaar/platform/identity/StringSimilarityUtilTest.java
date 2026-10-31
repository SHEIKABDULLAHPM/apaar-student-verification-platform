package com.apaar.platform.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringSimilarityUtilTest {

    @Test
    @DisplayName("Identical names yield 1.0 similarity")
    void identicalNames() {
        double score = StringSimilarityUtil.nameSimilarity("Aarav Sharma", "Aarav Sharma");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Reversed order tokens yield high token similarity")
    void tokenSortSimilarity() {
        double score = StringSimilarityUtil.nameSimilarity("Sharma Aarav", "Aarav Sharma");
        assertThat(score).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    @DisplayName("Minor typos yield above threshold similarity")
    void minorTypo() {
        double score = StringSimilarityUtil.nameSimilarity("Aarav Sharma", "Arav Sharma");
        assertThat(score).isGreaterThan(0.85);
    }

    @Test
    @DisplayName("Completely different names yield low similarity")
    void differentNames() {
        double score = StringSimilarityUtil.nameSimilarity("Aarav Sharma", "Priya Nair");
        assertThat(score).isLessThan(0.4);
    }
}

