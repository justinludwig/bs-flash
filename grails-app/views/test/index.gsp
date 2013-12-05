<!doctype html>
<html>
<head>
    <title>BS Flash Test</title>
    <link href="/css/test/app.css" rel="stylesheet">
</head>
<body>
    <g:if test="${flash.message != null}">
        <div class="message">${raw(flash.message)}</div>
    </g:if>
    <h1>BS Flash Test</h1>
    <g:form action="flashGet" method="get">
        <button type="submit">GET flash</button>
    </g:form>
    <g:form action="flashPost" method="post">
        <button type="submit">POST flash</button>
    </g:form>
    <g:form action="noFlashGet" method="get">
        <button type="submit">GET no flash</button>
    </g:form>
    <g:form action="noFlashPost" method="post">
        <button type="submit">POST no flash</button>
    </g:form>
    <script src="/js/test/app.js"></script>
</body>
</html>
