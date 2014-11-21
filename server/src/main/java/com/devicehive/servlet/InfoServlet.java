package com.devicehive.servlet;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;

import javax.ejb.EJB;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

@WebServlet("/index")
public class InfoServlet extends HttpServlet {

    private static final long serialVersionUID = -4886819685195322L;

    @EJB
    private transient ConfigurationService configurationService;

    private static final String INFO_PAGE = "info_page.jsp";

    @EJB
    PropertiesService propertiesService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(Constants.REST_SERVER_URL, configurationService.get(Constants.REST_SERVER_URL));
        request.setAttribute(Constants.WEBSOCKET_SERVER_URL, configurationService.get(Constants.WEBSOCKET_SERVER_URL));

        Properties properties = propertiesService.getProperties();
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            request.setAttribute(entry.getKey().toString(), entry.getValue());
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(INFO_PAGE);
        dispatcher.forward(request, response);
    }
}
