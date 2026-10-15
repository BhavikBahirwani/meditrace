<%--
    Shared error page for 403 / 404 / 500 / 503 and uncaught exceptions (see web.xml).
    Never shows stack traces to the user; details go to the server log.
--%><%@ page isErrorPage="true" %>
<c:set var="code" value="${pageContext.response.status}"/>
<c:choose>
    <c:when test="${not empty errorTitle}"><c:set var="title" value="${errorTitle}"/></c:when>
    <c:when test="${code == 403}"><c:set var="title" value="Access denied"/></c:when>
    <c:when test="${code == 404}"><c:set var="title" value="Page not found"/></c:when>
    <c:otherwise><c:set var="title" value="Something went wrong"/></c:otherwise>
</c:choose>
<c:set var="pageTitle" value="${title}"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<main class="container">
    <div class="error-page card">
        <div class="status">${code}</div>
        <h1 style="font-size:1.9rem"><c:out value="${title}"/></h1>
        <p class="muted">
            <c:choose>
                <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
                <c:when test="${code == 404}">We couldn't find that page.</c:when>
                <c:otherwise>An unexpected error occurred. Please try again.</c:otherwise>
            </c:choose>
        </p>
        <a class="btn btn-primary" href="${ctx}/">Back to home</a>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
