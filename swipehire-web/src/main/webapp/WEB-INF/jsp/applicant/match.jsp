<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Match"/><c:set var="activeNav" value="matches"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<p><a href="${ctx}/applicant/matches">&larr; My matches</a></p>
<div class="row spread">
    <div>
        <div class="company"><c:out value="${job.company.name}"/>
            <c:if test="${job.company.verified}"><span class="badge verified">&#10003; Verified company</span></c:if></div>
        <h1 style="margin:4px 0"><c:out value="${job.title}"/></h1>
    </div>
    <span class="status ${app.status}" style="font-size:1rem">${app.status}</span>
</div>

<div class="grid two" style="margin-top:14px">
    <div>
        <div class="card">
            <c:choose>
                <c:when test="${app.status == 'MATCHED'}">
                    <h2 style="margin-top:0">One-click apply</h2>
                    <p class="muted">Your resume was tailored for this job: the most relevant projects and experience come first and
                        matching skills are in bold. Review it, then apply.</p>
                    <div class="row">
                        <a class="btn ghost" href="${ctx}/files/resume?applicationId=${app.id}" target="_blank">&#128196; View tailored resume</a>
                        <form method="post" action="${ctx}/applicant/matches/${app.id}/apply" class="inline"><sh:csrf/><button class="btn clay big">Apply now</button></form>
                    </div>
                    <p class="muted small">This match expires 7 days after it was made (${f:date(app.statusChangedAt)}) if you don't apply.</p>
                </c:when>
                <c:otherwise>
                    <h2 style="margin-top:0">Your application</h2>
                    <p>
                        <a class="btn ghost" href="${ctx}/files/resume?applicationId=${app.id}" target="_blank">&#128196; Tailored resume (PDF)</a>
                        <c:if test="${not empty app.appliedAt}"><span class="muted small">Applied ${f:dateTime(app.appliedAt)}</span></c:if>
                    </p>
                </c:otherwise>
            </c:choose>
            <c:if test="${not pdfReady}"><p class="muted small">The PDF is generated in the background; it will be created on first open if it isn't ready yet.</p></c:if>
            <form method="post" action="${ctx}/applicant/matches/${app.id}/regenerate" class="inline">
                <sh:csrf/><button class="linkbtn" style="color:var(--sage-deep)">Regenerate from my latest profile</button></form>
        </div>

        <c:if test="${app.status == 'INTERVIEW'}">
            <div class="card">
                <h2 style="margin-top:0">Interview</h2>
                <c:choose>
                    <c:when test="${empty slots}"><p class="muted">The recruiter will propose interview times here.</p></c:when>
                    <c:otherwise>
                        <c:forEach var="s" items="${slots}">
                            <div class="row spread" style="padding:6px 0;border-bottom:1px solid var(--line)">
                                <div><b>${f:dateTime(s.slotStart)}</b> <span class="muted small">(${s.durationMin} min)</span>
                                    <div class="muted small"><c:out value="${s.location}"/></div></div>
                                <c:choose>
                                    <c:when test="${s.chosen}"><span class="badge verified">&#10003; Confirmed</span></c:when>
                                    <c:otherwise><form method="post" action="${ctx}/applicant/matches/${app.id}/slots/${s.id}"><sh:csrf/><button class="btn small">Pick this</button></form></c:otherwise>
                                </c:choose>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="card">
            <h2 style="margin-top:0">Why you matched (${score.totalPercent}%)</h2>
            <sh:skillChips matched="${matchedSkills}" missing="${missingSkills}"/>
            <sh:scoreBars s="${score}"/>
        </div>

        <div class="card">
            <h2 style="margin-top:0">What went into your tailored resume</h2>
            <p class="muted small">Your own entries, in the order they appear in the PDF, with how relevant each is to this job.</p>
            <table>
                <tr><th>#</th><th>Entry</th><th>Relevance</th></tr>
                <c:forEach var="t" items="${tailoredItems}" varStatus="st">
                    <tr><td>${st.count}</td><td><span class="chip">${f:title(t.item().type)}</span> <c:out value="${t.item().title}"/></td>
                        <td>${f:pct(t.relevance())}%</td></tr>
                </c:forEach>
            </table>
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
            <c:if test="${not app.status.terminal}">
                <form method="post" action="${ctx}/applicant/matches/${app.id}/withdraw" onsubmit="return confirm('Withdraw this application?')" style="margin-top:10px">
                    <sh:csrf/><button class="btn small ghost">Withdraw</button></form>
            </c:if>
        </div>
        <div class="card">
            <h3>About the job</h3>
            <div class="facts">
                <span>${f:title(job.workMode)}<c:if test="${not empty job.city}"> &middot; <c:out value="${job.city}"/></c:if></span>
                <span>${f:money(job.stipendMin)} - ${f:money(job.stipendMax)} / month</span>
            </div>
            <p class="desc"><c:out value="${job.description}"/></p>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
