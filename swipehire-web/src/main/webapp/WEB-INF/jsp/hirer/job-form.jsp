<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${empty job ? 'Post a job' : 'Edit job'}"/><c:set var="activeNav" value="newjob"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>${empty job ? 'Post a job' : 'Edit job'}</h1>
<form method="post" action="${ctx}/hirer/jobs${empty job ? '' : '/'.concat(job.id)}">
    <sh:csrf/>
    <div class="grid two">
        <div class="card">
            <h2 style="margin-top:0">The role</h2>
            <label>Title</label><input type="text" name="title" required maxlength="160" value="<c:out value='${form.title}'/>">
            <label>Description</label>
            <textarea name="description" required style="min-height:160px" placeholder="What will they work on? Which tools? The text is also used for TF-IDF matching."><c:out value="${form.description}"/></textarea>
            <div class="form-grid">
                <div><label>Type</label><select name="jobType"><c:forEach var="t" items="${jobTypes}"><option value="${t}" ${t == form.jobType ? 'selected' : ''}>${f:title(t)}</option></c:forEach></select></div>
                <div><label>Work mode</label><select name="workMode"><c:forEach var="m" items="${workModes}"><option value="${m}" ${m == form.workMode ? 'selected' : ''}>${f:title(m)}</option></c:forEach></select></div>
                <div><label>City (not needed for remote)</label><input type="text" name="city" maxlength="80" value="<c:out value='${form.city}'/>"></div>
                <div><label>Openings</label><input type="number" name="openings" min="1" value="${form.openings}"></div>
                <div><label>Stipend from (&#8377;/month)</label><input type="number" name="stipendMin" min="0" step="1000" value="${form.stipendMin}"></div>
                <div><label>Stipend to (&#8377;/month)</label><input type="number" name="stipendMax" min="0" step="1000" value="${form.stipendMax}"></div>
                <div><label>Application deadline</label><input type="date" name="deadline" required value="${form.deadline}"></div>
            </div>

            <h2>Who can see it</h2>
            <div class="form-grid">
                <div><label>Graduating from (year)</label><input type="number" name="minGradYear" min="1990" max="2040" value="${form.minGradYear}"></div>
                <div><label>Graduating until (year)</label><input type="number" name="maxGradYear" min="1990" max="2040" value="${form.maxGradYear}"></div>
            </div>
            <label class="check"><input type="checkbox" name="verifiedOnly" value="true" ${form.verifiedOnly ? 'checked' : ''}> Verified students only</label>
            <input type="hidden" name="_verifiedOnly" value="on">
            <label>Only these colleges (optional; leave all unticked for everyone)</label>
            <div class="chips">
                <c:forEach var="co" items="${colleges}">
                    <c:set var="picked" value="false"/>
                    <c:forEach var="id" items="${form.allowedCollegeIds}"><c:if test="${id == co.id}"><c:set var="picked" value="true"/></c:if></c:forEach>
                    <label class="check chip" style="margin:0"><input type="checkbox" name="allowedCollegeIds" value="${co.id}" ${picked ? 'checked' : ''}><c:out value="${co.name}"/></label>
                </c:forEach>
            </div>
            <c:if test="${empty job}">
                <label class="check" style="margin-top:14px"><input type="checkbox" name="publish" value="true" checked> Publish immediately (otherwise saved as draft)</label>
                <input type="hidden" name="_publish" value="on">
            </c:if>
            <p><button class="btn clay big">${empty job ? 'Post job' : 'Save changes'}</button></p>
        </div>

        <div class="card">
            <h2 style="margin-top:0">Skills</h2>
            <p class="muted small">Tick the skills this job needs. <b>Required</b> skills make up 45% of the match score; the
                <b>weight</b> (1-5) says how important each one is.</p>
            <div style="max-height:720px;overflow-y:auto">
                <table>
                    <tr><th>Use</th><th>Skill</th><th>Required</th><th>Weight</th></tr>
                    <c:forEach var="cat" items="${skillsByCategory}">
                        <tr><td colspan="4" class="muted small group">${cat.key}</td></tr>
                        <c:forEach var="sk" items="${cat.value}">
                            <c:set var="on" value="false"/><c:set var="req" value="false"/>
                            <c:forEach var="id" items="${form.skillIds}"><c:if test="${id == sk.id}"><c:set var="on" value="true"/></c:if></c:forEach>
                            <c:forEach var="id" items="${form.requiredSkillIds}"><c:if test="${id == sk.id}"><c:set var="req" value="true"/></c:if></c:forEach>
                            <c:set var="w" value="${empty form.weights[sk.id] ? 3 : form.weights[sk.id]}"/>
                            <tr>
                                <td><input type="checkbox" name="skillIds" value="${sk.id}" ${on ? 'checked' : ''}></td>
                                <td><c:out value="${sk.name}"/></td>
                                <td><input type="checkbox" name="requiredSkillIds" value="${sk.id}" ${req ? 'checked' : ''}></td>
                                <td><select name="weight_${sk.id}" style="width:auto">
                                    <c:forEach begin="1" end="5" var="n"><option value="${n}" ${n == w ? 'selected' : ''}>${n}</option></c:forEach>
                                </select></td>
                            </tr>
                        </c:forEach>
                    </c:forEach>
                </table>
            </div>
        </div>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
