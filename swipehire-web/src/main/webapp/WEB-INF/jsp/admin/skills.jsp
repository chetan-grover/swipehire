<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Skills"/><c:set var="activeNav" value="skills"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Skill dictionary</h1>
<div class="grid two">
    <div>
        <c:forEach var="cat" items="${skillsByCategory}">
            <div class="card"><h3>${cat.key}</h3>
                <div class="chips"><c:forEach var="s" items="${cat.value}"><span class="chip"><c:out value="${s.name}"/></span></c:forEach></div></div>
        </c:forEach>
    </div>
    <div>
        <form class="card" method="post" action="${ctx}/admin/skills">
            <sh:csrf/>
            <h3>Add a skill</h3>
            <div class="form-grid">
                <div><label>Name</label><input type="text" name="name" required maxlength="80"></div>
                <div><label>Category</label><input type="text" name="category" required maxlength="40" list="cats"></div>
            </div>
            <datalist id="cats"><c:forEach var="cat" items="${skillsByCategory}"><option value="${cat.key}"></c:forEach></datalist>
            <p><button class="btn">Add</button></p>
        </form>
        <div class="card">
            <h3>Synonyms (used by the PDF resume import)</h3>
            <form method="post" action="${ctx}/admin/synonyms" class="row">
                <sh:csrf/>
                <input type="text" name="alias" required placeholder="alias e.g. k8s" style="flex:1">
                <select name="skillId" style="flex:1"><c:forEach var="s" items="${skills}"><option value="${s.id}"><c:out value="${s.name}"/></option></c:forEach></select>
                <button class="btn small">Add</button>
            </form>
            <table style="margin-top:10px">
                <tr><th>Alias</th><th>Means</th><th></th></tr>
                <c:forEach var="syn" items="${synonyms}">
                    <tr><td><code><c:out value="${syn.alias}"/></code></td><td><c:out value="${syn.skill.name}"/></td>
                        <td><form method="post" action="${ctx}/admin/synonyms/delete"><sh:csrf/><input type="hidden" name="alias" value="<c:out value='${syn.alias}'/>"><button class="btn small ghost">Delete</button></form></td></tr>
                </c:forEach>
            </table>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
