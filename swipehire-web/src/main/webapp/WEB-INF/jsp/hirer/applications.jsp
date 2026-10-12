<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Pipeline"/><c:set var="activeNav" value="dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<p><a href="${ctx}/hirer/dashboard">&larr; Dashboard</a></p>
<div class="row spread">
    <h1 style="margin:0">Pipeline: <c:out value="${job.title}"/></h1>
    <a class="btn small ghost" href="${ctx}/files/export/pipeline.csv?jobId=${job.id}">&#11015; Export CSV</a>
</div>

<c:choose>
    <c:when test="${empty applications}">
        <div class="card empty" style="margin-top:16px"><div class="big">&#129309;</div><p class="muted">No matches yet. Review your <a href="${ctx}/hirer/jobs/${job.id}/queue">queue</a>.</p></div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap" style="margin-top:16px">
            <table>
                <tr><th>Applicant</th><th>Status</th><th>Matched</th><th>Applied</th><th>Last change</th><th></th></tr>
                <c:forEach var="a" items="${applications}">
                    <tr>
                        <td><b><c:out value="${a.match.applicant.user.fullName}"/></b>
                            <c:if test="${a.match.applicant.collegeVerified}"><span class="badge verified">&#10003;</span></c:if>
                            <div class="muted small"><c:out value="${a.match.applicant.headline}"/></div></td>
                        <td><span class="status ${a.status}">${a.status}</span>
                            <c:if test="${a.status == 'APPLIED'}"><div class="small" style="color:var(--clay)">Respond within 7 days</div></c:if></td>
                        <td class="small">${f:date(a.match.matchedAt)}</td>
                        <td class="small">${f:date(a.appliedAt)}</td>
                        <td class="small muted">${f:ago(a.statusChangedAt)}</td>
                        <td><a class="btn small" href="${ctx}/hirer/applications/${a.id}">Open</a></td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
