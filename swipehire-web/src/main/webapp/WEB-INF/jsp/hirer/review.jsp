<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${source == 'QUEUE' ? 'Queue' : 'Discover'}"/><c:set var="activeNav" value="dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<p><a href="${ctx}/hirer/dashboard">&larr; Dashboard</a></p>
<div class="row spread">
    <h1 style="margin:0"><c:out value="${job.title}"/></h1>
    <div class="row">
        <a class="btn small ${source == 'QUEUE' ? '' : 'ghost'}" href="${ctx}/hirer/jobs/${job.id}/queue">Queue (liked this job)</a>
        <a class="btn small ${source == 'DISCOVER' ? '' : 'ghost'}" href="${ctx}/hirer/jobs/${job.id}/discover">Discover</a>
        <a class="btn small ghost" href="${ctx}/hirer/jobs/${job.id}/applications">Pipeline</a>
    </div>
</div>
<p class="muted">
    <c:choose>
        <c:when test="${source == 'QUEUE'}">Applicants who already liked this job. Super-likes are pinned on top, then by match score. Like them back to match.</c:when>
        <c:otherwise>Opted-in applicants who haven't seen this job yet, ranked by match score. If you like them, they'll see that your company is interested.</c:otherwise>
    </c:choose>
</p>

<div class="deck-wrap">
    <section>
        <c:choose>
            <c:when test="${empty card}">
                <div class="card empty"><div class="big">&#9989;</div>
                    <h2>${source == 'QUEUE' ? 'Queue is empty' : 'No more candidates to discover'}</h2>
                    <p class="muted">${source == 'QUEUE' ? 'New likes appear here as applicants swipe.' : 'Check back later as new applicants join.'}</p></div>
            </c:when>
            <c:otherwise>
                <c:set var="p" value="${card.profile}"/>
                <div class="deck">
                    <c:if test="${fn:length(cards) > 1}"><div class="ghost-card"></div></c:if>
                    <article class="swipe-card" id="swipe-card" data-right="form-right" data-left="form-left">
                        <span class="stamp like">LIKE</span><span class="stamp nope">PASS</span>
                        <div class="card-head">
                            <div class="row" style="align-items:flex-start">
                                <c:choose>
                                    <c:when test="${not empty p.photoPath}"><img class="avatar" src="${ctx}/files/photo?userId=${p.userId}" alt=""></c:when>
                                    <c:otherwise><span class="avatar">${f:initials(p.user.fullName)}</span></c:otherwise>
                                </c:choose>
                                <div>
                                    <h1 style="margin:0"><c:out value="${p.user.fullName}"/></h1>
                                    <div class="muted"><c:out value="${p.headline}"/></div>
                                    <div class="row small" style="margin-top:4px">
                                        <c:if test="${p.collegeVerified}"><span class="badge verified">&#10003; <c:out value="${p.college.name}"/></span></c:if>
                                        <c:if test="${card.superSwipe}"><span class="badge super">&#9733; Super-liked this job</span></c:if>
                                    </div>
                                </div>
                            </div>
                            <div class="score-ring" style="--p:${card.score.totalPercent}">${card.score.totalPercent}%</div>
                        </div>
                        <c:if test="${not empty card.pitch}">
                            <blockquote class="card quote">&ldquo;<c:out value="${card.pitch}"/>&rdquo;</blockquote>
                        </c:if>
                        <div class="facts">
                            <span>&#127891; <c:out value="${p.degree}"/> <c:if test="${p.gradYear != null}">&middot; Class of ${p.gradYear}</c:if></span>
                            <span>&#128205; <c:out value="${p.city}"/></span>
                        </div>
                        <h3>Skills vs this job</h3>
                        <sh:skillChips matched="${card.matchedSkills}" missing="${card.missingRequired}"/>
                        <sh:scoreBars s="${card.score}"/>
                        <c:if test="${not empty card.topItems}">
                            <h3>Highlights</h3>
                            <ul style="margin:0;padding-left:18px">
                                <c:forEach var="it" items="${card.topItems}"><li><b><c:out value="${it.title}"/></b> <span class="muted small">${f:title(it.itemType)}</span></li></c:forEach>
                            </ul>
                        </c:if>
                        <div class="actions">
                            <form id="form-left" method="post" action="${ctx}/hirer/jobs/${job.id}/swipe" style="flex:1;display:flex">
                                <sh:csrf/><input type="hidden" name="applicantId" value="${p.userId}"><input type="hidden" name="direction" value="LEFT"><input type="hidden" name="source" value="${source}">
                                <button class="btn ghost big" style="flex:1">&#10007; Pass</button></form>
                            <form id="form-right" method="post" action="${ctx}/hirer/jobs/${job.id}/swipe" style="flex:1;display:flex">
                                <sh:csrf/><input type="hidden" name="applicantId" value="${p.userId}"><input type="hidden" name="direction" value="RIGHT"><input type="hidden" name="source" value="${source}">
                                <button class="btn green big" style="flex:1">&#10084; Like</button></form>
                        </div>
                    </article>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
    <aside>
        <div class="card">
            <h3>${fn:length(cards)} candidate(s)</h3>
            <c:forEach var="c2" items="${cards}" varStatus="st">
                <div class="row spread small" style="padding:4px 0;border-bottom:1px solid var(--line)">
                    <span>${st.count}. <c:out value="${c2.profile.user.fullName}"/> <c:if test="${c2.superSwipe}">&#9733;</c:if></span>
                    <b>${c2.score.totalPercent}%</b>
                </div>
            </c:forEach>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
