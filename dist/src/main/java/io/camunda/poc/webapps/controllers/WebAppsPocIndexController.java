/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.poc.webapps.controllers;

import static io.camunda.webapps.controllers.WebappsRequestForwardManager.getRequestedUrl;

import io.camunda.webapps.controllers.WebappsRequestForwardManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebAppsPocIndexController {

  @Autowired private ServletContext context;

  @Autowired private WebappsRequestForwardManager webappsRequestForwardManager;

  @GetMapping("/webapps")
  public String webapps(final Model model) {
    model.addAttribute("contextPath", context.getContextPath() + "/webapps/");
    return "webapps/index";
  }

  @RequestMapping(value = {"/webapps/{regex:[\\w-]+}", "/webapps/**/{regex:[\\w-]+}"})
  public String forwardToWebApps(final HttpServletRequest request) {
    return webappsRequestForwardManager.forward(request, "webapps");
  }

  /**
   * Redirects the old frontend routes to the /webapps sub-path. This can be removed after the
   * creation of the auto-discovery service.
   */
  @GetMapping({"/{regex:[\\d]+}", "/processes/*/start"})
  public String redirectOldRoutes(final HttpServletRequest request) {
    return "redirect:/webapps" + getRequestedUrl(request);
  }
}
