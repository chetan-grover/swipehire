<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Log in"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="narrow" style="margin:30px auto">
    <div class="card">
        <h1>Welcome back</h1>
        <c:if test="${param.error != null}"><div class="alert err">Wrong email or password, or the account is disabled.</div></c:if>
        <c:if test="${param.logout != null}"><div class="alert ok">You have been logged out.</div></c:if>
        <form method="post" action="${ctx}/login">
            <sh:csrf/>
            <label for="email">Email</label>
            <input type="email" id="email" name="email" required autofocus>
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required>
            <p><button class="btn big" style="width:100%">Log in</button></p>
        </form>
        <p class="muted small">New here? <a href="${ctx}/signup">Create an account</a></p>
    </div>
    <div class="card small">
        <b>Demo accounts</b> (password <code>demo1234</code>)
        <ul class="muted" style="margin:6px 0 0;padding-left:18px">
            <li>Applicant: aarav.mehta@gmail.com, ishita.rao@gmail.com, kabir.singh@gmail.com</li>
            <li>Hirer: priya@zenlabs.in (ZenLabs), arjun@finedge.in, meera@cloudnest.io</li>
            <li>Admin: admin@swipehire.in</li>
        </ul>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
