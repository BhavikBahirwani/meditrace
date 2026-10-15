<%-- Batch detail for its manufacturer: details + every pack code (each links to /verify). --%>
<c:set var="pageTitle" value="Batch ${batch.batchNo}"/>
<c:set var="bodyClass" value="area-manufacturer"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/dash-header.jspf" %>

<main class="container dash-body">
    <c:if test="${not empty flash}">
        <div class="alert alert-ok" role="status"><p><c:out value="${flash}"/></p></div>
    </c:if>

    <section class="card">
        <p style="margin:0 0 6px"><a href="${ctx}/manufacturer/dashboard">← Back to dashboard</a></p>
        <h2>Batch <span class="mono"><c:out value="${batch.batchNo}"/></span> ·
            <c:out value="${batch.productName}"/> <span class="muted product-form"><c:out value="${batch.dosageForm}"/></span></h2>
        <dl class="facts">
            <div><dt>MRP</dt><dd>${batch.mrpText}</dd></div>
            <div><dt>Manufactured</dt><dd>${batch.mfgDateText}</dd></div>
            <div><dt>Expires</dt><dd>${batch.expiryDateText}</dd></div>
            <div><dt>Packs</dt><dd>${batch.packCount}</dd></div>
        </dl>
    </section>

    <section class="card">
        <h2>Pack codes</h2>
        <p class="muted small">Each code is printed as a QR pointing to
            <span class="mono">${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${ctx}/verify?code=…</span>.
            Click one to test it.</p>
        <div class="code-grid">
            <c:forEach var="code" items="${batch.packCodes}">
                <a href="${ctx}/verify?code=${fn:escapeXml(code)}"><c:out value="${code}"/></a>
            </c:forEach>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
