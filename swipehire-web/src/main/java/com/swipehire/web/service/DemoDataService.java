package com.swipehire.web.service;

import com.swipehire.web.config.AppTime;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates realistic demo data (applicants, jobs, swipes) so queues and charts look alive in a
 * demo. Everything is written with JDBC batches: one PreparedStatement, many addBatch() calls,
 * one executeBatch(), which is far faster than one round trip per row. A fixed random seed makes
 * the data the same every time.
 */
@Service
public class DemoDataService {

    private record Track(String name, String headline, String[] skills, String[] jobTitles, String jobText, String[] projects) {
    }

    private static final Track[] TRACKS = {
            new Track("backend", "Backend developer (Java/Spring)",
                    new String[]{"Java", "Spring Boot", "SQL", "MySQL", "REST APIs", "Git", "Hibernate", "OOP", "Data Structures"},
                    new String[]{"Backend Developer Intern", "Java Developer Intern", "Software Engineer Trainee"},
                    "Develop REST APIs with Java and Spring Boot, model data in MySQL and write clean, tested code.",
                    new String[]{"Hostel Complaint Portal|Spring Boot REST API with MySQL and JWT login",
                            "Bank Simulator|Java OOP project with JDBC persistence", "Event Booking API|REST API in Spring Boot with Hibernate"}),
            new Track("frontend", "Frontend developer (React)",
                    new String[]{"JavaScript", "React", "HTML", "CSS", "TypeScript", "Git", "Figma"},
                    new String[]{"Frontend Intern", "React Developer Intern", "UI Engineer Intern"},
                    "Build responsive React interfaces in JavaScript and TypeScript from Figma designs.",
                    new String[]{"Weather Dashboard|React app consuming a public REST API",
                            "College Fest Website|Responsive HTML and CSS site", "Expense Splitter|TypeScript React app"}),
            new Track("data", "Aspiring data scientist",
                    new String[]{"Python", "Pandas", "SQL", "Machine Learning", "Data Analysis", "Excel", "Power BI"},
                    new String[]{"Data Analyst Intern", "ML Intern", "Business Analyst Intern"},
                    "Analyse data with SQL and Python Pandas, build dashboards and simple machine learning models.",
                    new String[]{"Movie Recommender|Machine learning recommender in Python",
                            "Sales Dashboard|Power BI report on SQL data", "Churn Prediction|Pandas and scikit-style model"}),
            new Track("devops", "Cloud & DevOps enthusiast",
                    new String[]{"Linux", "Docker", "AWS", "Kubernetes", "Git", "Python"},
                    new String[]{"DevOps Intern", "Cloud Intern", "Site Reliability Intern"},
                    "Automate deployments on Linux with Docker, manage AWS resources and Kubernetes clusters.",
                    new String[]{"CI Pipeline|Dockerised app deployed to AWS", "Home Lab|Kubernetes cluster on Linux VMs"}),
            new Track("mobile", "Android developer",
                    new String[]{"Kotlin", "Android", "Java", "REST APIs", "Git"},
                    new String[]{"Android Developer Intern", "Mobile Engineer Intern"},
                    "Build Android features in Kotlin and integrate REST APIs.",
                    new String[]{"Attendance App|Kotlin Android app with REST backend", "Notes App|Android app with offline sync"}),
            new Track("design", "Product designer",
                    new String[]{"Figma", "UI/UX Design", "HTML", "CSS", "Communication"},
                    new String[]{"UI/UX Design Intern", "Product Design Intern"},
                    "Design user flows and prototypes in Figma and run usability tests.",
                    new String[]{"Food App Redesign|Figma case study with user interviews", "Campus Map UX|UI/UX research project"})
    };
    private static final String[] FIRST = {"Aditi", "Rahul", "Sanya", "Arjun", "Meera", "Karan", "Pooja", "Nikhil", "Riya", "Siddharth",
            "Tanvi", "Yash", "Isha", "Varun", "Kavya", "Harsh", "Nisha", "Dev", "Anjali", "Manav"};
    private static final String[] LAST = {"Sharma", "Verma", "Reddy", "Iyer", "Nair", "Gupta", "Singh", "Patel", "Joshi", "Kulkarni",
            "Menon", "Bose", "Chopra", "Rao", "Das"};
    private static final String[] CITIES = {"Bengaluru", "Pune", "Hyderabad", "Mumbai", "Chennai", "New Delhi"};

