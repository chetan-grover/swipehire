-- =====================================================================
-- SwipeHire seed data (fictional people and companies).
-- INSERT IGNORE + fixed ids keep it idempotent: rerunning on every boot is harmless.
-- Every seeded account's password is: demo1234
-- =====================================================================

-- ---------- Users ----------
INSERT IGNORE INTO users (id, email, password_hash, full_name, role) VALUES
(1,   'admin@swipehire.in',        '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Platform Admin',  'ADMIN'),
(2,   'priya@zenlabs.in',          '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Priya Sharma',    'HIRER'),
(3,   'arjun@finedge.in',          '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Arjun Malhotra',  'HIRER'),
(4,   'meera@cloudnest.io',        '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Meera Pillai',    'HIRER'),
(5,   'rohit@pixelforge.in',       '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Rohit Verma',     'HIRER'),
(101, 'aarav.mehta@gmail.com',     '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Aarav Mehta',     'APPLICANT'),
(102, 'ishita.rao@gmail.com',      '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Ishita Rao',      'APPLICANT'),
(103, 'kabir.singh@gmail.com',     '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Kabir Singh',     'APPLICANT'),
(104, 'ananya.iyer@gmail.com',     '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Ananya Iyer',     'APPLICANT'),
(105, 'rohan.das@gmail.com',       '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Rohan Das',       'APPLICANT'),
(106, 'sneha.kulkarni@gmail.com',  '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Sneha Kulkarni',  'APPLICANT'),
(107, 'vikram.nair@gmail.com',     '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Vikram Nair',     'APPLICANT'),
(108, 'diya.patel@gmail.com',      '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Diya Patel',      'APPLICANT'),
(109, 'aditya.joshi@gmail.com',    '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Aditya Joshi',    'APPLICANT'),
(110, 'neha.gupta@gmail.com',      '$2a$10$U.701k1zOpx6Bm9wa.J.tuCeLMJPaQatKyYUCmZQPziOxHVG2jg4e', 'Neha Gupta',      'APPLICANT');

-- ---------- Colleges ----------
INSERT IGNORE INTO college (id, name, email_domain, city, status) VALUES
(1, 'Vellore Institute of Technology',            'vitstudent.ac.in',        'Vellore',   'VERIFIED'),
(2, 'Indian Institute of Technology Delhi',       'iitd.ac.in',              'New Delhi', 'VERIFIED'),
(3, 'BITS Pilani',                                'pilani.bits-pilani.ac.in','Pilani',    'VERIFIED'),
(4, 'Delhi Technological University',             'dtu.ac.in',               'New Delhi', 'VERIFIED'),
(5, 'National Institute of Technology Trichy',    'nitt.edu',                'Tiruchirappalli', 'VERIFIED'),
(6, 'SRM Institute of Science and Technology',    'srmist.edu.in',           'Chennai',   'VERIFIED'),
(7, 'Manipal Institute of Technology',            'learner.manipal.edu',     'Manipal',   'VERIFIED');

-- ---------- Skills dictionary ----------
INSERT IGNORE INTO skill (id, name, category) VALUES
(1, 'Java', 'Language'), (2, 'Python', 'Language'), (3, 'JavaScript', 'Language'), (4, 'C++', 'Language'),
(5, 'C', 'Language'), (6, 'Go', 'Language'), (7, 'Kotlin', 'Language'), (8, 'TypeScript', 'Language'),
(9, 'SQL', 'Language'),
(10, 'Spring Boot', 'Backend'), (11, 'Hibernate', 'Backend'), (12, 'Node.js', 'Backend'),
(13, 'REST APIs', 'Backend'), (14, 'Microservices', 'Backend'), (15, 'JDBC', 'Backend'),
(16, 'JSP', 'Backend'), (17, 'Servlets', 'Backend'),
(18, 'HTML', 'Frontend'), (19, 'CSS', 'Frontend'), (20, 'React', 'Frontend'), (21, 'Angular', 'Frontend'),
(22, 'MySQL', 'Database'), (23, 'PostgreSQL', 'Database'), (24, 'MongoDB', 'Database'), (25, 'Redis', 'Database'),
(26, 'Git', 'DevOps'), (27, 'Docker', 'DevOps'), (28, 'Linux', 'DevOps'), (29, 'AWS', 'DevOps'), (30, 'Kubernetes', 'DevOps'),
(31, 'Machine Learning', 'Data'), (32, 'Pandas', 'Data'), (33, 'Data Analysis', 'Data'), (34, 'Excel', 'Data'),
(35, 'Power BI', 'Data'),
(36, 'Data Structures', 'CS Fundamentals'), (37, 'Algorithms', 'CS Fundamentals'), (38, 'OOP', 'CS Fundamentals'),
(39, 'Operating Systems', 'CS Fundamentals'), (40, 'Computer Networks', 'CS Fundamentals'),
(41, 'Figma', 'Design'), (42, 'UI/UX Design', 'Design'),
(43, 'Android', 'Mobile'), (44, 'Communication', 'Soft Skill'), (45, 'JUnit', 'Testing');

