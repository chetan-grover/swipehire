package com.swipehire.web.service;

import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.web.dao.SwipeDao;
import com.swipehire.web.entity.Application;
import com.swipehire.web.entity.ApplicationStatusHistory;
import com.swipehire.web.entity.JobMatch;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.ApplicationRepository;
import com.swipehire.web.repository.ApplicationStatusHistoryRepository;
import com.swipehire.web.repository.JobMatchRepository;
import com.swipehire.web.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Turns a mutual right-swipe into a match + application + first history row. */
@Service
public class MatchService {

    private final SwipeDao swipeDao;
    private final JobMatchRepository matches;
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository history;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public MatchService(SwipeDao swipeDao, JobMatchRepository matches, ApplicationRepository applications,
                        ApplicationStatusHistoryRepository history, UserRepository users,
                        ApplicationEventPublisher events) {
        this.swipeDao = swipeDao;
        this.matches = matches;
        this.applications = applications;
        this.history = history;
        this.users = users;
        this.events = events;
    }

    /**
     * Must run inside the swipe transaction (MANDATORY), after the applicant row was locked.
     * @return true if a new match was created
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean createMatch(long applicantId, long jobId) {
        Long matchId = swipeDao.insertMatchIfAbsent(applicantId, jobId).orElse(null);
        if (matchId == null) {
            return false;
        }
        // The row was inserted with JDBC on the same connection, so JPA can read it right away.
        JobMatch match = matches.findById(matchId).orElseThrow();
        Application app = applications.save(new Application(match));
        history.save(new ApplicationStatusHistory(app, null, ApplicationStatus.MATCHED, null, "Mutual right-swipe"));

        User applicant = match.getApplicant().getUser();
        User hirer = users.findById(match.getJob().getPostedBy()).orElseThrow();
        // Listeners run after commit (see MatchEventListener): alerts, email, tailored PDF.
        events.publishEvent(new MatchCreatedEvent(app.getId(), matchId, applicantId, applicant.getFullName(),
                applicant.getEmail(), hirer.getId(), hirer.getEmail(), match.getJob().getTitle(),
                match.getJob().getCompany().getName()));
        return true;
    }
}
