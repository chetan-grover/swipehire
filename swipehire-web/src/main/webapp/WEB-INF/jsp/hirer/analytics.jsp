<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Analytics"/><c:set var="activeNav" value="dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<p><a href="${ctx}/hirer/dashboard">&larr; Dashboard</a></p>
<h1>Analytics: <c:out value="${job.title}"/></h1>

<section class="stats">
    <c:forEach var="s" items="${funnel}"><div class="stat"><b>${s.value}</b><span>${s.key}</span></div></c:forEach>
</section>

<div class="grid two">
    <div class="card">
        <h2 style="margin-top:0">Hiring funnel</h2>
        <img class="chart" src="${ctx}/files/chart?type=funnel&jobId=${job.id}" alt="Funnel chart">
    </div>
    <div class="card">
        <h2 style="margin-top:0">Skill gap in your applicant pool</h2>
        <p class="muted small">Share of applicants who liked this job and have each skill. Low bars = rare skills; consider
            making them optional or training for them.</p>
        <img class="chart" src="${ctx}/files/chart?type=skillgap&jobId=${job.id}" alt="Skill gap chart">
    </div>
</div>
<p><a class="btn ghost" href="${ctx}/files/export/pipeline.csv?jobId=${job.id}">&#11015; Export pipeline as CSV</a></p>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