    private final DataSource dataSource;
    private final PasswordEncoder encoder;

    public DemoDataService(DataSource dataSource, PasswordEncoder encoder) {
        this.dataSource = dataSource;
        this.encoder = encoder;
    }

    /** @return a short summary of what was created */
    @Transactional
    public String generate(int applicantCount, int jobCount) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            Random rnd = new Random(42);
            Map<String, Long> skillIds = skillIds(con);
            int offset = count(con, "SELECT COUNT(*) FROM users WHERE email LIKE 'demo.%@example.com'");
            String hash = encoder.encode("demo1234");

            // 1) users (batch, collecting generated ids)
            List<Long> userIds = new ArrayList<>();
            List<Track> userTracks = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (email, password_hash, full_name, role) VALUES (?, ?, ?, 'APPLICANT')",
                    Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < applicantCount; i++) {
                    int n = offset + i + 1;
                    ps.setString(1, "demo." + n + "@example.com");
                    ps.setString(2, hash);
                    ps.setString(3, FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)]);
                    ps.addBatch();
                    userTracks.add(TRACKS[rnd.nextInt(TRACKS.length)]);
                }
                ps.executeBatch();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    while (keys.next()) {
                        userIds.add(keys.getLong(1));
                    }
                }
            }

            // 2) profiles, cities, skills
            try (PreparedStatement profile = con.prepareStatement("""
                    INSERT INTO applicant_profile (user_id, headline, about, city, degree, grad_year, pref_work_mode,
                                                   pref_job_type, min_stipend, discoverable)
                    VALUES (?, ?, ?, ?, 'B.Tech', ?, ?, 'INTERNSHIP', ?, TRUE)""");
                 PreparedStatement city = con.prepareStatement("INSERT IGNORE INTO applicant_pref_city VALUES (?, ?)");
                 PreparedStatement skill = con.prepareStatement("INSERT IGNORE INTO applicant_skill VALUES (?, ?, ?)")) {
                String[] modes = {"ANY", "ANY", "HYBRID", "ONSITE", "REMOTE"};
                for (int i = 0; i < userIds.size(); i++) {
                    long id = userIds.get(i);
                    Track t = userTracks.get(i);
                    String home = CITIES[rnd.nextInt(CITIES.length)];
                    profile.setLong(1, id);
                    profile.setString(2, t.headline());
                    profile.setString(3, "Student interested in " + t.name() + " roles.");
                    profile.setString(4, home);
                    profile.setInt(5, 2025 + rnd.nextInt(4));
                    profile.setString(6, modes[rnd.nextInt(modes.length)]);
                    profile.setInt(7, 5000 * rnd.nextInt(5));
                    profile.addBatch();
                    city.setLong(1, id);
                    city.setString(2, home);
                    city.addBatch();
                    for (String s : pick(t.skills(), 3 + rnd.nextInt(4), rnd)) {
                        skill.setLong(1, id);
                        skill.setLong(2, skillIds.get(s));
                        skill.setInt(3, 2 + rnd.nextInt(4));
                        skill.addBatch();
                    }
                }
                profile.executeBatch();
                city.executeBatch();
                skill.executeBatch();
            }

            // 3) one or two resume items per applicant (their own text, so tailoring has material)
            for (int i = 0; i < userIds.size(); i++) {
                Track t = userTracks.get(i);
                for (String p : pick(t.projects(), 1 + rnd.nextInt(2), rnd)) {
                    String[] parts = p.split("\\|");
                    long itemId = insertReturningKey(con,
                            "INSERT INTO resume_item (applicant_id, item_type, title, description, display_order) VALUES (?, 'PROJECT', ?, ?, 1)",
                            userIds.get(i), parts[0], parts[1]);
                    try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO resume_item_skill VALUES (?, ?)")) {
                        for (String s : pick(t.skills(), 2, rnd)) {
                            ps.setLong(1, itemId);
                            ps.setLong(2, skillIds.get(s));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }

            // 4) jobs for verified companies, posted by one of their recruiters
            List<long[]> companyHirers = companyHirers(con);
            List<Long> jobIds = new ArrayList<>();
            String[] jobModes = {"REMOTE", "HYBRID", "ONSITE"};
            for (int i = 0; i < jobCount && !companyHirers.isEmpty(); i++) {
                Track t = TRACKS[rnd.nextInt(TRACKS.length)];
                long[] ch = companyHirers.get(rnd.nextInt(companyHirers.size()));
                String mode = jobModes[rnd.nextInt(3)];
                int min = 5000 * (1 + rnd.nextInt(6));
                long jobId = insertReturningKey(con, """
                                INSERT INTO job (company_id, posted_by, title, description, job_type, work_mode, city,
                                                 stipend_min, stipend_max, openings, deadline, status)
                                VALUES (?, ?, ?, ?, 'INTERNSHIP', ?, ?, ?, ?, ?, ?, 'ACTIVE')""",
                        ch[0], ch[1], t.jobTitles()[rnd.nextInt(t.jobTitles().length)], t.jobText(), mode,
                        mode.equals("REMOTE") ? null : CITIES[rnd.nextInt(CITIES.length)], min, min + 5000 * (1 + rnd.nextInt(3)),
                        1 + rnd.nextInt(3), Date.valueOf(AppTime.today().plusDays(10 + rnd.nextInt(40))));
                jobIds.add(jobId);
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO job_skill VALUES (?, ?, ?, ?)")) {
                    List<String> chosen = pick(t.skills(), 3 + rnd.nextInt(3), rnd);
                    for (int k = 0; k < chosen.size(); k++) {
                        ps.setLong(1, jobId);
                        ps.setLong(2, skillIds.get(chosen.get(k)));
                        ps.setInt(3, 2 + rnd.nextInt(4));
                        ps.setBoolean(4, k < 2);   // the first two picked skills are required
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 5) swipes: each demo applicant swipes ~8 random jobs, liking ~40% (only past days, so today's quota is untouched)
            int swipes = 0;
            List<Long> allJobs = new ArrayList<>(jobIds);
            allJobs.addAll(activeSeedJobs(con));
            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT IGNORE INTO applicant_swipe (applicant_id, job_id, direction, created_at)
                    VALUES (?, ?, ?, NOW() - INTERVAL ? DAY)""")) {
                for (long uid : userIds) {
                    for (Long jobId : pickIds(allJobs, 8, rnd)) {
                        ps.setLong(1, uid);
                        ps.setLong(2, jobId);
                        ps.setString(3, rnd.nextInt(10) < 4 ? "RIGHT" : "LEFT");
                        ps.setInt(4, 1 + rnd.nextInt(13));
                        ps.addBatch();
                        swipes++;
                    }
                }
                ps.executeBatch();
            }
            return userIds.size() + " applicants, " + jobIds.size() + " jobs and " + swipes + " swipes created (password demo1234)";
        } catch (SQLException e) {
            throw new IllegalStateException("Demo data generation failed: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    private static Map<String, Long> skillIds(Connection con) throws SQLException {
        Map<String, Long> m = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT name, id FROM skill"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString(1), rs.getLong(2));
            }
        }
        return m;
    }

    private static List<long[]> companyHirers(Connection con) throws SQLException {
        List<long[]> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT c.id, MIN(h.user_id) FROM company c JOIN hirer h ON h.company_id = c.id WHERE c.status = 'VERIFIED' GROUP BY c.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new long[]{rs.getLong(1), rs.getLong(2)});
            }
        }
        return list;
    }

    private static List<Long> activeSeedJobs(Connection con) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM job WHERE status = 'ACTIVE' AND id <= 12");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong(1));
            }
        }
        return ids;
    }

    private static int count(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static long insertReturningKey(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static List<String> pick(String[] from, int n, Random rnd) {
        Set<String> s = new LinkedHashSet<>();
        while (s.size() < Math.min(n, from.length)) {
            s.add(from[rnd.nextInt(from.length)]);
        }
        return new ArrayList<>(s);
    }

    private static List<Long> pickIds(List<Long> from, int n, Random rnd) {
        Set<Long> s = new LinkedHashSet<>();
        while (s.size() < Math.min(n, from.size())) {
            s.add(from.get(rnd.nextInt(from.size())));
        }
        return new ArrayList<>(s);
    }
}
