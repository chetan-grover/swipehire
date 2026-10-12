<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Notifications"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="row spread">
    <h1>Notifications</h1>
    <form method="post" action="${ctx}/notifications/read-all"><sh:csrf/><button class="btn ghost small">Mark all as read</button></form>
</div>

<c:choose>
    <c:when test="${empty notifications}">
        <div class="card empty"><div class="big">&#128276;</div><p class="muted">Nothing yet. Matches, status changes and messages will show up here.</p></div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table>
                <c:forEach var="n" items="${notifications}">
                    <tr class="${empty n.readAt ? 'unread' : ''}">
                        <td style="width:130px"><span class="chip">${f:title(n.type)}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty n.link}"><a href="${ctx}${n.link}"><c:out value="${n.title}"/></a></c:when>
                                <c:otherwise><c:out value="${n.title}"/></c:otherwise>
                            </c:choose>
                            <c:if test="${not empty n.body}"><div class="muted small" style="font-weight:normal"><c:out value="${n.body}"/></div></c:if>
                        </td>
                        <td class="muted small" style="width:110px">${f:ago(n.createdAt)}</td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
