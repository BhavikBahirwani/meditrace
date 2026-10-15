<%-- Registration with a colour-coded role picker. Posts to RegisterServlet. --%>
<c:set var="pageTitle" value="Create an account"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<main class="container">
    <div class="auth-wrap" style="max-width:720px">
        <div class="card">
            <h1 style="font-size:2rem">Join MediTrace</h1>
            <p class="muted">Register your business to record batches and handovers on the blockchain.</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert"><p><c:out value="${error}"/></p></div>
            </c:if>

            <form method="post" action="${ctx}/register">
                <fieldset class="role-picker">
                    <legend>I am a…</legend>
                    <div class="role-option" style="--role-colour: var(--manufacturer)">
                        <input type="radio" id="role-m" name="role" value="MANUFACTURER" required
                               ${role == 'MANUFACTURER' ? 'checked' : ''}>
                        <label for="role-m"><span class="swatch"></span>
                            <span><strong>Manufacturer</strong><span class="desc">Makes medicines, registers batches</span></span>
                        </label>
                    </div>
                    <div class="role-option" style="--role-colour: var(--distributor)">
                        <input type="radio" id="role-d" name="role" value="DISTRIBUTOR"
                               ${role == 'DISTRIBUTOR' ? 'checked' : ''}>
                        <label for="role-d"><span class="swatch"></span>
                            <span><strong>Distributor</strong><span class="desc">Wholesaler / stockist</span></span>
                        </label>
                    </div>
                    <div class="role-option" style="--role-colour: var(--pharmacy)">
                        <input type="radio" id="role-p" name="role" value="PHARMACY"
                               ${role == 'PHARMACY' ? 'checked' : ''}>
                        <label for="role-p"><span class="swatch"></span>
                            <span><strong>Pharmacy</strong><span class="desc">Chemist selling to customers</span></span>
                        </label>
                    </div>
                </fieldset>

                <div class="grid-2">
                    <div class="field">
                        <label for="company">Company / store name</label>
                        <input id="company" name="company" type="text" required maxlength="120"
                               value="${fn:escapeXml(company)}" placeholder="Sharma Medicals">
                    </div>
                    <div class="field">
                        <label for="city">City</label>
                        <input id="city" name="city" type="text" required maxlength="60"
                               value="${fn:escapeXml(city)}" placeholder="Ghaziabad">
                    </div>
                    <div class="field">
                        <label for="fullName">Your name</label>
                        <input id="fullName" name="fullName" type="text" required maxlength="100"
                               value="${fn:escapeXml(fullName)}" autocomplete="name">
                    </div>
                    <div class="field">
                        <label for="licenseNo">Drug licence no. <span class="muted">(optional)</span></label>
                        <input id="licenseNo" name="licenseNo" type="text" maxlength="40"
                               value="${fn:escapeXml(licenseNo)}" class="mono">
                    </div>
                    <div class="field">
                        <label for="email">Email</label>
                        <input id="email" name="email" type="email" required maxlength="120"
                               value="${fn:escapeXml(email)}" autocomplete="email">
                    </div>
                    <div class="field">
                        <label for="username">Username</label>
                        <input id="username" name="username" type="text" required pattern="[A-Za-z0-9._\-]{3,30}"
                               value="${fn:escapeXml(username)}" autocomplete="username">
                        <div class="hint">3–30 letters, digits, . _ -</div>
                    </div>
                    <div class="field">
                        <label for="password">Password</label>
                        <input id="password" name="password" type="password" required minlength="8"
                               autocomplete="new-password">
                        <div class="hint">At least 8 characters</div>
                    </div>
                    <div class="field">
                        <label for="confirm">Confirm password</label>
                        <input id="confirm" name="confirm" type="password" required minlength="8"
                               autocomplete="new-password">
                    </div>
                </div>

                <button class="btn btn-primary btn-block" type="submit">Create account</button>
            </form>
            <p class="small muted" style="margin:18px 0 0">Already registered? <a href="${ctx}/login">Sign in</a></p>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
