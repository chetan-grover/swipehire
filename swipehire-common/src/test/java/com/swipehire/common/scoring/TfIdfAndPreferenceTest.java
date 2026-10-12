package com.swipehire.common.scoring;

import com.swipehire.common.enums.SwipeDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TfIdfAndPreferenceTest {

    @Test
    void analyzerStemsAndDropsStopWords() {
        Map<String, Integer> tf = TextAnalyzer.termFrequencies("The developers are developing APIs");
        assertFalse(tf.containsKey("the"));
        assertTrue(tf.containsKey("develop"), tf.toString());
        assertEquals(2, tf.get("develop"));
    }

    @Test
    void cosineIsOneForIdenticalAndZeroForDisjointText() {
        var a = TextAnalyzer.termFrequencies("java spring backend");
        var b = TextAnalyzer.termFrequencies("figma illustration poster");
        TfIdfModel model = new TfIdfModel(List.of(a, b));
        assertEquals(1.0, model.similarity(a, a), 1e-9);
        assertEquals(0.0, model.similarity(a, b), 1e-9);
    }

    @Test
    void rareTermsGetHigherIdf() {
        var d1 = TextAnalyzer.termFrequencies("java kotlin");
        var d2 = TextAnalyzer.termFrequencies("java python");
        var d3 = TextAnalyzer.termFrequencies("java golang");
        TfIdfModel model = new TfIdfModel(List.of(d1, d2, d3));
        assertTrue(model.idf("kotlin") > model.idf("java"));
    }

    @Test
    void preferenceNudgesAreClampedAndUndoable() {
        Map<Long, Double> afterLike = PreferenceUpdater.apply(Map.of(1L, 0.95), List.of(1L, 2L), SwipeDirection.SUPER, false);
        assertEquals(1.0, afterLike.get(1L), 1e-9);    // clamped at 1
        assertEquals(0.2, afterLike.get(2L), 1e-9);
        Map<Long, Double> undone = PreferenceUpdater.apply(afterLike, List.of(2L), SwipeDirection.SUPER, true);
        assertEquals(0.0, undone.get(2L), 1e-9);
        Map<Long, Double> pass = PreferenceUpdater.apply(Map.of(), List.of(3L), SwipeDirection.LEFT, false);
        assertEquals(-0.05, pass.get(3L), 1e-9);
    }
}
