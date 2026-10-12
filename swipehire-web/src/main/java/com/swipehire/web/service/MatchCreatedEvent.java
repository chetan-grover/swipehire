package com.swipehire.web.service;

/**
 * Published inside the swipe transaction when a match is created. It carries plain values (not
 * entities) because listeners run AFTER the transaction has committed and its session is closed.
 */
public record MatchCreatedEvent(long applicationId, long matchId, long applicantId, String applicantName,
                                String applicantEmail, long hirerId, String hirerEmail, String jobTitle,
                                String companyName) {
}
