<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Review imported skills"/><c:set var="activeNav" value="skills"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="card narrow" style="margin:0 auto">
    <h1>Skills found in your resume</h1>
    <c:choose>
        <c:when test="${empty suggested}">
            <p class="muted">We didn't find any new dictionary skills in that PDF. You can still add skills by hand.</p>
            <a class="btn" href="${ctx}/applicant/skills">Back to skills</a>
        </c:when>
        <c:otherwise>
            <p class="muted">Untick anything that isn't right. Ticked skills are added with proficiency 3; you can change it afterwards.</p>
            <form method="post" action="${ctx}/applicant/skills/import-review">
                <sh:csrf/>
                <c:forEach var="s" items="${suggested}">
                    <label class="check"><input type="checkbox" name="skillIds" value="${s.id}" checked>
                        <c:out value="${s.name}"/> <span class="muted small">${s.category}</span></label>
                </c:forEach>
                <p class="row"><button class="btn clay">Add selected skills</button><a class="btn ghost" href="${ctx}/applicant/skills">Cancel</a></p>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
