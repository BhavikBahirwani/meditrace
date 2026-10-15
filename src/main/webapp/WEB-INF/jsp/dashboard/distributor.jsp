<%-- Distributor dashboard (teal area colour). Data: request attributes "user" and "view". --%>
<c:set var="pageTitle" value="Distributor dashboard"/>
<c:set var="bodyClass" value="area-distributor"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/dash-header.jspf" %>

<main class="container dash-body">
    <%@ include file="/WEB-INF/jsp/fragments/dash-top.jspf" %>

    <div class="dash-grid">
        <c:set var="stockTitle" value="Stock in your warehouse"/>
        <c:set var="linkBatches" value="${false}"/>
        <%@ include file="/WEB-INF/jsp/fragments/stock.jspf" %>

        <%@ include file="/WEB-INF/jsp/fragments/activity.jspf" %>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
