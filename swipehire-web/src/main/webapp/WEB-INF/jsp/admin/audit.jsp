<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Audit trail"/><c:set var="activeNav" value="audit"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Application audit trail</h1>
<p class="muted">The latest 100 rows of <code>application_status_history</code>. Every status change on the platform is recorded here.</p>
<div class="table-wrap">
    <table>
        <tr><th>When</th><th>Applicant</th><th>Job</th><th>Change</th><th>By</th><th>Note</th></tr>
        <c:forEach var="h" items="${rows}">
            <tr>
                <td class="small">${f:dateTime(h.changedAt)}</td>
                <td><c:out value="${h.application.match.applicant.user.fullName}"/></td>
                <td><c:out value="${h.application.match.job.title}"/></td>
                <td><span class="muted">${empty h.fromStatus ? '-' : h.fromStatus}</span> &rarr; <span class="status ${h.toStatus}">${h.toStatus}</span></td>
                <td><c:out value="${empty h.changedBy ? 'System' : h.changedBy.fullName}"/></td>
                <td class="small"><c:out value="${h.note}"/></td>
            </tr>
        </c:forEach>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