-- Aliases are lowercase; the PDF import lowercases text before matching.
INSERT IGNORE INTO skill_synonym (alias, skill_id) VALUES
('js', 3), ('ts', 8), ('cpp', 4), ('c plus plus', 4), ('golang', 6),
('spring', 10), ('springboot', 10), ('node', 12), ('nodejs', 12), ('rest', 13), ('restful', 13),
('reactjs', 20), ('react.js', 20), ('postgres', 23), ('mongo', 24),
('k8s', 30), ('amazon web services', 29),
('ml', 31), ('dsa', 36), ('data structures and algorithms', 36), ('oops', 38),
('object oriented programming', 38), ('ms excel', 34), ('powerbi', 35), ('ux', 42), ('ui/ux', 42), ('junit 5', 45);

-- ---------- Companies & hirers ----------
INSERT IGNORE INTO company (id, name, website, description, city, status, verified_by, verified_at) VALUES
(1, 'ZenLabs Technologies', 'https://zenlabs.example', 'Product studio building SaaS tools for Indian SMEs.', 'Bengaluru', 'VERIFIED', 1, NOW()),
(2, 'FinEdge Capital', 'https://finedge.example', 'Fintech firm working on trading infrastructure and credit analytics.', 'Mumbai', 'VERIFIED', 1, NOW()),
(3, 'CloudNest Systems', 'https://cloudnest.example', 'Cloud hosting and developer platform company.', 'Pune', 'VERIFIED', 1, NOW()),
(4, 'PixelForge Studios', 'https://pixelforge.example', 'Early-stage design and game studio.', 'Hyderabad', 'PENDING', NULL, NULL);

INSERT IGNORE INTO hirer (user_id, company_id, designation) VALUES
(2, 1, 'Engineering Manager'),
(3, 2, 'Talent Acquisition Lead'),
(4, 3, 'HR Business Partner'),
(5, 4, 'Founder');

-- ---------- Jobs ----------
INSERT IGNORE INTO job (id, company_id, posted_by, title, description, job_type, work_mode, city,
                        stipend_min, stipend_max, openings, deadline, min_grad_year, max_grad_year, verified_only, status) VALUES
(1, 1, 2, 'Java Backend Intern',
 'Build and maintain REST APIs in Java and Spring Boot for our invoicing product. You will write SQL queries, design database tables, review pull requests on Git and help containerise services with Docker. Strong OOP fundamentals expected.',
 'INTERNSHIP', 'HYBRID', 'Bengaluru', 25000, 35000, 2, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 2026, 2028, FALSE, 'ACTIVE'),
