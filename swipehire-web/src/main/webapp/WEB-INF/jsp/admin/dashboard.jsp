<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Admin"/><c:set var="activeNav" value="admin"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Platform overview</h1>
<section class="stats">
    <c:forEach var="s" items="${stats}"><div class="stat"><b>${s.value}</b><span>${s.key}</span></div></c:forEach>
    <div class="stat"><b>${onlineUsers}</b><span>Users online (WebSocket)</span></div>
</section>

<div class="grid two">
    <div class="card">
        <h2 style="margin-top:0">Match engine (RMI)</h2>
        <table>
            <tr><td>Endpoint</td><td><code>${engine.endpoint}</code></td></tr>
            <tr><td>Ping</td><td><span class="${fn:startsWith(enginePing, 'UNREACHABLE') ? 'chip miss' : 'chip ok'}"><c:out value="${enginePing}"/></span></td></tr>
            <tr><td>Last ranking ran on</td><td>${engine.lastMode} (${engine.lastLatencyMs} ms)</td></tr>
            <tr><td>Calls via RMI / local fallback</td><td>${engine.remoteCalls} / ${engine.degradedCalls}</td></tr>
        </table>
        <p class="muted small">If the engine JVM is stopped, ranking falls back to the same scorer inside the web app ("degraded mode"), so users never see an error.</p>
    </div>
    <div class="card">
        <h2 style="margin-top:0">Needs attention</h2>
        <p><a href="${ctx}/admin/companies">${pendingCompanies} company verification(s) pending</a></p>
        <p><a href="${ctx}/admin/colleges">${pendingColleges} college request(s) pending</a></p>
        <form method="post" action="${ctx}/admin/run-maintenance"><sh:csrf/>
            <button class="btn ghost small">Run hourly maintenance now (expiry, warnings, OTP cleanup)</button></form>
        <form method="post" action="${ctx}/admin/demo-data" style="margin-top:8px" onsubmit="return confirm('Add 100 demo applicants, 30 jobs and swipes?')"><sh:csrf/>
            <button class="btn ghost small">Generate demo data (JDBC batch insert)</button></form>
    </div>
</div>

<div class="card">
    <h2 style="margin-top:0">Activity</h2>
    <img class="chart" src="${ctx}/files/chart?type=activity" alt="Daily swipes and matches">
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
