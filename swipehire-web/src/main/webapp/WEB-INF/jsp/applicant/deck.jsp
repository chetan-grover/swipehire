<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Discover"/><c:set var="activeNav" value="deck"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="deck-wrap">
    <section>
        <c:choose>
            <c:when test="${not checklist.skills}">
                <div class="card empty">
                    <div class="big">&#128075;</div>
                    <h2>Add your skills to start swiping</h2>
                    <p class="muted">Matching is built on skills. Add a few (or import them from your PDF resume) and your deck fills up.</p>
                    <a class="btn clay" href="${ctx}/applicant/skills">Add skills</a>
                </div>
            </c:when>
            <c:when test="${empty card}">
                <div class="card empty">
                    <div class="big">&#127881;</div>
                    <h2>You've seen every job that fits you</h2>
                    <p class="muted">New jobs appear here as soon as recruiters post them. Meanwhile, check your matches or widen your preferences.</p>
                    <a class="btn" href="${ctx}/applicant/matches">My matches</a>
                    <a class="btn ghost" href="${ctx}/applicant/profile">Edit preferences</a>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="job" value="${card.job}"/>
                <div class="deck">
                    <c:if test="${fn:length(cards) > 1}"><div class="ghost-card"></div></c:if>
                    <%-- data-right/data-left name the forms the drag gesture submits --%>
                    <article class="swipe-card" id="swipe-card" data-right="form-right" data-left="form-left">
                        <span class="stamp like">LIKE</span><span class="stamp nope">PASS</span>
                        <div class="card-head">
                            <div>
                                <div class="company">
                                    <c:out value="${job.company.name}"/>
                                    <c:choose>
                                        <c:when test="${job.company.verified}"><span class="badge verified">&#10003; Verified company</span></c:when>
                                        <c:otherwise><span class="badge pending">Unverified</span></c:otherwise>
                                    </c:choose>
                                </div>
                                <h1 style="margin:4px 0"><c:out value="${job.title}"/></h1>
                                <div class="row">
                                    <c:if test="${card.companyInterested}"><span class="badge interest">&#9733; This company is interested in you</span></c:if>
                                    <c:if test="${card.score.exploration}"><span class="badge explore" title="Shown to help you discover something different">Exploring</span></c:if>
                                </div>
                            </div>
                            <div class="score-ring" style="--p:${card.score.totalPercent}" title="Match score">${card.score.totalPercent}%</div>
                        </div>

                        <div class="facts">
                            <span>&#128188; ${f:title(job.jobType)}</span>
                            <span>&#128205; ${f:title(job.workMode)}<c:if test="${not empty job.city}"> &middot; <c:out value="${job.city}"/></c:if></span>
                            <span>&#128176; ${f:money(job.stipendMin)} - ${f:money(job.stipendMax)} / month</span>
                            <span>&#128197; closes in ${f:daysUntil(job.deadline)} days</span>
                            <span title="Share of applications this recruiter answered within 7 days">&#9201;
                                <c:choose>
                                    <c:when test="${card.responsiveness != null}">Responds to ${card.responsiveness}% within a week</c:when>
                                    <c:otherwise>New recruiter</c:otherwise>
                                </c:choose>
                            </span>
                        </div>

                        <h3>Why this job</h3>
                        <sh:skillChips matched="${card.matchedSkills}" missing="${card.missingRequired}" optional="${card.missingOptional}"/>
                        <sh:scoreBars s="${card.score}"/>

                        <details>
                            <summary class="muted">Job description</summary>
                            <p class="desc"><c:out value="${job.description}"/></p>
                        </details>

                        <div class="actions">
                            <form id="form-left" method="post" action="${ctx}/applicant/swipe" class="inline" style="flex:1;display:flex">
                                <sh:csrf/><input type="hidden" name="jobId" value="${job.id}"><input type="hidden" name="direction" value="LEFT">
                                <button class="btn ghost big" style="flex:1">&#10007; Pass</button>
                            </form>
                            <form id="form-right" method="post" action="${ctx}/applicant/swipe" class="inline" style="flex:1;display:flex">
                                <sh:csrf/><input type="hidden" name="jobId" value="${job.id}"><input type="hidden" name="direction" value="RIGHT">
                                <button class="btn green big" style="flex:1" ${rightLeft <= 0 ? 'disabled' : ''}>&#10084; Like</button>
                            </form>
                        </div>
                        <form method="post" action="${ctx}/applicant/swipe" class="pitch-box">
                            <sh:csrf/><input type="hidden" name="jobId" value="${job.id}"><input type="hidden" name="direction" value="SUPER">
                            <input type="text" name="pitch" maxlength="140" required placeholder="Super-like with a 140-character pitch to the recruiter">
                            <button class="btn clay" ${superLeft <= 0 ? 'disabled' : ''}>&#9733; Super</button>
                        </form>
                    </article>
                </div>
                <p class="muted small" style="text-align:center">Tip: drag the card right to like, left to pass.</p>
            </c:otherwise>
        </c:choose>
    </section>

    <aside>
        <div class="card quota">
            <h3>Today</h3>
            <p><b>${rightLeft}</b> of 25 likes left<br><b>${superLeft}</b> of 3 super-likes left</p>
            <c:if test="${remaining >= 0}"><p class="muted small">${remaining} job(s) left in your deck</p></c:if>
            <c:if test="${canUndo}">
                <form method="post" action="${ctx}/applicant/swipe/undo"><sh:csrf/><button class="btn ghost small">&#8630; Undo last swipe (10 s)</button></form>
            </c:if>
        </div>
        <c:if test="${not checklist.complete or not checklist.verified}">
            <div class="card">
                <h3>Improve your matches</h3>
                <ul class="checklist small">
                    <li>${checklist.basics ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/profile">Headline &amp; graduation year</a></li>
                    <li>${checklist.skills ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/skills">Skills</a></li>
                    <li>${checklist.resume ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/resume">Projects &amp; experience</a></li>
                    <li>${checklist.verified ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/verify">Verify your college</a></li>
                </ul>
            </div>
        </c:if>
        <div class="card small muted">
            <b style="color:var(--forest)">How the score works</b><br>
            45% required skills you have, 20% text similarity between your profile and the job (TF-IDF),
            15% what you tend to like, 10% location/mode, 10% stipend. Every 10th card is an
            "exploring" pick so you don't only see more of the same.
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
