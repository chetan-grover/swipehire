<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Profile"/><c:set var="activeNav" value="profile"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>Your profile</h1>
<div class="grid two">
    <div>
        <form class="card" method="post" action="${ctx}/applicant/profile">
            <sh:csrf/>
            <h2 style="margin-top:0">About you</h2>
            <div class="form-grid">
                <div><label>Full name</label><input type="text" name="fullName" value="<c:out value='${form.fullName}'/>" maxlength="120"></div>
                <div><label>Phone</label><input type="text" name="phone" value="<c:out value='${form.phone}'/>" maxlength="20"></div>
            </div>
            <label>Headline</label>
            <input type="text" name="headline" value="<c:out value='${form.headline}'/>" maxlength="160" placeholder="e.g. Java backend developer in the making">
            <label>About</label>
            <textarea name="about" maxlength="2000"><c:out value="${form.about}"/></textarea>
            <div class="form-grid">
                <div><label>Degree</label><input type="text" name="degree" value="<c:out value='${form.degree}'/>" maxlength="100"></div>
                <div><label>Graduation year</label><input type="number" name="gradYear" value="${form.gradYear}" min="1990" max="2040"></div>
                <div><label>Current city</label><input type="text" name="city" value="<c:out value='${form.city}'/>" maxlength="80"></div>
            </div>

            <h2>What you're looking for</h2>
            <div class="form-grid">
                <div><label>Work mode</label>
                    <select name="prefWorkMode">
                        <c:forEach var="m" items="${workModes}"><option value="${m}" ${m == form.prefWorkMode ? 'selected' : ''}>${f:title(m)}</option></c:forEach>
                    </select></div>
                <div><label>Job type</label>
                    <select name="prefJobType">
                        <c:forEach var="t" items="${jobTypes}"><option value="${t}" ${t == form.prefJobType ? 'selected' : ''}>${f:title(t)}</option></c:forEach>
                    </select></div>
                <div><label>Minimum stipend (&#8377;/month)</label><input type="number" name="minStipend" value="${form.minStipend}" min="0" step="1000"></div>
            </div>
            <label>Preferred cities (comma separated)</label>
            <input type="text" name="preferredCities" value="<c:out value='${form.preferredCities}'/>" placeholder="Bengaluru, Pune">
            <label class="check"><input type="checkbox" name="discoverable" value="true" ${form.discoverable ? 'checked' : ''}>
                Let recruiters discover my profile (they may like you first)</label>
            <input type="hidden" name="_discoverable" value="on">
            <p><button class="btn">Save profile</button></p>
        </form>
    </div>

    <div>
        <div class="card">
            <div class="row">
                <c:choose>
                    <c:when test="${not empty profile.photoPath}"><img class="avatar" src="${ctx}/files/photo?userId=${profile.userId}" alt="Profile photo"></c:when>
                    <c:otherwise><span class="avatar">${f:initials(profile.user.fullName)}</span></c:otherwise>
                </c:choose>
                <div>
                    <b><c:out value="${profile.user.fullName}"/></b>
                    <c:choose>
                        <c:when test="${profile.collegeVerified}"><div><span class="badge verified">&#10003; Verified student</span> <c:out value="${profile.college.name}"/></div></c:when>
                        <c:otherwise><div><a href="${ctx}/applicant/verify" class="small">Verify your college</a></div></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <%-- Multipart upload handled by the raw PhotoServlet; the CSRF token goes in the URL because
                 the multipart body isn't parsed before Spring Security checks the token. --%>
            <form method="post" action="${ctx}/files/photo?${_csrf.parameterName}=${_csrf.token}" enctype="multipart/form-data" style="margin-top:12px">
                <label>Profile photo (JPEG/PNG, max 2 MB)</label>
                <div class="row"><input type="file" name="photo" accept="image/jpeg,image/png" required><button class="btn small">Upload</button></div>
            </form>
        </div>
        <div class="card">
            <h3>Profile checklist</h3>
            <ul class="checklist">
                <li>${checklist.basics ? '&#9989;' : '&#11036;'} Headline and graduation year</li>
                <li>${checklist.skills ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/skills">At least one skill</a></li>
                <li>${checklist.resume ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/resume">At least one project or experience</a></li>
                <li>${checklist.verified ? '&#9989;' : '&#11036;'} <a href="${ctx}/applicant/verify">Verified college email</a></li>
            </ul>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
