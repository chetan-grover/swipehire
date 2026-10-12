package com.swipehire.common.tailoring;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds dictionary skills in free text (used after PDFBox extracts text from an uploaded resume).
 * Matching is whole-word and case-insensitive, and works for names with symbols like "C++" or
 * "Node.js". Single-letter names (like "C") are skipped because they match far too often.
 * Results are only SUGGESTIONS: the applicant confirms them before anything is saved.
 */
public final class SkillDetector {

    private final Map<Pattern, Long> patterns = new LinkedHashMap<>();

    /** @param termToSkillId skill names and synonyms (any case) -> skill id */
    public SkillDetector(Map<String, Long> termToSkillId) {
        termToSkillId.forEach((term, skillId) -> {
            String t = term.trim().toLowerCase(Locale.ROOT);
            if (t.length() >= 2) {
                // "Not preceded/followed by a letter or digit" works even when the term ends in '+' or '.'.
                patterns.put(Pattern.compile("(?<![a-z0-9])" + Pattern.quote(t) + "(?![a-z0-9+#])"), skillId);
            }
        });
    }

    public Set<Long> detect(String text) {
        Set<Long> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        patterns.forEach((pattern, skillId) -> {
            if (pattern.matcher(lower).find()) {
                found.add(skillId);
            }
        });
        return found;
    }
}
