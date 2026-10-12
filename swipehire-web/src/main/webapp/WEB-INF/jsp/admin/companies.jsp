<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Companies"/><c:set var="activeNav" value="companies"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Company verification</h1>
<h2>Pending (${fn:length(pending)})</h2>
<c:if test="${empty pending}"><p class="muted">Nothing to review.</p></c:if>
<c:forEach var="co" items="${pending}">
    <div class="card row spread">
        <div>
            <h3><c:out value="${co.name}"/></h3>
            <div class="muted small"><c:out value="${co.city}"/> &middot; <c:out value="${co.website}"/> &middot; registered ${f:date(co.createdAt)}</div>
            <c:if test="${not empty co.description}"><p class="small"><c:out value="${co.description}"/></p></c:if>
        </div>
        <div class="row">
            <form method="post" action="${ctx}/admin/companies/${co.id}"><sh:csrf/><input type="hidden" name="status" value="VERIFIED"><button class="btn green small">Verify</button></form>
            <form method="post" action="${ctx}/admin/companies/${co.id}"><sh:csrf/><input type="hidden" name="status" value="REJECTED"><button class="btn red small">Reject</button></form>
        </div>
    </div>
</c:forEach>

<h2>All companies</h2>
<div class="table-wrap">
    <table>
        <tr><th>Name</th><th>City</th><th>Status</th><th>Since</th></tr>
        <c:forEach var="co" items="${all}">
            <tr><td><c:out value="${co.name}"/></td><td><c:out value="${co.city}"/></td>
                <td><span class="badge ${co.verified ? 'verified' : 'pending'}">${co.status}</span></td><td class="small">${f:date(co.createdAt)}</td></tr>
        </c:forEach>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
