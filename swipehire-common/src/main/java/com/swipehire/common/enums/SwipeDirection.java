package com.swipehire.common.enums;

/** SUPER is applicant-only: a like with a 140-char pitch, limited to 3 per day. */
public enum SwipeDirection {
    LEFT, RIGHT, SUPER;

    /** RIGHT and SUPER both count as "interested" for match detection. */
    public boolean isPositive() {
        return this != LEFT;
    }
}
