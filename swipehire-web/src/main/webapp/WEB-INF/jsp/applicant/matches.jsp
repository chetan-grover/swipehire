<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My matches"/><c:set var="activeNav" value="matches"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>My matches</h1>
<c:choose>
    <c:when test="${empty applications}">
        <div class="card empty">
            <div class="big">&#128140;</div>
            <h2>No matches yet</h2>
            <p class="muted">A match happens when you like a job and the recruiter likes you back. Keep swiping!</p>
            <a class="btn clay" href="${ctx}/applicant/deck">Open my deck</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table>
                <tr><th>Job</th><th>Company</th><th>Status</th><th>Matched</th><th>Last update</th><th></th></tr>
                <c:forEach var="a" items="${applications}">
                    <tr>
                        <td><b><c:out value="${a.match.job.title}"/></b></td>
                        <td><c:out value="${a.match.job.company.name}"/></td>
                        <td><span class="status ${a.status}">${a.status}</span>
                            <c:if test="${a.status == 'MATCHED'}"><div class="small" style="color:var(--clay)">Apply within 7 days</div></c:if></td>
                        <td class="muted small">${f:date(a.match.matchedAt)}</td>
                        <td class="muted small">${f:ago(a.statusChangedAt)}</td>
                        <td><a class="btn small" href="${ctx}/applicant/matches/${a.id}">Open</a></td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