(2, 1, 2, 'Frontend Developer Intern',
 'Create responsive web interfaces using React, JavaScript and TypeScript. Translate Figma designs into clean HTML and CSS components, fix UI bugs and collaborate through Git.',
 'INTERNSHIP', 'REMOTE', NULL, 15000, 20000, 2, DATE_ADD(CURDATE(), INTERVAL 21 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(3, 1, 2, 'Graduate Software Engineer',
 'Full-time role for fresh graduates. Solve problems with data structures and algorithms, write object oriented Java services, and work close to databases and operating system concepts. Verified students only.',
 'FULL_TIME', 'ONSITE', 'Bengaluru', 60000, 80000, 3, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 2025, 2026, TRUE, 'ACTIVE'),
(4, 2, 3, 'Data Analyst Intern',
 'Analyse loan and transaction data with SQL and Python Pandas. Build Power BI and Excel dashboards for the credit team and present insights clearly to stakeholders.',
 'INTERNSHIP', 'HYBRID', 'Mumbai', 20000, 30000, 1, DATE_ADD(CURDATE(), INTERVAL 25 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(5, 2, 3, 'Quant Developer Intern',
 'Write low-latency C++ components for our order matching engine and Python tools for backtesting. Requires strong algorithms and data structures skills and comfort with Linux.',
 'INTERNSHIP', 'ONSITE', 'Mumbai', 40000, 50000, 1, DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, NULL, TRUE, 'ACTIVE'),
(6, 2, 3, 'Machine Learning Intern',
 'Train and evaluate machine learning models for credit risk using Python, Pandas and scikit-style workflows. Clean data with SQL, track experiments and version code with Git.',
 'INTERNSHIP', 'REMOTE', NULL, 25000, 30000, 2, DATE_ADD(CURDATE(), INTERVAL 30 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(7, 3, 4, 'DevOps Intern',
 'Automate deployments on Linux servers, build Docker images, manage AWS resources and help run Kubernetes clusters. Scripting in Python and disciplined Git usage required.',
 'INTERNSHIP', 'HYBRID', 'Pune', 20000, 25000, 2, DATE_ADD(CURDATE(), INTERVAL 28 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(8, 3, 4, 'Backend Engineer (Go)',
 'Design microservices in Go backed by PostgreSQL and Redis. Expose REST APIs, containerise with Docker and own services end to end in production.',
 'FULL_TIME', 'REMOTE', NULL, 70000, 90000, 1, DATE_ADD(CURDATE(), INTERVAL 40 DAY), 2025, 2026, FALSE, 'ACTIVE'),
(9, 3, 4, 'Full Stack Intern',
 'Work across a Java Spring Boot backend and a React frontend for our customer console. Write MySQL queries, HTML and CSS, and ship features weekly.',
 'INTERNSHIP', 'ONSITE', 'Pune', 18000, 22000, 2, DATE_ADD(CURDATE(), INTERVAL 18 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(10, 3, 4, 'Android Developer Intern',
 'Build features for our Android monitoring app in Kotlin, integrate REST APIs and maintain legacy Java screens. Code reviews happen on Git.',
 'INTERNSHIP', 'HYBRID', 'Pune', 15000, 20000, 1, DATE_ADD(CURDATE(), INTERVAL 22 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(11, 4, 5, 'UI/UX Design Intern',
 'Design user flows, wireframes and high-fidelity prototypes in Figma for mobile games. Run small usability tests and communicate design decisions to developers.',
 'INTERNSHIP', 'ONSITE', 'Hyderabad', 10000, 15000, 1, DATE_ADD(CURDATE(), INTERVAL 15 DAY), NULL, NULL, FALSE, 'ACTIVE'),
(12, 4, 5, 'Game Programmer Intern',
 'Implement gameplay systems in C++ with clean object oriented design and efficient algorithms.',
 'INTERNSHIP', 'ONSITE', 'Hyderabad', 12000, 16000, 1, DATE_ADD(CURDATE(), INTERVAL 30 DAY), NULL, NULL, FALSE, 'DRAFT');

INSERT IGNORE INTO job_skill (job_id, skill_id, weight, is_required) VALUES
(1, 1, 5, TRUE), (1, 10, 4, TRUE), (1, 9, 3, TRUE), (1, 13, 3, FALSE), (1, 26, 2, FALSE), (1, 27, 2, FALSE), (1, 38, 3, FALSE),
(2, 3, 5, TRUE), (2, 20, 4, TRUE), (2, 18, 3, TRUE), (2, 19, 3, FALSE), (2, 8, 2, FALSE), (2, 26, 2, FALSE),
(3, 36, 5, TRUE), (3, 37, 5, TRUE), (3, 38, 3, TRUE), (3, 1, 4, FALSE), (3, 9, 3, FALSE), (3, 39, 2, FALSE),
(4, 9, 5, TRUE), (4, 34, 4, TRUE), (4, 2, 4, FALSE), (4, 32, 3, FALSE), (4, 35, 3, FALSE), (4, 44, 2, FALSE),
(5, 4, 5, TRUE), (5, 2, 4, TRUE), (5, 37, 4, TRUE), (5, 36, 3, FALSE), (5, 28, 2, FALSE),
(6, 2, 5, TRUE), (6, 31, 5, TRUE), (6, 32, 4, FALSE), (6, 9, 3, FALSE), (6, 26, 2, FALSE),
(7, 28, 5, TRUE), (7, 27, 4, TRUE), (7, 26, 3, TRUE), (7, 29, 4, FALSE), (7, 30, 3, FALSE), (7, 2, 2, FALSE),
(8, 6, 5, TRUE), (8, 23, 4, TRUE), (8, 14, 4, FALSE), (8, 27, 3, FALSE), (8, 25, 3, FALSE), (8, 13, 3, FALSE),
(9, 1, 4, TRUE), (9, 20, 4, TRUE), (9, 10, 4, FALSE), (9, 22, 3, FALSE), (9, 18, 2, FALSE), (9, 19, 2, FALSE),
(10, 7, 5, TRUE), (10, 43, 5, TRUE), (10, 1, 3, FALSE), (10, 13, 3, FALSE), (10, 26, 2, FALSE),
(11, 41, 5, TRUE), (11, 42, 5, TRUE), (11, 44, 3, FALSE), (11, 18, 2, FALSE), (11, 19, 2, FALSE),
(12, 4, 4, TRUE), (12, 38, 3, FALSE), (12, 37, 2, FALSE);

-- The quant role is open only to verified students of these colleges.
INSERT IGNORE INTO job_allowed_college (job_id, college_id) VALUES (5, 1), (5, 2), (5, 3);

-- ---------- Applicant profiles ----------
INSERT IGNORE INTO applicant_profile (user_id, headline, about, city, degree, grad_year, college_id, college_email,
                                      college_verified_at, pref_work_mode, pref_job_type, min_stipend, discoverable) VALUES
(101, 'Java backend developer in the making', 'I enjoy building APIs and designing databases.', 'Vellore', 'B.Tech CSE', 2027, 1, 'aarav.mehta2023@vitstudent.ac.in', NOW(), 'HYBRID', 'INTERNSHIP', 15000, TRUE),
(102, 'ML and data enthusiast', 'Applied ML for agriculture and finance.', 'New Delhi', 'B.Tech Mathematics and Computing', 2027, 2, 'ishita.rao@iitd.ac.in', NOW(), 'ANY', 'INTERNSHIP', 20000, TRUE),
(103, 'Frontend developer who loves clean UI', 'Self-taught React developer and freelancer.', 'New Delhi', 'BCA', 2026, NULL, NULL, NULL, 'REMOTE', 'INTERNSHIP', 10000, TRUE),
(104, 'Competitive programmer, C++', 'ICPC regionalist interested in trading systems.', 'Pilani', 'B.E. Computer Science', 2027, 3, 'f20230104@pilani.bits-pilani.ac.in', NOW(), 'ONSITE', 'INTERNSHIP', 30000, TRUE),
(105, 'Cloud and DevOps learner', 'I automate everything I touch.', 'New Delhi', 'B.Tech IT', 2027, 4, 'rohan.das@dtu.ac.in', NOW(), 'HYBRID', 'INTERNSHIP', 15000, TRUE),
(106, 'Android developer (Kotlin)', 'Building apps people in my town actually use.', 'Pune', 'B.Tech CSE', 2027, NULL, NULL, NULL, 'HYBRID', 'INTERNSHIP', 12000, TRUE),
(107, 'Backend engineer, distributed systems', 'Final-year student looking for full-time backend roles.', 'Tiruchirappalli', 'B.Tech CSE', 2026, 5, 'vikram.nair@nitt.edu', NOW(), 'ANY', 'FULL_TIME', 50000, TRUE),
(108, 'Product designer', 'I design interfaces that feel obvious.', 'Chennai', 'B.Des', 2027, 6, 'diya.patel@srmist.edu.in', NOW(), 'ONSITE', 'INTERNSHIP', 8000, TRUE),
(109, 'Exploring web development', 'Second-year student trying out full stack.', 'Indore', 'B.Tech CSE', 2028, NULL, NULL, NULL, 'ANY', 'ANY', 0, FALSE),
(110, 'Data analyst', 'Turning spreadsheets into decisions.', 'Mumbai', 'B.Tech IT', 2026, 7, 'neha.gupta@learner.manipal.edu', NOW(), 'HYBRID', 'INTERNSHIP', 20000, TRUE);

INSERT IGNORE INTO applicant_pref_city (applicant_id, city) VALUES
(101, 'Bengaluru'), (101, 'Pune'), (102, 'Mumbai'), (102, 'Bengaluru'), (103, 'New Delhi'),
(104, 'Mumbai'), (104, 'Hyderabad'), (105, 'Pune'), (105, 'Bengaluru'), (106, 'Pune'),
(107, 'Bengaluru'), (107, 'Pune'), (108, 'Hyderabad'), (108, 'Chennai'), (110, 'Mumbai');

INSERT IGNORE INTO applicant_skill (applicant_id, skill_id, proficiency) VALUES
(101, 1, 4), (101, 10, 3), (101, 9, 4), (101, 22, 4), (101, 13, 3), (101, 26, 3), (101, 36, 4), (101, 37, 3), (101, 38, 4), (101, 15, 3), (101, 11, 2),
(102, 2, 5), (102, 31, 4), (102, 32, 4), (102, 9, 3), (102, 33, 4), (102, 26, 3), (102, 4, 3), (102, 37, 4), (102, 36, 4),
(103, 3, 4), (103, 20, 4), (103, 18, 5), (103, 19, 4), (103, 8, 3), (103, 26, 3), (103, 41, 2), (103, 12, 3),
(104, 4, 5), (104, 2, 4), (104, 37, 5), (104, 36, 5), (104, 28, 3), (104, 39, 3),
(105, 28, 4), (105, 27, 4), (105, 29, 3), (105, 30, 2), (105, 26, 4), (105, 2, 3), (105, 6, 2),
(106, 7, 4), (106, 43, 4), (106, 1, 3), (106, 13, 3), (106, 26, 3), (106, 41, 2),
(107, 1, 4), (107, 6, 3), (107, 23, 4), (107, 14, 3), (107, 27, 3), (107, 25, 3), (107, 13, 4), (107, 36, 4), (107, 37, 4), (107, 38, 4), (107, 39, 3), (107, 9, 4), (107, 40, 3),
(108, 41, 5), (108, 42, 5), (108, 18, 3), (108, 19, 3), (108, 44, 4),
(109, 1, 3), (109, 18, 3), (109, 19, 3), (109, 3, 3), (109, 22, 3), (109, 10, 2), (109, 20, 2), (109, 16, 3), (109, 17, 3),
(110, 9, 5), (110, 34, 5), (110, 35, 4), (110, 2, 3), (110, 32, 3), (110, 33, 4), (110, 44, 4);

-- ---------- Structured resume items ----------
INSERT IGNORE INTO resume_item (id, applicant_id, item_type, title, organization, start_date, end_date, description, display_order) VALUES
(1001, 101, 'EDUCATION', 'B.Tech Computer Science and Engineering', 'VIT Vellore', '2023-07-01', '2027-05-31', 'CGPA 8.7. Coursework: DBMS, Operating Systems, Advanced Java.', 1),
(1002, 101, 'PROJECT', 'Campus Canteen Ordering System', NULL, '2024-08-01', '2024-11-30', 'Built a Spring Boot REST API with MySQL for pre-ordering food; reduced queue time during lunch breaks. Designed normalised tables and wrote SQL reports.', 2),
(1003, 101, 'PROJECT', 'Library Management with JDBC', NULL, '2024-01-01', '2024-03-31', 'Desktop app in Java using JDBC and MySQL with an object oriented domain model for books, members and loans.', 3),
(1004, 101, 'EXPERIENCE', 'Backend Intern', 'LocalKart', '2025-05-01', '2025-07-31', 'Added REST endpoints in Java Spring Boot for the seller dashboard and fixed bugs reported on Git issues.', 4),
(1005, 101, 'ACHIEVEMENT', 'Top 5% in LeetCode Weekly Contest', NULL, '2025-02-01', NULL, 'Consistent problem solving in data structures and algorithms.', 5),

(1011, 102, 'EDUCATION', 'B.Tech Mathematics and Computing', 'IIT Delhi', '2023-07-01', '2027-05-31', 'CGPA 9.1.', 1),
(1012, 102, 'PROJECT', 'Crop Yield Prediction', NULL, '2024-06-01', '2024-09-30', 'Trained gradient boosted machine learning models in Python on district-level rainfall and soil data using Pandas.', 2),
(1013, 102, 'PROJECT', 'Stock Sentiment Dashboard', NULL, '2025-01-01', '2025-03-31', 'Data analysis pipeline in Python and SQL that scores news sentiment for NIFTY stocks.', 3),
(1014, 102, 'EXPERIENCE', 'Research Intern', 'IIT Delhi AI Lab', '2025-05-01', '2025-07-31', 'Implemented machine learning baselines in Python for satellite image classification.', 4),
(1015, 102, 'ACHIEVEMENT', 'Kaggle Notebooks Expert', NULL, '2024-12-01', NULL, 'Published machine learning notebooks with 500+ upvotes.', 5),

(1021, 103, 'PROJECT', 'Portfolio Builder', NULL, '2024-03-01', '2024-05-31', 'React and JavaScript app that generates personal portfolio sites from a form; responsive HTML and CSS.', 1),
(1022, 103, 'PROJECT', 'Realtime Chat App', NULL, '2024-09-01', '2024-10-31', 'Chat application with a Node.js backend and React frontend.', 2),
(1023, 103, 'EXPERIENCE', 'Freelance Web Developer', 'Self-employed', '2023-06-01', NULL, 'Delivered 8 small-business websites in HTML, CSS and JavaScript.', 3),
(1024, 103, 'CERTIFICATION', 'Meta Front-End Developer Certificate', 'Coursera', '2024-01-01', '2024-02-28', 'React, JavaScript and accessibility fundamentals.', 4),

(1031, 104, 'EDUCATION', 'B.E. Computer Science', 'BITS Pilani', '2023-08-01', '2027-05-31', 'CGPA 9.0.', 1),
(1032, 104, 'ACHIEVEMENT', 'ICPC Asia Regionalist', 'ICPC', '2024-12-01', NULL, 'Ranked in top 50 teams at ICPC Asia regionals using C++, algorithms and data structures.', 2),
(1033, 104, 'PROJECT', 'Order Book Matching Engine', NULL, '2025-01-01', '2025-04-30', 'Price-time priority matching engine in C++ on Linux handling 1M orders/sec using custom data structures.', 3),
(1034, 104, 'PROJECT', 'Monte Carlo Option Pricer', NULL, '2024-07-01', '2024-08-31', 'Option pricing in Python with a C++ extension for speed.', 4),

(1041, 105, 'PROJECT', 'CI/CD Pipeline for College Fest Website', NULL, '2024-08-01', '2024-10-31', 'Dockerised the fest site, deployed on AWS EC2 with Linux and automated releases from Git.', 1),
(1042, 105, 'CERTIFICATION', 'AWS Certified Cloud Practitioner', 'Amazon Web Services', '2025-01-01', NULL, 'Cloud fundamentals on AWS.', 2),
(1043, 105, 'EXPERIENCE', 'Open Source Contributor', 'CNCF sandbox project', '2025-03-01', NULL, 'Merged Go pull requests improving Kubernetes operator logging; active on Git reviews.', 3),

(1051, 106, 'PROJECT', 'Expense Tracker Android App', NULL, '2024-05-01', '2024-07-31', 'Kotlin Android app with offline storage, 1k+ downloads.', 1),
(1052, 106, 'PROJECT', 'City Bus Pass App', NULL, '2023-12-01', '2024-02-28', 'Android app in Java that renews bus passes through a REST API.', 2),
(1053, 106, 'ACHIEVEMENT', 'Smart India Hackathon Finalist', 'Government of India', '2024-12-01', NULL, 'Built an Android Kotlin app for farm equipment rental.', 3),

(1061, 107, 'EXPERIENCE', 'SDE Intern', 'PayRail', '2025-05-01', '2025-07-31', 'Built Java microservices with PostgreSQL and Redis caching for payment reconciliation.', 1),
(1062, 107, 'PROJECT', 'URL Shortener in Go', NULL, '2024-09-01', '2024-10-31', 'Go service with Redis, packaged with Docker, 5k req/s on a laptop.', 2),
(1063, 107, 'PROJECT', 'Distributed Key-Value Store', NULL, '2025-01-01', '2025-04-30', 'Raft-based store in Go; applied operating systems and computer networks concepts.', 3),

(1071, 108, 'PROJECT', 'Redesign of College ERP', NULL, '2024-07-01', '2024-09-30', 'Figma redesign of the student ERP; UI/UX research with 40 students.', 1),
(1072, 108, 'EXPERIENCE', 'Design Intern', 'Studio Kraft', '2025-05-01', '2025-07-31', 'Designed onboarding flows in Figma and presented UI/UX decisions to clients.', 2),
(1073, 108, 'CERTIFICATION', 'Google UX Design Certificate', 'Coursera', '2024-01-01', '2024-04-30', 'UI/UX design process and usability testing.', 3),

(1081, 109, 'PROJECT', 'Student Result Portal', NULL, '2025-01-01', '2025-03-31', 'Java Servlets and JSP app with MySQL to publish semester results.', 1),
(1082, 109, 'PROJECT', 'Todo App in React', NULL, '2024-10-01', '2024-10-31', 'Small React and JavaScript todo app.', 2),

(1091, 110, 'EXPERIENCE', 'Business Analyst Intern', 'RetailMart', '2025-05-01', '2025-07-31', 'Wrote SQL to analyse store sales and built Excel and Power BI reports for regional managers.', 1),
(1092, 110, 'PROJECT', 'Sales Performance Dashboard', NULL, '2024-08-01', '2024-09-30', 'Power BI dashboard on a SQL data model with drill-downs by region.', 2),
(1093, 110, 'CERTIFICATION', 'Google Data Analytics Certificate', 'Coursera', '2024-01-01', '2024-05-31', 'Data analysis with SQL, spreadsheets and visualisation.', 3);

INSERT IGNORE INTO resume_item_skill (resume_item_id, skill_id) VALUES
(1002, 10), (1002, 22), (1002, 13), (1002, 9), (1003, 1), (1003, 15), (1003, 22), (1003, 38),
(1004, 1), (1004, 10), (1004, 13), (1004, 26), (1005, 36), (1005, 37),
(1012, 2), (1012, 31), (1012, 32), (1013, 2), (1013, 33), (1013, 9), (1014, 2), (1014, 31), (1015, 31),
(1021, 20), (1021, 3), (1021, 18), (1021, 19), (1022, 12), (1022, 20), (1023, 18), (1023, 19), (1023, 3), (1024, 20), (1024, 3),
(1032, 4), (1032, 37), (1032, 36), (1033, 4), (1033, 28), (1033, 36), (1034, 2), (1034, 4),
(1041, 27), (1041, 29), (1041, 28), (1041, 26), (1042, 29), (1043, 6), (1043, 30), (1043, 26),
(1051, 7), (1051, 43), (1052, 43), (1052, 1), (1052, 13), (1053, 43), (1053, 7),
(1061, 1), (1061, 14), (1061, 23), (1061, 25), (1062, 6), (1062, 25), (1062, 27), (1063, 6), (1063, 39), (1063, 40),
(1071, 41), (1071, 42), (1072, 41), (1072, 42), (1072, 44), (1073, 42),
(1081, 17), (1081, 16), (1081, 22), (1081, 1), (1082, 20), (1082, 3),
(1091, 9), (1091, 34), (1091, 35), (1092, 35), (1092, 9), (1093, 33), (1093, 9), (1093, 34);
