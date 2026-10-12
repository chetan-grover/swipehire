package com.swipehire.common.tailoring;

import com.swipehire.common.enums.ResumeItemType;

import java.util.List;
import java.util.Set;

/**
 * Output of tailoring: the applicant's own items, selected and reordered for one job.
 * Nothing in here is new text; every item points back to an existing resume_item row.
 */
public record TailoredResume(List<Section> sections, List<Long> orderedSkillIds, Set<Long> highlightSkillIds) {

    public record Section(ResumeItemType type, List<ScoredItem> items) {
    }

    public record ScoredItem(ResumeItemData item, double relevance) {
    }

    /** All chosen items in final order, for storing in tailored_resume_item. */
    public List<ScoredItem> allItems() {
        return sections.stream().flatMap(s -> s.items().stream()).toList();
    }
}
