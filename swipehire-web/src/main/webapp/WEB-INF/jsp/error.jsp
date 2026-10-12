<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Something went wrong"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="card empty">
    <div class="big">${status == 404 ? '&#128269;' : status == 403 ? '&#128274;' : '&#9888;'}</div>
    <h1>
        <c:choose>
            <c:when test="${status == 404}">We couldn't find that page</c:when>
            <c:when test="${status == 403}">You don't have access to this page</c:when>
            <c:otherwise>Something went wrong</c:otherwise>
        </c:choose>
    </h1>
    <p class="muted"><c:out value="${empty message ? '' : message}"/></p>
    <p><a class="btn" href="${ctx}/">Back to home</a></p>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
