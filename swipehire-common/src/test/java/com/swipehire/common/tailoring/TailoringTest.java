package com.swipehire.common.tailoring;

import com.swipehire.common.dto.JobData;
import com.swipehire.common.enums.ResumeItemType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TailoringTest {

    private static final long JAVA = 1, SPRING = 10, SQL = 9, FIGMA = 41, PYTHON = 2;

    private static JobData javaJob() {
        JobData j = new JobData();
        j.setId(1);
        j.setTitle("Java Backend Intern");
        j.setText("Build REST APIs with Java, Spring Boot and SQL");
        j.setSkillWeights(Map.of(JAVA, 5, SPRING, 4, SQL, 3));
        j.setRequiredSkillIds(Set.of(JAVA, SPRING));
        return j;
    }

    private static ResumeItemData item(long id, ResumeItemType type, String title, String desc, int order, Long... skills) {
        return new ResumeItemData(id, type, title, null, null, desc, Set.of(skills), order);
    }

    @Test
    void relevantProjectsMoveUpAndOnlyExistingItemsAreUsed() {
        List<ResumeItemData> items = List.of(
                item(1, ResumeItemType.PROJECT, "Poster designs", "Designed posters in Figma", 1, FIGMA),
                item(2, ResumeItemType.PROJECT, "Canteen API", "Spring Boot REST API in Java with SQL", 2, JAVA, SPRING, SQL),
                item(3, ResumeItemType.PROJECT, "Data scraper", "Python scraper", 3, PYTHON));

        TailoredResume t = new ResumeTailor().tailor(items, List.of(FIGMA, JAVA, PYTHON), javaJob());
        List<Long> order = t.allItems().stream().map(s -> s.item().getId()).toList();

        assertEquals(2L, order.get(0), "the Java project should come first");
        // Never invents content: every output item is one of the inputs.
        assertTrue(items.containsAll(t.allItems().stream().map(TailoredResume.ScoredItem::item).toList()));
    }

    @Test
    void sectionLimitsDropTheLeastRelevantItems() {
        List<ResumeItemData> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            items.add(item(i, ResumeItemType.PROJECT, "Project " + i, "misc", i));
        }
        items.add(item(99, ResumeItemType.PROJECT, "Java service", "Java Spring Boot SQL", 6, JAVA, SPRING, SQL));
        TailoredResume t = new ResumeTailor().tailor(items, List.of(), javaJob());
        List<Long> ids = t.allItems().stream().map(s -> s.item().getId()).toList();
        assertEquals(3, ids.size());
        assertEquals(99L, ids.get(0));
        // Ties keep the applicant's own order.
        assertEquals(List.of(99L, 1L, 2L), ids);
    }

    @Test
    void educationStaysFirstAndInOriginalOrder() {
        List<ResumeItemData> items = List.of(
                item(1, ResumeItemType.PROJECT, "Java API", "Java Spring", 1, JAVA, SPRING),
                item(2, ResumeItemType.EDUCATION, "B.Tech", "college", 2),
                item(3, ResumeItemType.EDUCATION, "Class XII", "school", 3));
        TailoredResume t = new ResumeTailor().tailor(items, List.of(), javaJob());
        assertEquals(ResumeItemType.EDUCATION, t.sections().get(0).type());
        assertEquals(List.of(2L, 3L), t.sections().get(0).items().stream().map(s -> s.item().getId()).toList());
    }

    @Test
    void skillsAreOrderedRequiredFirstThenOptionalThenOthers() {
        List<Long> ordered = ResumeTailor.orderSkills(List.of(FIGMA, SQL, PYTHON, SPRING, JAVA), javaJob());
        assertEquals(List.of(JAVA, SPRING, SQL, FIGMA, PYTHON), ordered);
        assertEquals(Set.of(JAVA, SPRING, SQL), ResumeTailor.highlightSkills(ordered, javaJob()));
    }

    @Test
    void skillDetectorMatchesWholeWordsAndSymbols() {
        SkillDetector d = new SkillDetector(Map.of(
                "Java", 1L, "JavaScript", 3L, "C++", 4L, "C", 5L, "Node.js", 12L, "nodejs", 12L, "k8s", 30L));
        Set<Long> found = d.detect("Skills: JavaScript, C++ and Node.js. Deployed on K8s.");
        assertEquals(Set.of(3L, 4L, 12L, 30L), found);   // not Java (only inside JavaScript), not C (single letter)
    }
}
