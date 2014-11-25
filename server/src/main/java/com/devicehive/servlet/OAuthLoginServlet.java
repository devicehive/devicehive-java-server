package com.devicehive.servlet;

import com.devicehive.configuration.Constants;
import com.devicehive.service.IdentityProviderService;

import javax.ejb.EJB;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by tmatvienko on 11/25/14.
 */
@WebServlet(urlPatterns = {"/login", "/home"})
public class OAuthLoginServlet extends HttpServlet {
    private static final long serialVersionUID = -2186819836195371L;

    private static final String OAUTH2_LOGIN_PAGE = "oauthLogin/login.jsp";
    private static final String OAUTH2_HOME_PAGE = "oauthLogin/home.jsp";

    @EJB
    private IdentityProviderService identityProviderService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(Constants.REST_SERVER_URL, "TEST");
        RequestDispatcher dispatcher = request.getRequestDispatcher(OAUTH2_HOME_PAGE);
        if (request.getRequestURI().contains("/login")) {
            request.setAttribute(Constants.GOOGLE_CLIENT_ID, identityProviderService.find(Constants.GOOGLE_IDENTITY_ID).getClientId());
            request.setAttribute(Constants.FACEBOOK_CLIENT_ID, identityProviderService.find(Constants.FACEBOOK_IDENTITY_ID).getClientId());
            dispatcher = request.getRequestDispatcher(OAUTH2_LOGIN_PAGE);
        }
        dispatcher.forward(request, response);
    }
}
