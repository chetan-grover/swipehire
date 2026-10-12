package com.swipehire.common.scoring;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Turns free text into a bag of terms using Lucene's EnglishAnalyzer: lower-casing, stop-word
 * removal ("the", "and") and Porter stemming ("developing" and "developer" -> "develop").
 * Lucene analyzers are thread-safe, so one shared instance is enough.
 */
public final class TextAnalyzer {

    private static final Analyzer ANALYZER = new EnglishAnalyzer();

    private TextAnalyzer() {
    }

    /** term -> how many times it occurs. */
    public static Map<String, Integer> termFrequencies(String text) {
        Map<String, Integer> tf = new HashMap<>();
        if (text == null || text.isBlank()) {
            return tf;
        }
        try (TokenStream ts = ANALYZER.tokenStream("body", text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                tf.merge(term.toString(), 1, Integer::sum);
            }
            ts.end();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return tf;
    }
}
