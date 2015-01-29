<%--
  Created by IntelliJ IDEA.
  User: tatyana
  Date: 11/25/14
  Time: 3:17 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Home page</title>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css">
  <link href="//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
  <link href="css/social-buttons.css" rel="stylesheet"/>
  <script type="text/javascript" src="webjars/jquery/1.11.1/jquery.min.js"></script>
  <script type="application/javascript">
    var deviceHiveUrl = "http://" + window.location.host + "/DeviceHiveJava/";
    var deviceHiveLoginUrl = deviceHiveUrl + "login";

    $(document).ready(function () {
      if (!sessionStorage.deviceHiveToken) {
        document.location.href = deviceHiveLoginUrl;
      }
    });

    var sendLogoutRequest = function () {
      var xhr = new XMLHttpRequest();
      xhr.open('GET', deviceHiveUrl + 'rest/oauth2/token/remove', true);
      xhr.setRequestHeader('Authorization', "Bearer " + sessionStorage.deviceHiveToken);

      xhr.onreadystatechange = function (e) {
        if (xhr.readyState == 4) {
          if(xhr.status == 200){
            sessionStorage.deviceHiveToken = "";
            document.location.href = deviceHiveLoginUrl;
          }
        }
      };
      xhr.send(null);
    };
  </script>
</head>
<body>
<h1>Welcome, user!</h1>
<div>
  <button onclick="sendLogoutRequest()" class="btn btn-linkedin">Log out</button>
</div>
</body>
</html>
