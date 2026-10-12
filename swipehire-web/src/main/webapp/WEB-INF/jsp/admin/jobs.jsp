<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Jobs"/><c:set var="activeNav" value="jobs"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Jobs</h1>
<form class="card row" method="get" action="${ctx}/admin/jobs">
    <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Title or company" style="flex:2">
    <select name="status" style="flex:1"><option value="">Any status</option>
        <c:forEach var="s" items="${statuses}"><option value="${s}" ${s == status ? 'selected' : ''}>${s}</option></c:forEach></select>
    <button class="btn">Search</button>
</form>
<div class="table-wrap">
    <table>
        <tr><th>Title</th><th>Company</th><th>Status</th><th>Deadline</th><th></th></tr>
        <c:forEach var="j" items="${jobs}">
            <tr>
                <td><c:out value="${j.title}"/></td><td><c:out value="${j.company.name}"/></td>
                <td><span class="status ${j.status}">${j.status}</span></td><td class="small">${f:date(j.deadline)}</td>
                <td><c:if test="${j.status != 'CLOSED'}"><form method="post" action="${ctx}/admin/jobs/${j.id}/close" onsubmit="return confirm('Close this job?')">
                    <sh:csrf/><button class="btn small ghost">Close</button></form></c:if></td>
            </tr>
        </c:forEach>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
