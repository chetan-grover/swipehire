package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Small builders so each test states only what matters to it. */
final class Fixtures {

    static final long JAVA = 1, SPRING = 10, SQL = 9, GIT = 26, REACT = 20;

    private Fixtures() {
    }

    static ApplicantData applicant(long id, Map<Long, Integer> skills) {
        ApplicantData a = new ApplicantData();
        a.setId(id);
        a.setSkillProficiency(new HashMap<>(skills));
        a.setText("java spring boot backend developer rest api mysql");
        a.setPreferredCities(new HashSet<>(Set.of("bengaluru")));
        a.setPrefWorkMode(WorkMode.ANY);
        a.setPrefJobType(JobType.ANY);
        a.setGradYear(2027);
        return a;
    }

    /** Java backend job: Java(5, req), Spring(4, req), SQL(3, req), Git(2, optional). */
    static JobData javaJob(long id) {
        JobData j = new JobData();
        j.setId(id);
        j.setTitle("Java Backend Intern");
        j.setText("Build REST APIs in Java and Spring Boot with SQL databases");
        j.setSkillWeights(new HashMap<>(Map.of(JAVA, 5, SPRING, 4, SQL, 3, GIT, 2)));
        j.setRequiredSkillIds(new HashSet<>(Set.of(JAVA, SPRING, SQL)));
        j.setJobType(JobType.INTERNSHIP);
        j.setWorkMode(WorkMode.ONSITE);
        j.setCity("Bengaluru");
        j.setStipendMin(20000);
        j.setStipendMax(30000);
        return j;
    }
}
