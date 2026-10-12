package com.swipehire.common.scoring;

import com.swipehire.common.enums.SwipeDirection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Learns what an applicant likes from their swipes: every skill on a liked job gets a small
 * positive nudge, every skill on a passed job a smaller negative one. Values stay in [-1, 1]
 * and decay daily so old habits fade. This is the "lightweight collaborative filtering" from the proposal.
 */
public final class PreferenceUpdater {

    public static final double RIGHT_DELTA = 0.10;
    public static final double SUPER_DELTA = 0.20;
    public static final double LEFT_DELTA = -0.05;
    public static final double DAILY_DECAY = 0.98;

    private PreferenceUpdater() {
    }

    public static double delta(SwipeDirection direction) {
        return switch (direction) {
            case RIGHT -> RIGHT_DELTA;
            case SUPER -> SUPER_DELTA;
            case LEFT -> LEFT_DELTA;
        };
    }

    /**
     * @param current  the applicant's current affinities (missing = 0)
     * @param skillIds skills of the swiped job
     * @param undo     true to reverse an earlier swipe (applies the opposite nudge)
     * @return only the entries that changed: skillId -> new affinity
     */
    public static Map<Long, Double> apply(Map<Long, Double> current, Collection<Long> skillIds,
                                          SwipeDirection direction, boolean undo) {
        double d = undo ? -delta(direction) : delta(direction);
        Map<Long, Double> updated = new HashMap<>();
        for (Long skillId : skillIds) {
            double old = current.getOrDefault(skillId, 0.0);
            updated.put(skillId, Math.max(-1, Math.min(1, old + d)));
        }
        return updated;
    }
}
