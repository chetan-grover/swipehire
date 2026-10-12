package com.swipehire.common.tailoring;

import com.swipehire.common.dto.JobData;
import com.swipehire.common.enums.ResumeItemType;
import com.swipehire.common.scoring.TextAnalyzer;
import com.swipehire.common.scoring.TfIdfModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tailors a resume for one job by SELECTING and REORDERING the applicant's existing items,
 * never by writing new text. Item relevance:
 * <pre>
 * relevance = 0.7 * (weight of the job's skills tagged on this item / total job skill weight)
 *           + 0.3 * TF-IDF cosine(item text, job description)
 * </pre>
 */
public final class ResumeTailor {

    static final double W_SKILLS = 0.7;
    static final double W_TEXT = 0.3;

    /** Max items kept per section, so the PDF stays one page and focused. */
    static final Map<ResumeItemType, Integer> SECTION_LIMITS = new EnumMap<>(Map.of(
            ResumeItemType.EDUCATION, 2,
            ResumeItemType.EXPERIENCE, 3,
            ResumeItemType.PROJECT, 3,
            ResumeItemType.ACHIEVEMENT, 3,
            ResumeItemType.CERTIFICATION, 2));

    public TailoredResume tailor(List<ResumeItemData> items, List<Long> applicantSkillIds, JobData job) {
        Map<Long, Double> relevance = relevance(items, job);

        // Group by section; inside a section order by relevance, ties keep the applicant's order.
        Map<ResumeItemType, List<ResumeItemData>> bySection = new EnumMap<>(ResumeItemType.class);
        for (ResumeItemData item : items) {
            bySection.computeIfAbsent(item.getType(), t -> new ArrayList<>()).add(item);
        }
        List<TailoredResume.Section> sections = new ArrayList<>();
        for (Map.Entry<ResumeItemType, List<ResumeItemData>> e : bySection.entrySet()) {
            Comparator<ResumeItemData> order = e.getKey() == ResumeItemType.EDUCATION
                    // Education stays in the applicant's (chronological) order.
                    ? Comparator.comparingInt(ResumeItemData::getDisplayOrder)
                    : Comparator.comparingDouble((ResumeItemData i) -> relevance.get(i.getId())).reversed()
                    .thenComparingInt(ResumeItemData::getDisplayOrder);
            List<TailoredResume.ScoredItem> chosen = e.getValue().stream()
                    .sorted(order)
                    .limit(SECTION_LIMITS.get(e.getKey()))
                    .map(i -> new TailoredResume.ScoredItem(i, relevance.get(i.getId())))
                    .toList();
            sections.add(new TailoredResume.Section(e.getKey(), chosen));
        }
        // Education first, then the section with the most relevant item first.
        sections.sort(Comparator.comparing((TailoredResume.Section s) -> s.type() != ResumeItemType.EDUCATION)
                .thenComparing(Comparator.comparingDouble(ResumeTailor::bestRelevance).reversed()));

        return new TailoredResume(sections, orderSkills(applicantSkillIds, job), highlightSkills(applicantSkillIds, job));
    }

    Map<Long, Double> relevance(List<ResumeItemData> items, JobData job) {
        Map<String, Integer> jobTf = TextAnalyzer.termFrequencies(job.getTitle() + " " + job.getText());
        Map<Long, Map<String, Integer>> itemTfs = new LinkedHashMap<>();
        for (ResumeItemData item : items) {
            itemTfs.put(item.getId(), TextAnalyzer.termFrequencies(item.getTitle() + " " + item.getDescription()));
        }
        List<Map<String, Integer>> corpus = new ArrayList<>(itemTfs.values());
        corpus.add(jobTf);
        TfIdfModel model = new TfIdfModel(corpus);

        int totalWeight = job.getSkillWeights().values().stream().mapToInt(Integer::intValue).sum();
        Map<Long, Double> result = new LinkedHashMap<>();
        for (ResumeItemData item : items) {
            int covered = 0;
            for (Long skillId : item.getSkillIds()) {
                covered += job.getSkillWeights().getOrDefault(skillId, 0);
            }
            double skillPart = totalWeight == 0 ? 0 : covered / (double) totalWeight;
            double textPart = model.similarity(itemTfs.get(item.getId()), jobTf);
            result.put(item.getId(), W_SKILLS * skillPart + W_TEXT * textPart);
        }
        return result;
    }

    /** Required matched skills first, then other matched skills (heaviest first), then the rest unchanged. */
    static List<Long> orderSkills(List<Long> applicantSkillIds, JobData job) {
        Map<Long, Integer> w = job.getSkillWeights();
        Comparator<Long> rank = Comparator
                .comparingInt((Long id) -> !w.containsKey(id) ? 2 : job.getRequiredSkillIds().contains(id) ? 0 : 1)
                .thenComparing(id -> -w.getOrDefault(id, 0));
        // List.sort is stable, so skills with equal rank keep the applicant's order.
        List<Long> ordered = new ArrayList<>(applicantSkillIds);
        ordered.sort(rank);
        return ordered;
    }

    static Set<Long> highlightSkills(List<Long> applicantSkillIds, JobData job) {
        Set<Long> s = new HashSet<>(applicantSkillIds);
        s.retainAll(job.getSkillWeights().keySet());
        return s;
    }

    private static double bestRelevance(TailoredResume.Section s) {
        return s.items().stream().mapToDouble(TailoredResume.ScoredItem::relevance).max().orElse(0);
    }
}
