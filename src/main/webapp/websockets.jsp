<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>

<%
    StringBuilder wsUrl = new StringBuilder()
        .append(request.isSecure() ? "wss://" : "ws://")
        .append(request.getServerName())
        .append(":")
        .append(request.getServerPort())
        .append(request.getContextPath())
        .append("/")
        .append(request.getParameter("path"));
%>
    <script type="text/javascript">
        var client = new WebSocket("<%=wsUrl%>");

        client.onmessage = function(event) {
            document.getElementById("response").value = event.data;
            alert(event.data);
        }

        function send() {

            client.send(document.getElementById("request").value);
        }
    </script>
</head>
<body>
 <table style="width: 100%">
     <tr>
         <td>
             Request
         </td>
         <td>
             Response
         </td>
     </tr>
     <tr>

         <td>
             <textarea id="request" rows="40" style="width: 100%"></textarea>
         </td>
         <td>
             <textarea id="response" rows="40" style="width: 100%"></textarea>
        </td>
     </tr>
 </table>
 <button onclick="send()" >Send</button>
</body>
</html>