<%@ tag body-content="empty" description="Hidden CSRF token field; every POST form must include it" %>
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
