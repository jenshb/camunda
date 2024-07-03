/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapp.controllers;

import static io.camunda.poc.webapp.security.WebAppsPocURIs.LOGIN_RESOURCE;
import static io.camunda.poc.webapp.security.WebAppsPocURIs.REQUESTED_URL;
import static io.camunda.webapps.controllers.WebappsRequestForwardManager.getRequestedUrl;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebAppsPocIndexController {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebAppsPocIndexController.class);

  @Autowired private ServletContext context;

  @GetMapping("/webapps")
  public String webapps(final Model model) {
    model.addAttribute("contextPath", context.getContextPath() + "/webapps/");
    return "webapps/index";
  }

  @RequestMapping(value = {"/webapps/{regex:[\\w-]+}", "/webapps/**/{regex:[\\w-]+}"})
  public String forwardToWebApps(final HttpServletRequest request) {
    return forward(request, "webapps");
  }

  /**
   * Redirects the old frontend routes to the /webapps sub-path. This can be removed after the
   * creation of the auto-discovery service.
   */
  @GetMapping({"/{regex:[\\d]+}", "/processes/*/start"})
  public String redirectOldRoutes(final HttpServletRequest request) {
    return "redirect:/webapps" + getRequestedUrl(request);
  }

  private String forward(final HttpServletRequest request, final String app) {
    if (isNotLoggedIn()) {
      return saveRequestAndRedirectToLogin(request);
    } else {
      return "forward:/" + app;
    }
  }

  private String saveRequestAndRedirectToLogin(final HttpServletRequest request) {
    final String requestedUrl = getRequestedUrl(request);
    request.getSession(true).setAttribute(REQUESTED_URL, requestedUrl);
    LOGGER.warn(
        "Requested path {}, but not authenticated. Redirect to  {} ",
        request.getRequestURI().substring(request.getContextPath().length()),
        LOGIN_RESOURCE);
    return "forward:" + LOGIN_RESOURCE;
  }

  public static String getRequestedUrl(final HttpServletRequest request) {
    final String requestedPath =
        request.getRequestURI().substring(request.getContextPath().length());
    final String queryString = request.getQueryString();
    final String requestedUrl =
        StringUtils.isEmpty(queryString) ? requestedPath : requestedPath + "?" + queryString;
    return requestedUrl;
  }

  private boolean isNotLoggedIn() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (authentication instanceof AnonymousAuthenticationToken)
        || !authentication.isAuthenticated();
  }
}
