<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Application"/><c:set var="activeNav" value="dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<p><a href="${ctx}/hirer/jobs/${job.id}/applications">&larr; Pipeline for <c:out value="${job.title}"/></a></p>
<div class="row spread">
    <div class="row">
        <c:choose>
            <c:when test="${not empty applicant.photoPath}"><img class="avatar" src="${ctx}/files/photo?userId=${applicant.userId}" alt=""></c:when>
            <c:otherwise><span class="avatar">${f:initials(applicant.user.fullName)}</span></c:otherwise>
        </c:choose>
        <div>
            <h1 style="margin:0"><c:out value="${applicant.user.fullName}"/></h1>
            <div class="muted"><c:out value="${applicant.headline}"/></div>
            <c:if test="${applicant.collegeVerified}"><span class="badge verified">&#10003; Verified student &middot; <c:out value="${applicant.college.name}"/></span></c:if>
        </div>
    </div>
    <span class="status ${app.status}" style="font-size:1rem">${app.status}</span>
</div>

<div class="grid two" style="margin-top:14px">
    <div>
        <div class="card">
            <h2 style="margin-top:0">Decision</h2>
            <c:choose>
                <c:when test="${app.status == 'MATCHED'}"><p class="muted">Waiting for the applicant to apply with their tailored resume.</p></c:when>
                <c:when test="${empty nextStatuses}"><p class="muted">This application is closed (${app.status}).</p></c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/hirer/applications/${app.id}/status">
                        <sh:csrf/>
                        <label>Note to the applicant (optional)</label>
                        <input type="text" name="note" maxlength="500" placeholder="e.g. Great project work! Let's talk.">
                        <div class="row" style="margin-top:10px">
                            <c:forEach var="s" items="${nextStatuses}">
                                <button class="btn ${s == 'REJECTED' ? 'ghost' : s == 'OFFER' ? 'green' : ''}" name="status" value="${s}">${f:title(s)}</button>
                            </c:forEach>
                        </div>
                    </form>
                </c:otherwise>
            </c:choose>
            <p style="margin-top:14px"><a class="btn ghost" href="${ctx}/files/resume?applicationId=${app.id}" target="_blank">&#128196; Tailored resume (PDF)</a></p>
        </div>

        <c:if test="${app.status == 'INTERVIEW'}">
            <form class="card" method="post" action="${ctx}/hirer/applications/${app.id}/slots">
                <sh:csrf/>
                <h2 style="margin-top:0">Interview slots</h2>
                <c:forEach var="s" items="${slots}">
                    <div class="small">${f:dateTime(s.slotStart)} (${s.durationMin} min)
                        <c:if test="${s.chosen}"><span class="badge verified">&#10003; picked by applicant</span></c:if></div>
                </c:forEach>
                <p class="muted small">Propose up to 3 times (replaces earlier proposals):</p>
                <div class="form-grid">
                    <div><input type="datetime-local" name="slot1" required></div>
                    <div><input type="datetime-local" name="slot2"></div>
                    <div><input type="datetime-local" name="slot3"></div>
                </div>
                <div class="form-grid">
                    <div><label>Duration (min)</label><input type="number" name="duration" value="30" min="10" max="240"></div>
                    <div><label>Where / link</label><input type="text" name="location" maxlength="255" placeholder="Google Meet link or office address"></div>
                </div>
                <p><button class="btn">Send slots</button></p>
            </form>
        </c:if>

        <div class="card">
            <h2 style="margin-top:0">Match score ${score.totalPercent}%</h2>
            <sh:skillChips matched="${matchedSkills}" missing="${missingSkills}"/>
            <sh:scoreBars s="${score}"/>
        </div>

        <div class="card">
            <h2 style="margin-top:0">Full profile</h2>
            <p class="muted small"><c:out value="${applicant.degree}"/> <c:if test="${applicant.gradYear != null}">&middot; Class of ${applicant.gradYear}</c:if> &middot; <c:out value="${applicant.city}"/></p>
            <c:if test="${not empty applicant.about}"><p><c:out value="${applicant.about}"/></p></c:if>
            <div class="chips"><c:forEach var="s" items="${applicant.skills}"><span class="chip"><c:out value="${s.skill.name}"/> &middot; ${s.proficiency}/5</span></c:forEach></div>
            <c:forEach var="it" items="${resumeItems}">
                <div style="margin-top:10px"><b><c:out value="${it.title}"/></b> <span class="muted small">${f:title(it.itemType)} &middot; ${it.dateRange}</span>
                    <div class="small"><c:out value="${it.description}"/></div></div>
            </c:forEach>
        </div>
    </div>

    <div>
        <%@ include file="/WEB-INF/jsp/common/chat.jspf" %>
        <div class="card">
            <h2 style="margin-top:0">Timeline</h2>
            <ul class="timeline">
                <c:forEach var="h" items="${timeline}">
                    <li><b>${h.toStatus}</b> <span class="muted small">${f:dateTime(h.changedAt)} &middot;
                        <c:out value="${empty h.changedBy ? 'SwipeHire' : h.changedBy.fullName}"/></span>
                        <c:if test="${not empty h.note}"><div class="small"><c:out value="${h.note}"/></div></c:if></li>
                </c:forEach>
            </ul>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
