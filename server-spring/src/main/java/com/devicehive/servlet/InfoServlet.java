package com.devicehive.servlet;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InfoServlet extends HttpServlet {
    private static final long serialVersionUID = -4886819685195322L;

    private static final String INFO_PAGE = "info_page.jsp";

    @Autowired
    private transient ConfigurationService configurationService;

    @Autowired
    private Environment env;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(Constants.REST_SERVER_URL, configurationService.get(Constants.REST_SERVER_URL));
        request.setAttribute(Constants.WEBSOCKET_SERVER_URL, configurationService.get(Constants.WEBSOCKET_SERVER_URL));

//        Properties properties = env.getProperties();
//        for (Map.Entry<?, ?> entry : properties.entrySet()) {
//            request.setAttribute(entry.getKey().toString(), entry.getValue());
//        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(INFO_PAGE);
        dispatcher.forward(request, response);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getAutowireCapableBeanFactory().autowireBean(this);
    }
}
