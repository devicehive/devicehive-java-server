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
<table>
    <tr>
        <td>
            RESTful endpoint
        </td>
        <td>
            <a href="<%=request.getAttribute(Constants.REST_SERVER_URL)%>"><%=request.getAttribute(Constants.REST_SERVER_URL)%></a>
        </td>
    </tr>
    <tr>
        <td>
            Websocket Client endpoint
        </td>
        <td>
            <a href="<%=request.getAttribute(Constants.WEBSOCKET_SERVER_URL)%>/client"><%=request.getAttribute(Constants.WEBSOCKET_SERVER_URL)%>/client</a>
        </td>
    </tr>
    <tr>
        <td>
            Websocket Device endpoint
        </td>
        <td>
            <a href="<%=request.getAttribute(Constants.WEBSOCKET_SERVER_URL)%>/device"><%=request.getAttribute(Constants.WEBSOCKET_SERVER_URL)%>/device</a>
        </td>
    </tr>
</table>
</p>
</body>
</html>