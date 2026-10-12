<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Colleges"/><c:set var="activeNav" value="colleges"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Colleges</h1>
<div class="grid two">
    <div>
        <h2>Requests (${fn:length(pending)})</h2>
        <c:if test="${empty pending}"><p class="muted">No pending requests.</p></c:if>
        <c:forEach var="co" items="${pending}">
            <div class="card row spread">
                <div><b><c:out value="${co.name}"/></b><div class="muted small">@<c:out value="${co.emailDomain}"/> &middot; <c:out value="${co.city}"/></div></div>
                <div class="row">
                    <form method="post" action="${ctx}/admin/colleges/${co.id}"><sh:csrf/><input type="hidden" name="status" value="VERIFIED"><button class="btn green small">Approve</button></form>
                    <form method="post" action="${ctx}/admin/colleges/${co.id}"><sh:csrf/><input type="hidden" name="status" value="REJECTED"><button class="btn red small">Reject</button></form>
                </div>
            </div>
        </c:forEach>
        <form class="card" method="post" action="${ctx}/admin/colleges">
            <sh:csrf/>
            <h3>Add a college</h3>
            <label>Name</label><input type="text" name="name" required maxlength="150">
            <div class="form-grid">
                <div><label>Email domain</label><input type="text" name="domain" required placeholder="college.ac.in"></div>
                <div><label>City</label><input type="text" name="city" maxlength="80"></div>
            </div>
            <p><button class="btn">Add</button></p>
        </form>
    </div>
    <div>
        <h2>Verified (${fn:length(verified)})</h2>
        <div class="table-wrap"><table>
            <tr><th>College</th><th>Domain</th></tr>
            <c:forEach var="co" items="${verified}"><tr><td><c:out value="${co.name}"/></td><td class="muted">@<c:out value="${co.emailDomain}"/></td></tr></c:forEach>
        </table></div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
