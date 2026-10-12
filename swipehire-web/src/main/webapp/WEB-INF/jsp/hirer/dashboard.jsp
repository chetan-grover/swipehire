<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dashboard"/><c:set var="activeNav" value="dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="row spread">
    <div>
        <h1 style="margin-bottom:4px"><c:out value="${company.name}"/>
            <c:choose>
                <c:when test="${company.verified}"><span class="badge verified">&#10003; Verified company</span></c:when>
                <c:otherwise><span class="badge pending">Verification pending</span></c:otherwise>
            </c:choose>
        </h1>
        <p class="muted" style="margin:0"><c:out value="${hirer.designation}"/></p>
    </div>
    <a class="btn clay" href="${ctx}/hirer/jobs/new">+ Post a job</a>
</div>
<c:if test="${not company.verified}">
    <div class="alert err" style="margin-top:12px">Until an admin verifies <c:out value="${company.name}"/>, you can keep at most 2 jobs active.</div>
</c:if>

<section class="stats">
    <div class="stat"><b>${fn:length(jobs)}</b><span>Jobs</span></div>
    <div class="stat"><b>${responsiveness == null ? '-' : responsiveness.toString().concat('%')}</b><span>Responded within 7 days (shown on your job cards)</span></div>
    <div class="stat"><b><c:choose><c:when test="${avgResponseHours == null}">-</c:when><c:otherwise>${Math.round(avgResponseHours)} h</c:otherwise></c:choose></b><span>Average time to first response</span></div>
</section>

<c:choose>
    <c:when test="${empty jobs}">
        <div class="card empty"><div class="big">&#128188;</div><h2>Post your first job</h2>
            <p class="muted">Applicants who like it appear in your queue, ranked by an explainable match score.</p>
            <a class="btn clay" href="${ctx}/hirer/jobs/new">Post a job</a></div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table>
                <tr><th>Job</th><th>Status</th><th>Waiting for you</th><th>Matches</th><th>Deadline</th><th>Actions</th></tr>
                <c:forEach var="j" items="${jobs}">
                    <c:set var="cnt" value="${counters[j.id]}"/>
                    <tr>
                        <td><b><c:out value="${j.title}"/></b><div class="muted small">${f:title(j.jobType)} &middot; ${f:title(j.workMode)} <c:out value="${j.city}"/></div></td>
                        <td><span class="status ${j.status}">${j.status}</span></td>
                        <td><c:choose><c:when test="${cnt[0] > 0}"><a class="btn small clay" href="${ctx}/hirer/jobs/${j.id}/queue">${cnt[0]} to review</a></c:when><c:otherwise><span class="muted">0</span></c:otherwise></c:choose></td>
                        <td><a href="${ctx}/hirer/jobs/${j.id}/applications">${cnt[1]} match(es)</a></td>
                        <td class="small">${f:date(j.deadline)}</td>
                        <td>
                            <div class="row">
                                <a class="btn small" href="${ctx}/hirer/jobs/${j.id}/queue">Queue</a>
                                <a class="btn small ghost" href="${ctx}/hirer/jobs/${j.id}/discover">Discover</a>
                                <a class="btn small ghost" href="${ctx}/hirer/jobs/${j.id}/analytics">Analytics</a>
                                <a class="btn small ghost" href="${ctx}/hirer/jobs/${j.id}/edit">Edit</a>
                                <form method="post" action="${ctx}/hirer/jobs/${j.id}/status" class="inline">
                                    <sh:csrf/>
                                    <c:choose>
                                        <c:when test="${j.status == 'ACTIVE'}"><input type="hidden" name="status" value="PAUSED"><button class="btn small ghost">Pause</button></c:when>
                                        <c:when test="${j.status != 'CLOSED'}"><input type="hidden" name="status" value="ACTIVE"><button class="btn small green">Publish</button></c:when>
                                    </c:choose>
                                </form>
                                <c:if test="${j.status != 'CLOSED'}">
                                    <form method="post" action="${ctx}/hirer/jobs/${j.id}/status" class="inline" onsubmit="return confirm('Close this job for good?')">
                                        <sh:csrf/><input type="hidden" name="status" value="CLOSED"><button class="btn small ghost">Close</button></form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
