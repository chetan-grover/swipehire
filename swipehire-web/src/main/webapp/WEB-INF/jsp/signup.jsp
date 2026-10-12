<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign up"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="narrow" style="margin:30px auto">
    <div class="card">
        <div class="row" style="margin-bottom:10px">
            <a class="btn small ${role == 'APPLICANT' ? '' : 'ghost'}" href="${ctx}/signup?role=APPLICANT">I'm looking for a job</a>
            <a class="btn small ${role == 'HIRER' ? '' : 'ghost'}" href="${ctx}/signup?role=HIRER">I'm hiring</a>
        </div>
        <h1>${role == 'HIRER' ? 'Create a recruiter account' : 'Create your account'}</h1>
        <form method="post" action="${ctx}/signup">
            <sh:csrf/>
            <input type="hidden" name="role" value="${role}">
            <label for="fullName">Full name</label>
            <input type="text" id="fullName" name="fullName" value="<c:out value='${fullName}'/>" required maxlength="120">
            <label for="email">Email</label>
            <input type="email" id="email" name="email" value="<c:out value='${email}'/>" required maxlength="190">
            <label for="password">Password (min 8 characters)</label>
            <input type="password" id="password" name="password" required minlength="8">

            <c:if test="${role == 'HIRER'}">
                <h2>Your company</h2>
                <label for="companyId">Join an existing company</label>
                <select id="companyId" name="companyId">
                    <option value="">-- my company is not listed --</option>
                    <c:forEach var="co" items="${companies}">
                        <option value="${co.id}"><c:out value="${co.name}"/><c:if test="${co.verified}"> (verified)</c:if></option>
                    </c:forEach>
                </select>
                <p class="muted small">...or register a new one (it will be verified by an admin):</p>
                <div class="form-grid">
                    <div><label for="newCompanyName">New company name</label>
                        <input type="text" id="newCompanyName" name="newCompanyName" value="<c:out value='${newCompanyName}'/>" maxlength="150"></div>
                    <div><label for="companyCity">City</label><input type="text" id="companyCity" name="companyCity" maxlength="80"></div>
                </div>
                <label for="companyWebsite">Website</label>
                <input type="text" id="companyWebsite" name="companyWebsite" maxlength="200" placeholder="https://">
                <label for="designation">Your designation</label>
                <input type="text" id="designation" name="designation" maxlength="100" placeholder="e.g. Talent Acquisition Lead">
            </c:if>
            <p><button class="btn big clay" style="width:100%">Create account</button></p>
        </form>
        <p class="muted small">Already have an account? <a href="${ctx}/login">Log in</a></p>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
