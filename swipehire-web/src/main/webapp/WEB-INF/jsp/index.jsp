<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value=""/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<section class="hero">
    <h1>Swipe<span>Hire</span></h1>
    <p>Mutual-match hiring for internships and fresher roles. You swipe on jobs, recruiters swipe on you,
        and only a match moves forward, with a resume tailored to that exact job.</p>
    <div class="row" style="margin-top:18px">
        <c:choose>
            <c:when test="${me.role == 'APPLICANT'}"><a class="btn big clay" href="${ctx}/applicant/deck">Open my deck</a></c:when>
            <c:when test="${me.role == 'HIRER'}"><a class="btn big clay" href="${ctx}/hirer/dashboard">Go to dashboard</a></c:when>
            <c:when test="${me.role == 'ADMIN'}"><a class="btn big clay" href="${ctx}/admin">Admin overview</a></c:when>
            <c:otherwise>
                <a class="btn big clay" href="${ctx}/signup?role=APPLICANT">I'm looking for a job</a>
                <a class="btn big" href="${ctx}/signup?role=HIRER">I'm hiring</a>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<section class="stats">
    <c:forEach var="s" items="${stats}">
        <div class="stat"><b>${s.value}</b><span>${s.key}</span></div>
    </c:forEach>
</section>

<section class="grid three">
    <div class="card">
        <h3>Less noise</h3>
        <p class="muted">25 likes and 3 super-likes a day. No mass-applying, so every application means something.</p>
    </div>
    <div class="card">
        <h3>Explainable matching</h3>
        <p class="muted">Every card shows its score breakdown and the skills you match or miss. No black box.</p>
    </div>
    <div class="card">
        <h3>One-click tailored apply</h3>
        <p class="muted">On a match we pick and reorder <em>your own</em> projects and skills for that job. Nothing is invented.</p>
    </div>
    <div class="card">
        <h3>No ghosting</h3>
        <p class="muted">Recruiters have 7 days to respond. Their responsiveness score is shown on every job card.</p>
    </div>
    <div class="card">
        <h3>Verified badges</h3>
        <p class="muted">Students verify their college email with an OTP; companies are verified by our admins.</p>
    </div>
    <div class="card">
        <h3>Real-time</h3>
        <p class="muted">Match alerts and chat arrive instantly over WebSockets.</p>
    </div>
</section>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
