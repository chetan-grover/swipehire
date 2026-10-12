<%@ tag body-content="empty" description="Matched / missing skill chips shown on every card" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="matched" required="true" type="java.util.List" %>
<%@ attribute name="missing" required="false" type="java.util.List" %>
<%@ attribute name="optional" required="false" type="java.util.List" %>
<div class="chips">
    <c:forEach var="s" items="${matched}"><span class="chip ok">&#10003; <c:out value="${s}"/></span></c:forEach>
    <c:forEach var="s" items="${missing}"><span class="chip miss" title="Required skill you don't have">&#10007; <c:out value="${s}"/></span></c:forEach>
    <c:forEach var="s" items="${optional}"><span class="chip opt" title="Nice to have"><c:out value="${s}"/></span></c:forEach>
</div>
