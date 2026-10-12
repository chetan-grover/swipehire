<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Skills"/><c:set var="activeNav" value="skills"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Your skills</h1>
<div class="grid two">
    <div class="card">
        <h2 style="margin-top:0">Current skills</h2>
        <c:choose>
            <c:when test="${empty profile.skills}"><p class="muted">No skills yet. Add some on the right, or import them from your resume.</p></c:when>
            <c:otherwise>
                <table>
                    <tr><th>Skill</th><th>Proficiency (1-5)</th><th></th></tr>
                    <c:forEach var="s" items="${profile.skills}">
                        <tr>
                            <td><c:out value="${s.skill.name}"/> <span class="muted small">${s.skill.category}</span></td>
                            <td>
                                <form method="post" action="${ctx}/applicant/skills" class="row">
                                    <sh:csrf/><input type="hidden" name="skillId" value="${s.skill.id}">
                                    <select name="proficiency" style="width:auto">
                                        <c:forEach begin="1" end="5" var="p"><option value="${p}" ${p == s.proficiency ? 'selected' : ''}>${p}</option></c:forEach>
                                    </select>
                                    <button class="btn small ghost">Update</button>
                                </form>
                            </td>
                            <td><form method="post" action="${ctx}/applicant/skills/${s.skill.id}/remove"><sh:csrf/><button class="btn small ghost">Remove</button></form></td>
                        </tr>
                    </c:forEach>
                </table>
                <p class="muted small">3 or above counts as fully covering a job's requirement.</p>
            </c:otherwise>
        </c:choose>
    </div>

    <div>
        <form class="card" method="post" action="${ctx}/applicant/skills">
            <sh:csrf/>
            <h2 style="margin-top:0">Add a skill</h2>
            <label>Skill</label>
            <select name="skillId" required>
                <c:forEach var="cat" items="${skillsByCategory}">
                    <optgroup label="${cat.key}">
                        <c:forEach var="sk" items="${cat.value}"><option value="${sk.id}"><c:out value="${sk.name}"/></option></c:forEach>
                    </optgroup>
                </c:forEach>
            </select>
            <label>Proficiency</label>
            <select name="proficiency">
                <option value="1">1 - just started</option><option value="2">2 - basic</option>
                <option value="3" selected>3 - comfortable</option><option value="4">4 - strong</option><option value="5">5 - expert</option>
            </select>
            <p><button class="btn">Add skill</button></p>
        </form>

        <form class="card" method="post" action="${ctx}/files/resume-import?${_csrf.parameterName}=${_csrf.token}" enctype="multipart/form-data">
            <h2 style="margin-top:0">Import from your PDF resume</h2>
            <p class="muted small">We read the text of your PDF (with Apache PDFBox) and suggest skills we recognise.
                You confirm each one; nothing is added automatically.</p>
            <input type="file" name="resume" accept="application/pdf" required>
            <p><button class="btn clay">Scan my resume</button></p>
            <c:if test="${not empty latestUpload}">
                <p class="muted small">Last upload: <c:out value="${latestUpload.fileName}"/> (${f:ago(latestUpload.uploadedAt)}).
                    Its text also improves your text-similarity score.</p>
            </c:if>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
