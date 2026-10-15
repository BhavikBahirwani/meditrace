<%--
    Verification result (mobile-first). One template, four states driven by
    result.status: GENUINE (green), TAMPERED (red), POSSIBLE_FAKE (amber), NOT_FOUND (ink).
--%>
<c:set var="pageTitle" value="${result.status.label} · ${result.packCode}"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<c:set var="st" value="${result.status.name}"/>
<main class="container verify-page">

    <%-- 1. Big status card --%>
    <section class="result-hero ${result.status.cssClass}" aria-live="polite">
        <span class="icon" aria-hidden="true">
            <c:choose>
                <c:when test="${st == 'GENUINE'}">
                    <svg width="36" height="36" viewBox="0 0 24 24"><path d="m5 12.5 4.5 4.5L19 7.5" stroke="#fff" stroke-width="2.8" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </c:when>
                <c:when test="${st == 'TAMPERED'}">
                    <svg width="36" height="36" viewBox="0 0 24 24"><path d="M6.5 6.5l11 11m0-11-11 11" stroke="#fff" stroke-width="2.8" stroke-linecap="round"/></svg>
                </c:when>
                <c:when test="${st == 'POSSIBLE_FAKE'}">
                    <svg width="36" height="36" viewBox="0 0 24 24"><path d="M12 4 2.5 20h19L12 4z" fill="none" stroke="#1A1446" stroke-width="2.2" stroke-linejoin="round"/><path d="M12 10v4.5" stroke="#1A1446" stroke-width="2.2" stroke-linecap="round"/><circle cx="12" cy="17.3" r="1.3" fill="#1A1446"/></svg>
                </c:when>
                <c:otherwise>
                    <svg width="36" height="36" viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5" stroke="#fff" stroke-width="2.4" fill="none"/><path d="m16 16 4.5 4.5" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/></svg>
                </c:otherwise>
            </c:choose>
        </span>
        <h1><c:out value="${result.status.label}"/></h1>
        <p><c:out value="${result.detail}"/></p>
        <span class="code"><c:out value="${result.packCode}"/></span>
    </section>

    <%-- 2. Recall / expiry warnings --%>
    <c:if test="${not empty result.recall}">
        <div class="alert alert-error" role="alert" style="margin-top:16px">
            <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="#B42318"/><path d="M12 7v6" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/><circle cx="12" cy="16.8" r="1.4" fill="#fff"/></svg>
            <p><strong>This batch has been recalled (${result.recall.severityText}) on ${result.recall.recallDateText}.</strong>
                <c:out value="${result.recall.reason}"/> Do not use it — return it to the pharmacy.</p>
        </div>
    </c:if>
    <c:if test="${result.found and result.batch.expired}">
        <div class="alert alert-warning" role="alert" style="margin-top:16px">
            <p><strong>Expired.</strong> This batch expired in ${result.batch.expiryDateText}.</p>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${result.found}">
            <%-- 3. Product facts --%>
            <section class="card" style="margin-top:16px">
                <h2><c:out value="${result.batch.productName}"/>
                    <span class="muted product-form"><c:out value="${result.batch.dosageForm}"/></span></h2>
                <dl class="facts">
                    <div><dt>Batch no.</dt><dd class="mono"><c:out value="${result.batch.batchNo}"/></dd></div>
                    <div><dt>MRP</dt><dd>${result.batch.mrpText}</dd></div>
                    <div><dt>Pack status</dt><dd>${result.pack.sold ? 'Sold' : 'In supply chain'}</dd></div>
                    <div><dt>Manufactured</dt><dd>${result.batch.mfgDateText}</dd></div>
                    <div><dt>Expires</dt><dd>${result.batch.expiryDateText}</dd></div>
                    <div><dt>Made by</dt><dd><c:out value="${result.manufacturer.displayName}"/></dd></div>
                    <c:if test="${not empty result.currentHolder}">
                        <div class="wide"><dt>Last held by</dt><dd><c:out value="${result.currentHolder.displayName}"/></dd></div>
                    </c:if>
                </dl>
            </section>

            <%-- 4. Journey timeline --%>
            <section class="card">
                <h2>Journey of this pack</h2>
                <ol class="timeline">
                    <c:forEach var="step" items="${result.journey}">
                        <li>
                            <span class="dot ${step.roleSlug}" aria-hidden="true"></span>
                            <span class="label"><c:out value="${step.label}"/></span>
                            <span class="who"><c:out value="${step.actor}"/><c:if test="${not empty step.city}">, <c:out value="${step.city}"/></c:if></span>
                            <span class="meta">
                                <span>${step.timeText}</span>
                                <c:choose>
                                    <c:when test="${not empty step.blockIndex}">
                                        <span>Block #${step.blockIndex} · <span class="mono">${step.shortHash}…</span></span>
                                        <c:if test="${step.verified}"><span class="pill genuine">✓ verified</span></c:if>
                                    </c:when>
                                    <c:when test="${step.roleSlug != 'customer'}">
                                        <span class="pill warning">awaiting block</span>
                                    </c:when>
                                </c:choose>
                            </span>
                        </li>
                    </c:forEach>
                </ol>
            </section>

            <p class="small muted" style="margin-top:12px">
                <c:choose>
                    <c:when test="${result.previousScans == 0}">This is the first time this code has been checked.</c:when>
                    <c:otherwise>This code has been checked ${result.previousScans} time${result.previousScans == 1 ? '' : 's'} before.</c:otherwise>
                </c:choose>
            </p>
        </c:when>
        <c:otherwise>
            <section class="card" style="margin-top:16px">
                <h2>What now?</h2>
                <ul>
                    <li>Check the code was typed exactly as printed, e.g. <span class="mono">MT-B1023-7F3A9C</span>.</li>
                    <li>If it is correct, this pack is not registered with MediTrace. Do not use it before asking your pharmacist.</li>
                </ul>
            </section>
        </c:otherwise>
    </c:choose>

    <c:if test="${st == 'TAMPERED' or st == 'POSSIBLE_FAKE'}">
        <section class="card">
            <h2>What should I do?</h2>
            <ul style="margin:0;padding-left:20px">
                <li>Do not take this medicine until it has been checked.</li>
                <li>Keep the pack and your bill.</li>
                <li>Show this page to the pharmacist, or report it to your State Drug Controller.</li>
            </ul>
        </section>
    </c:if>

    <%-- 5. Check another --%>
    <section class="card">
        <h2>Check another pack</h2>
        <form class="verify-form" method="get" action="${ctx}/verify" role="search" style="max-width:none">
            <label for="code" class="visually-hidden">Pack code</label>
            <input id="code" name="code" type="text" required maxlength="40" autocomplete="off"
                   autocapitalize="characters" spellcheck="false" placeholder="MT-B1023-7F3A9C">
            <button class="btn btn-primary" type="submit">Verify</button>
        </form>
    </section>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
