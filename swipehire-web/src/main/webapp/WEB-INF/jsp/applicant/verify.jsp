<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Verify college"/><c:set var="activeNav" value="verify"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<h1>College verification</h1>
<div class="grid two">
    <div>
        <c:if test="${profile.collegeVerified}">
            <div class="card">
                <span class="badge verified">&#10003; Verified student</span>
                <h2><c:out value="${profile.college.name}"/></h2>
                <p class="muted">Verified with <c:out value="${profile.collegeEmail}"/> on ${f:date(profile.collegeVerifiedAt)}.
                    Jobs marked "verified students only" are now open to you.</p>
            </div>
        </c:if>
        <form class="card" method="post" action="${ctx}/applicant/verify/send">
            <sh:csrf/>
            <h2 style="margin-top:0">${profile.collegeVerified ? 'Verify a different college email' : 'Step 1: your college email'}</h2>
            <label>College email address</label>
            <input type="email" name="collegeEmail" required value="<c:out value='${collegeEmail}'/>" placeholder="name.rollno@vitstudent.ac.in">
            <p><button class="btn">Send code</button></p>
            <p class="muted small">We send a 6-digit code valid for 10 minutes. Only a hash of the code is stored.</p>
        </form>
        <form class="card" method="post" action="${ctx}/applicant/verify/confirm">
            <sh:csrf/>
            <h2 style="margin-top:0">Step 2: enter the code</h2>
            <input type="text" name="code" required maxlength="6" pattern="[0-9]{6}" inputmode="numeric" placeholder="123456"
                   style="font-size:1.4rem;letter-spacing:6px;max-width:220px" ${otpSent ? 'autofocus' : ''}>
            <p><button class="btn clay">Verify</button></p>
            <p class="muted small">5 attempts per code.</p>
        </form>
    </div>
    <div>
        <div class="card">
            <h3>Supported colleges</h3>
            <table>
                <tr><th>College</th><th>Email domain</th></tr>
                <c:forEach var="co" items="${colleges}"><tr><td><c:out value="${co.name}"/></td><td class="muted">@<c:out value="${co.emailDomain}"/></td></tr></c:forEach>
            </table>
        </div>
        <form class="card" method="post" action="${ctx}/applicant/verify/request-college">
            <sh:csrf/>
            <h3>College not listed?</h3>
            <label>College name</label><input type="text" name="name" required maxlength="150">
            <div class="form-grid">
                <div><label>Student email domain</label><input type="text" name="domain" required placeholder="college.edu.in"></div>
                <div><label>City</label><input type="text" name="city" maxlength="80"></div>
            </div>
            <p><button class="btn ghost">Request it</button></p>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
