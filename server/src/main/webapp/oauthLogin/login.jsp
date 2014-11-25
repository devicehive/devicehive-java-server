<%@ page import="com.devicehive.configuration.Constants" %>
<%--
  Created by IntelliJ IDEA.
  User: tatyana
  Date: 11/25/14
  Time: 12:32 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Login page</title>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css">
  <link href="//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
  <link href="css/social-buttons.css" rel="stylesheet"/>
  <script type="text/javascript" src="webjars/jquery/1.11.1/jquery.min.js"></script>
  <script type="application/javascript">
    $(document).ready(function () {
      var deviceHiveUrl = "http://" + window.location.host + "/DeviceHiveJava/";
      var deviceHiveHomeUrl = deviceHiveUrl + "home";
      var deviceHiveLoginUrl = deviceHiveUrl + "login";

      if (sessionStorage.deviceHiveToken) {
        document.location.href = deviceHiveHomeUrl;
      }

      [].forEach.call(document.getElementsByName("redirect_uri"), function(elem) {
        elem.value = deviceHiveLoginUrl;
      });

      var params = {}, queryString = location.hash.substring(1),
              regex = /([^&=]+)=([^&]*)/g, m;
      if (queryString) {
        while (m = regex.exec(queryString)) {
          params[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
        }

        var xhr = new XMLHttpRequest();
        xhr.open('GET', deviceHiveUrl + 'rest/oauth2/token/apply?' + queryString, true);
        xhr.setRequestHeader('Authorization', 'Identity');

        xhr.onreadystatechange = function (e) {
          if (xhr.readyState == 4) {
            if(xhr.status == 200){
              var token = xhr.response;
              if (token) {
                sessionStorage.deviceHiveToken = token;
                document.location.href = deviceHiveHomeUrl;
              }
            } else {
              document.body.innerHTML = xhr.response;
            }
          }
        };
        xhr.send(null);
      }
    });
  </script>
</head>
<body>
<h1>Welcome to DeviceHive!</h1>
<div>
  <form action="https://accounts.google.com/o/oauth2/auth" method="get">
    <input name="response_type" value="token" type="hidden">
    <input name="client_id" value="<%=request.getAttribute(Constants.GOOGLE_CLIENT_ID)%>" type="hidden">
    <input name="redirect_uri" type="hidden">
    <input name="scope" value="email" type="hidden">
    <input name="state" value="identity_provider_id=<%=Constants.GOOGLE_IDENTITY_ID%>" type="hidden">
    <button type="submit" class="btn btn-google-plus"><i class="fa fa-google-plus"> Sign in with Google</i></button>
  </form>
</div>

<div>
  <form action="https://www.facebook.com/dialog/oauth" method="get">
    <input name="response_type" value="token" type="hidden">
    <input name="client_id" value="<%=request.getAttribute(Constants.FACEBOOK_CLIENT_ID)%>" type="hidden">
    <input name="redirect_uri" type="hidden">
    <input name="scope" value="email" type="hidden">
    <input name="state" value="identity_provider_id=<%=Constants.FACEBOOK_IDENTITY_ID%>" type="hidden">
    <button type="submit" class="btn btn-facebook"><i class="fa fa-facebook"> Sign in with Facebook</i></button>
  </form>
</div>
</body>
</html>
