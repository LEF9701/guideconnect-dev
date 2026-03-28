package com.guideconnect.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller that renders user-friendly error pages.
 *
 * <p>Implements Spring Boot's {@link ErrorController} to intercept the
 * default {@code /error} path and return appropriate Thymeleaf templates
 * based on the HTTP status code (400, 403, 404, or 500).</p>
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Handles all error requests by determining the HTTP status code and
     * rendering the corresponding error template.
     *
     * <p>Supported status codes and their templates:</p>
     * <ul>
     *   <li>400 Bad Request - {@code error/400}</li>
     *   <li>403 Forbidden - {@code error/403}</li>
     *   <li>404 Not Found - {@code error/404}</li>
     *   <li>500 and all others - {@code error/500}</li>
     * </ul>
     *
     * @param request the HTTP request containing the error status code
     * @param model   the Thymeleaf model
     * @return the appropriate error view name
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusCode != null) {
            int status = Integer.parseInt(statusCode.toString());
            model.addAttribute("statusCode", status);

            if (status == HttpStatus.BAD_REQUEST.value()) {
                return "error/400";
            } else if (status == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (status == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            }
        }

        return "error/500";
    }
}
