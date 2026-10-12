<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Insights"/><c:set var="activeNav" value="insights"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Your insights</h1>
<section class="stats">
    <c:forEach var="s" items="${stats}"><div class="stat"><b>${s.value}</b><span>${s.key}</span></div></c:forEach>
</section>

<div class="card">
    <h2 style="margin-top:0">What should I learn next?</h2>
    <c:choose>
        <c:when test="${empty missing}">
            <p class="muted">Like a few jobs first. We'll then show which required skills come up most often among the jobs you liked but don't have yet.</p>
        </c:when>
        <c:otherwise>
            <p class="muted">Required skills you're missing, counted over the jobs you liked. Learning the top one helps the most.</p>
            <img class="chart" src="${ctx}/files/chart?type=missing" alt="Bar chart of missing skills">
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
