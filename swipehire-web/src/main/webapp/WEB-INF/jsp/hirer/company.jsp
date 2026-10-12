<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Company"/><c:set var="activeNav" value="company"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="narrow">
    <h1><c:out value="${company.name}"/></h1>
    <p>
        <c:choose>
            <c:when test="${company.verified}"><span class="badge verified">&#10003; Verified on ${f:date(company.verifiedAt)}</span></c:when>
            <c:otherwise><span class="badge pending">${company.status}</span> <span class="muted small">An admin reviews new companies.</span></c:otherwise>
        </c:choose>
    </p>
    <form class="card" method="post" action="${ctx}/hirer/company">
        <sh:csrf/>
        <label>Website</label><input type="text" name="website" value="<c:out value='${company.website}'/>" maxlength="200">
        <label>City</label><input type="text" name="city" value="<c:out value='${company.city}'/>" maxlength="80">
        <label>About the company (shown to applicants)</label>
        <textarea name="description" maxlength="2000"><c:out value="${company.description}"/></textarea>
        <p><button class="btn">Save</button></p>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
