<%@ page import="com.devicehive.configuration.Constants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>
    <p>This is devicehive java server info page</p>

    <p>More info can be found at <a href="http://www.devicehive.com/">DeviceHive home page</a></p>

    <p>
        Rest server utl: <%=request.getAttribute(Constants.REST_SERVER_URL)%><br>
        Web socket server url:  <%=request.getAttribute(Constants.WEBSOCKET_SERVER_URL)%>

</body>
</html>