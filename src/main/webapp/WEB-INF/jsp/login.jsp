<%-- Sign-in form. Posts to LoginServlet. --%>
<c:set var="pageTitle" value="Sign in"/>
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<main class="container">
    <div class="auth-wrap">
        <div class="card">
            <h1 style="font-size:2rem">Sign in</h1>
            <p class="muted">For manufacturers, distributors and pharmacies. Customers don't need an account —
                just <a href="${ctx}/">verify a pack</a>.</p>

            <c:if test="${not empty flash}">
                <div class="alert alert-ok" role="status"><p><c:out value="${flash}"/></p></div>
            </c:if>
            <c:if test="${param.loggedOut == '1'}">
                <div class="alert alert-info" role="status"><p>You have been signed out.</p></div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert"><p><c:out value="${error}"/></p></div>
            </c:if>

            <form method="post" action="${ctx}/login" novalidate>
                <input type="hidden" name="next" value="${fn:escapeXml(next)}">
                <div class="field">
                    <label for="username">Username</label>
                    <input id="username" name="username" type="text" required autocomplete="username"
                           value="${fn:escapeXml(username)}" autofocus>
                </div>
                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" required autocomplete="current-password">
                </div>
                <button class="btn btn-primary btn-block" type="submit">Sign in</button>
            </form>

            <p class="small muted" style="margin:18px 0 0">New to MediTrace? <a href="${ctx}/register">Create an account</a></p>
        </div>

        <div class="card small">
            <strong>Demo accounts</strong> (password <span class="mono">demo1234</span>)
            <ul class="list" style="margin-top:8px">
                <li><span class="mono">xyzpharma</span> <span class="pill manufacturer">Manufacturer</span></li>
                <li><span class="mono">abcdist</span> <span class="pill distributor">Distributor</span></li>
                <li><span class="mono">sharmamed</span> <span class="pill pharmacy">Pharmacy</span></li>
            </ul>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
