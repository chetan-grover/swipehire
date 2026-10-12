package com.swipehire.web.service;

import com.swipehire.web.entity.Skill;
import com.swipehire.web.entity.SkillSynonym;
import com.swipehire.web.repository.SkillRepository;
import com.swipehire.web.repository.SkillSynonymRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory copy of the skill dictionary (45 rows, read on every card). Reloaded when the admin
 * edits skills. The volatile field makes a freshly loaded map visible to all threads at once.
 */
@Service
public class SkillCatalog {

    private final SkillRepository skills;
    private final SkillSynonymRepository synonyms;
    private volatile List<Skill> ordered;
    private volatile Map<Long, Skill> byId;

    public SkillCatalog(SkillRepository skills, SkillSynonymRepository synonyms) {
        this.skills = skills;
        this.synonyms = synonyms;
    }

    public synchronized void reload() {
        List<Skill> all = skills.findAllByOrderByCategoryAscNameAsc();
        Map<Long, Skill> map = new HashMap<>();
        all.forEach(s -> map.put(s.getId(), s));
        ordered = all;
        byId = map;
    }

    private void ensureLoaded() {
        if (byId == null) {
            reload();
        }
    }

    public List<Skill> all() {
        ensureLoaded();
        return ordered;
    }

    /** category -> skills, for grouped checkboxes in forms. */
    public Map<String, List<Skill>> byCategory() {
        Map<String, List<Skill>> m = new LinkedHashMap<>();
        all().forEach(s -> m.computeIfAbsent(s.getCategory(), c -> new java.util.ArrayList<>()).add(s));
        return m;
    }

    public Skill get(Long id) {
        ensureLoaded();
        return byId.get(id);
    }

    public String name(Long id) {
        Skill s = get(id);
        return s == null ? "?" : s.getName();
    }

    public List<String> names(Collection<Long> ids) {
        return ids.stream().map(this::name).toList();
    }

    /** Every name and synonym -> skill id, for the PDF skill detector. */
    public Map<String, Long> detectionTerms() {
        Map<String, Long> terms = new HashMap<>();
        all().forEach(s -> terms.put(s.getName(), s.getId()));
        for (SkillSynonym syn : synonyms.findAll()) {
            terms.put(syn.getAlias(), syn.getSkill().getId());
        }
        return terms;
    }
}
