package com.alex.market.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CartFilter implements Filter {

    private final String SESSION_CART = "cart";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) servletRequest).getSession();

        if (session.getAttribute(SESSION_CART) == null) {
            session.setAttribute(SESSION_CART, new HashMap<Long, Integer>());
        }
        filterChain.doFilter(servletRequest, servletResponse);

    }
}
