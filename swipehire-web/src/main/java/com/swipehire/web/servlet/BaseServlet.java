package com.swipehire.web.servlet;

import com.swipehire.web.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.security.Principal;

/**
 * Common helpers for our raw servlets.
 * <p>
 * The servlet container (not Spring) creates servlet objects, so we fetch Spring beans from the
 * WebApplicationContext stored in the ServletContext. The logged-in user comes from the standard
 * Servlet API call request.getUserPrincipal(), which Spring Security fills in.
 */
public abstract class BaseServlet extends HttpServlet {

    protected <T> T bean(Class<T> type) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getBean(type);
    }

    protected AppUserPrincipal currentUser(HttpServletRequest req) {
        Principal p = req.getUserPrincipal();
        if (p instanceof Authentication auth && auth.getPrincipal() instanceof AppUserPrincipal user) {
            return user;
        }
        return null;
    }

    protected static long longParam(HttpServletRequest req, String name) {
        try {
            return Long.parseLong(req.getParameter(name));
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
