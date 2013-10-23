package com.devicehive.servlet;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;

import javax.ejb.EJB;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/index")
public class InfoServlet extends HttpServlet {

    private static final long serialVersionUID = -4886819685195322L;

    @EJB
    private transient ConfigurationService configurationService;

    private static final String INFO_PAGE = "info_page.jsp";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(Constants.REST_SERVER_URL, configurationService.get(Constants.REST_SERVER_URL));
        request.setAttribute(Constants.WEBSOCKET_SERVER_URL, configurationService.get(Constants.WEBSOCKET_SERVER_URL));
        RequestDispatcher dispatcher = request.getRequestDispatcher(INFO_PAGE);
        dispatcher.forward(request, response);
    }
}
