package com.swipehire.common.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Application pipeline states. The allowed transitions live here, in one place, so the
 * service layer and the tests share one source of truth for the state machine.
 */
public enum ApplicationStatus {
    MATCHED, APPLIED, SHORTLISTED, INTERVIEW, OFFER, REJECTED, WITHDRAWN, EXPIRED;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case MATCHED -> EnumSet.of(APPLIED, WITHDRAWN, EXPIRED);
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED, WITHDRAWN, EXPIRED);
            case SHORTLISTED -> EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN);
            case INTERVIEW -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER, REJECTED, WITHDRAWN, EXPIRED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
