package com.swipehire.common.enums;

import org.junit.jupiter.api.Test;

import static com.swipehire.common.enums.ApplicationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(MATCHED.canMoveTo(APPLIED));
        assertTrue(APPLIED.canMoveTo(SHORTLISTED));
        assertTrue(SHORTLISTED.canMoveTo(INTERVIEW));
        assertTrue(INTERVIEW.canMoveTo(OFFER));
    }

    @Test
    void cannotSkipStagesOrGoBackwards() {
        assertFalse(MATCHED.canMoveTo(OFFER));
        assertFalse(APPLIED.canMoveTo(INTERVIEW));
        assertFalse(INTERVIEW.canMoveTo(APPLIED));
    }

    @Test
    void expiryOnlyBeforeHirerActs() {
        assertTrue(MATCHED.canMoveTo(EXPIRED));
        assertTrue(APPLIED.canMoveTo(EXPIRED));
        assertFalse(SHORTLISTED.canMoveTo(EXPIRED));
    }

    @Test
    void terminalStatesHaveNoExit() {
        for (ApplicationStatus s : new ApplicationStatus[]{OFFER, REJECTED, WITHDRAWN, EXPIRED}) {
            assertTrue(s.isTerminal(), s + " should be terminal");
        }
    }
}
