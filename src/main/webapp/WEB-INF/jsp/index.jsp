<%-- Landing page: hero, pack-code input, how it works. Served by HomeServlet at "/". --%>
<c:set var="pageTitle" value="Scan. Verify. Trust your medicine."/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<main>
    <section class="hero container">
        <c:if test="${not empty startupError}">
            <div class="alert alert-error" role="alert" style="text-align:left">
                <p><strong>Database not connected.</strong> <c:out value="${startupError}"/></p>
            </div>
        </c:if>

        <span class="pill eyebrow">Medicine traceability for India</span>
        <h1>Scan. Verify.<br><span>Trust your medicine.</span></h1>
        <p class="lead">Every MediTrace pack carries a unique code sealed on a blockchain at the factory.
            Scan the QR or type the code to see where your medicine has been — and whether it is genuine.</p>

        <form class="verify-form" method="get" action="${ctx}/verify" role="search">
            <label for="code" class="visually-hidden">Pack code</label>
            <input id="code" name="code" type="text" required maxlength="40" autocomplete="off"
                   autocapitalize="characters" spellcheck="false" placeholder="MT-B1023-7F3A9C">
            <button class="btn btn-primary" type="submit">Verify pack</button>
        </form>

        <div class="demo-codes" aria-label="Demo codes">
            <span class="small muted" style="width:100%">Try a demo code:</span>
            <a href="${ctx}/verify?code=MT-B1023-7F3A9C">MT-B1023-7F3A9C</a>
            <a href="${ctx}/verify?code=MT-B1023-2D81E4">MT-B1023-2D81E4</a>
            <a href="${ctx}/verify?code=MT-B0991-A96F09">MT-B0991-A96F09</a>
            <a href="${ctx}/verify?code=MT-B9999-000000">MT-B9999-000000</a>
        </div>
    </section>

    <section class="container">
        <div class="steps">
            <div class="card">
                <span class="step-num">1</span>
                <h3>Sealed at the factory</h3>
                <p class="muted" style="margin:0">The manufacturer registers each batch. Its details and every pack code
                    are fingerprinted (SHA-256) into a block.</p>
            </div>
            <div class="card">
                <span class="step-num">2</span>
                <h3>Tracked at every handover</h3>
                <p class="muted" style="margin:0">Each move from manufacturer to distributor to pharmacy becomes a new
                    block, linked to the one before it.</p>
            </div>
            <div class="card">
                <span class="step-num">3</span>
                <h3>Checked by you</h3>
                <p class="muted" style="margin:0">Your scan re-checks the fingerprints. Edited records show
                    <strong>Tampered</strong>; a code already sold shows <strong>Possible fake</strong>.</p>
            </div>
        </div>

        <h2 style="margin-top:40px">Built for the whole supply chain</h2>
        <div class="roles-strip">
            <div class="role-tile bg-manufacturer">
                <h3>Manufacturers</h3>
                <p>Register batches and issue unique pack codes.</p>
            </div>
            <div class="role-tile bg-distributor">
                <h3>Distributors</h3>
                <p>Receive and dispatch stock with a tamper-evident trail.</p>
            </div>
            <div class="role-tile bg-pharmacy">
                <h3>Pharmacies</h3>
                <p>Sell verified packs and act on recalls instantly.</p>
            </div>
        </div>
    </section>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
