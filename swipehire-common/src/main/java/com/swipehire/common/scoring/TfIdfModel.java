package com.swipehire.common.scoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Classic TF-IDF with cosine similarity.
 * <ul>
 *   <li>tf weight = 1 + ln(count): sub-linear, so 10 mentions is not 10x more important</li>
 *   <li>idf = ln((N + 1) / (df + 1)) + 1: rare terms matter more; the +1s avoid division by zero</li>
 *   <li>cosine = (a . b) / (|a| |b|): 0 = nothing in common, 1 = same direction</li>
 * </ul>
 * IDF is computed over the documents being compared in one ranking call.
 */
public final class TfIdfModel {

    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final int documentCount;

    public TfIdfModel(Collection<Map<String, Integer>> documents) {
        this.documentCount = documents.size();
        for (Map<String, Integer> doc : documents) {
            for (String term : doc.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
    }

    public double idf(String term) {
        int df = documentFrequency.getOrDefault(term, 0);
        return Math.log((documentCount + 1.0) / (df + 1.0)) + 1.0;
    }

    public Map<String, Double> vectorize(Map<String, Integer> tf) {
        Map<String, Double> v = new HashMap<>();
        tf.forEach((term, count) -> v.put(term, (1 + Math.log(count)) * idf(term)));
        return v;
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        // Iterate over the smaller vector for the dot product.
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(x -> x * x).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(x -> x * x).sum());
        return (normA == 0 || normB == 0) ? 0 : dot / (normA * normB);
    }

    /** Similarity of two raw term-frequency maps under this model. */
    public double similarity(Map<String, Integer> tfA, Map<String, Integer> tfB) {
        return cosine(vectorize(tfA), vectorize(tfB));
    }
}
