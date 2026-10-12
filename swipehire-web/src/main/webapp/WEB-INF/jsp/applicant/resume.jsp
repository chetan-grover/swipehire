<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Resume"/><c:set var="activeNav" value="resume"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Structured resume</h1>
<p class="muted">Each entry is a building block. When you match, SwipeHire picks the entries most relevant to that job,
    puts them first and bolds the matching skills. It never writes anything for you.</p>

<div class="grid two">
    <div>
        <c:if test="${empty items}"><div class="card empty"><div class="big">&#128221;</div><p class="muted">No entries yet. Add your projects, internships and achievements.</p></div></c:if>
        <c:forEach var="it" items="${items}">
            <div class="card">
                <div class="row spread">
                    <div>
                        <span class="chip">${f:title(it.itemType)}</span>
                        <h3 style="display:inline;margin-left:6px"><c:out value="${it.title}"/></h3>
                    </div>
                    <form method="post" action="${ctx}/applicant/resume/${it.id}/delete" onsubmit="return confirm('Delete this entry?')">
                        <sh:csrf/><button class="btn small ghost">Delete</button>
                    </form>
                </div>
                <div class="muted small"><c:out value="${it.organization}"/> ${empty it.organization ? '' : '&middot;'} ${it.dateRange}</div>
                <c:if test="${not empty it.description}"><p style="margin:8px 0"><c:out value="${it.description}"/></p></c:if>
                <div class="chips"><c:forEach var="s" items="${it.skills}"><span class="chip"><c:out value="${s.name}"/></span></c:forEach></div>
            </div>
        </c:forEach>
    </div>

    <form class="card" method="post" action="${ctx}/applicant/resume">
        <sh:csrf/>
        <h2 style="margin-top:0">Add an entry</h2>
        <div class="form-grid">
            <div><label>Type</label>
                <select name="itemType"><c:forEach var="t" items="${itemTypes}"><option value="${t}" ${t == 'PROJECT' ? 'selected' : ''}>${f:title(t)}</option></c:forEach></select></div>
            <div><label>Organisation (optional)</label><input type="text" name="organization" maxlength="160"></div>
        </div>
        <label>Title</label><input type="text" name="title" required maxlength="160" placeholder="e.g. Campus Canteen Ordering System">
        <div class="form-grid">
            <div><label>Start date</label><input type="date" name="startDate"></div>
            <div><label>End date (empty = ongoing)</label><input type="date" name="endDate"></div>
        </div>
        <label>What did you do? (your own words)</label>
        <textarea name="description" maxlength="2000" placeholder="Built a Spring Boot REST API with MySQL..."></textarea>
        <label>Skills used in this entry</label>
        <div style="max-height:220px;overflow-y:auto;border:1px solid var(--line);border-radius:9px;padding:6px 10px">
            <c:forEach var="cat" items="${skillsByCategory}">
                <div class="muted small" style="margin-top:6px">${cat.key}</div>
                <div class="chips">
                    <c:forEach var="sk" items="${cat.value}">
                        <label class="check chip" style="margin:0"><input type="checkbox" name="skillIds" value="${sk.id}"><c:out value="${sk.name}"/></label>
                    </c:forEach>
                </div>
            </c:forEach>
        </div>
        <p><button class="btn">Add entry</button></p>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
