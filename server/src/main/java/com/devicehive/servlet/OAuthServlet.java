package com.devicehive.servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/oauth2")
public class OAuthServlet extends HttpServlet {

    private static final long serialVersionUID = -4886819685195322L;

    private static final String OAUTH2_PAGE = "oauth2/index.html";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(OAUTH2_PAGE);
        dispatcher.forward(request, response);
    }
}
