<%-- Pharmacy dashboard (magenta area colour). Data: request attributes "user" and "view". --%>
<c:set var="pageTitle" value="Pharmacy dashboard"/>
<c:set var="bodyClass" value="area-pharmacy"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/dash-header.jspf" %>

<main class="container dash-body">
    <%@ include file="/WEB-INF/jsp/fragments/dash-top.jspf" %>

    <div class="dash-grid">
        <c:set var="stockTitle" value="On your shelves"/>
        <c:set var="linkBatches" value="${false}"/>
        <%@ include file="/WEB-INF/jsp/fragments/stock.jspf" %>

        <%@ include file="/WEB-INF/jsp/fragments/scans.jspf" %>

        <div class="span-2">
            <%@ include file="/WEB-INF/jsp/fragments/activity.jspf" %>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
