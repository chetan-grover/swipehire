<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Users"/><c:set var="activeNav" value="users"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Users</h1>
<form class="card row" method="get" action="${ctx}/admin/users">
    <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Name or email" style="flex:2">
    <select name="role" style="flex:1"><option value="">Any role</option>
        <c:forEach var="r" items="${roles}"><option value="${r}" ${r == role ? 'selected' : ''}>${r}</option></c:forEach></select>
    <select name="enabled" style="flex:1">
        <option value="">Any state</option>
        <option value="true" ${enabled == true ? 'selected' : ''}>Active</option>
        <option value="false" ${enabled == false ? 'selected' : ''}>Disabled</option>
    </select>
    <button class="btn">Search</button>
</form>
<p class="muted small">This search is built with the JPA Criteria API: only the filters you fill in become WHERE conditions.</p>
<div class="table-wrap">
    <table>
        <tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>State</th><th></th></tr>
        <c:forEach var="u" items="${users}">
            <tr>
                <td><c:out value="${u.fullName}"/></td><td><c:out value="${u.email}"/></td><td>${u.role}</td>
                <td class="small">${f:date(u.createdAt)}</td>
                <td><span class="chip ${u.enabled ? 'ok' : 'miss'}">${u.enabled ? 'Active' : 'Disabled'}</span></td>
                <td><c:if test="${u.id != me.id}"><form method="post" action="${ctx}/admin/users/${u.id}/toggle"><sh:csrf/>
                    <button class="btn small ${u.enabled ? 'ghost' : 'green'}">${u.enabled ? 'Disable' : 'Enable'}</button></form></c:if></td>
            </tr>
        </c:forEach>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
