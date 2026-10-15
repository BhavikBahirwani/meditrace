<%-- Manufacturer dashboard (blue area colour). Data: request attributes "user" and "view". --%>
<c:set var="pageTitle" value="Manufacturer dashboard"/>
<c:set var="bodyClass" value="area-manufacturer"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/dash-header.jspf" %>

<main class="container dash-body">
    <%@ include file="/WEB-INF/jsp/fragments/dash-top.jspf" %>

    <div class="dash-grid">
        <section class="card" id="register">
            <h2>Register a new batch</h2>
            <p class="muted small">Creates the batch and one unique code per pack in a single database
                transaction, then seals it on the blockchain.</p>
            <form method="post" action="${ctx}/manufacturer/batches">
                <div class="grid-2">
                    <div class="field">
                        <label for="batchNo">Batch no.</label>
                        <input id="batchNo" name="batchNo" type="text" class="mono" required
                               pattern="[A-Za-z0-9]{2,20}" placeholder="B1030">
                    </div>
                    <div class="field">
                        <label for="medicineName">Medicine</label>
                        <input id="medicineName" name="medicineName" type="text" required maxlength="100" placeholder="Paracetamol">
                    </div>
                    <div class="field">
                        <label for="strength">Strength</label>
                        <input id="strength" name="strength" type="text" required maxlength="30" placeholder="500 mg">
                    </div>
                    <div class="field">
                        <label for="dosageForm">Dosage form</label>
                        <select id="dosageForm" name="dosageForm" required>
                            <option>Tablet</option><option>Capsule</option><option>Syrup</option>
                            <option>Injection</option><option>Ointment</option>
                        </select>
                    </div>
                    <div class="field">
                        <label for="mfgDate">Manufacturing date</label>
                        <input id="mfgDate" name="mfgDate" type="date" required>
                    </div>
                    <div class="field">
                        <label for="expiryDate">Expiry date</label>
                        <input id="expiryDate" name="expiryDate" type="date" required>
                    </div>
                    <div class="field">
                        <label for="mrp">MRP (₹)</label>
                        <input id="mrp" name="mrp" type="number" min="0.01" step="0.01" required placeholder="25.00">
                    </div>
                    <div class="field">
                        <label for="packCount">Number of packs</label>
                        <input id="packCount" name="packCount" type="number" min="1" max="500" required placeholder="20">
                    </div>
                </div>
                <button class="btn btn-area btn-block" type="submit">Register &amp; seal batch</button>
            </form>
        </section>

        <%@ include file="/WEB-INF/jsp/fragments/activity.jspf" %>

        <c:set var="stockTitle" value="Your batches"/>
        <c:set var="linkBatches" value="${true}"/>
        <%@ include file="/WEB-INF/jsp/fragments/stock.jspf" %>

        <%@ include file="/WEB-INF/jsp/fragments/scans.jspf" %>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
