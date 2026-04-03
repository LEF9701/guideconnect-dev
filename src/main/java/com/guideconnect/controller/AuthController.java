package com.guideconnect.controller;

import com.guideconnect.model.Role;
import com.guideconnect.model.User;
import com.guideconnect.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller responsible for authentication-related endpoints.
 *
 * <p>Handles user login and registration flows, delegating account creation
 * to {@link UserService} and rendering Thymeleaf templates for the auth views.</p>
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    /**
     * Constructs an {@code AuthController} with the required service dependency.
     *
     * @param userService the service used for user registration and lookup
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the login page.
     *
     * <p>Optionally adds error or logout messages to the model when the
     * corresponding query parameters are present (e.g. after a failed login
     * attempt or a successful logout).</p>
     *
     * @param error  if present, indicates a login error occurred
     * @param logout if present, indicates the user has just logged out
     * @param model  the Thymeleaf model
     * @return the login view name
     */
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password.");
        }
        if ("suspended".equals(status)) {
            model.addAttribute("error", "Your account has been suspended. Please contact an administrator.");
        } else if ("banned".equals(status)) {
            model.addAttribute("error", "Your account has been banned. Email us at admin@guideconne.com for assistance.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    /**
     * Displays the registration form.
     *
     * @param model the Thymeleaf model
     * @return the registration view name
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    /**
     * Processes a new user registration.
     *
     * <p>Validates the submitted user data and delegates account creation to
     * {@link UserService#register(String, String, String, Role)}. On success the user is redirected to the
     * login page; on failure the registration form is re-displayed with an error
     * message.</p>
     *
     * @param user  the user data submitted from the registration form
     * @param model the Thymeleaf model
     * @return a redirect to the login page on success, or the registration view on error
     */
    @PostMapping("/register")
    public String registerUser(User user, Model model) {
        try {
            userService.register(user.getDisplayName(), user.getEmail(), user.getPassword(), user.getRole());
            return "redirect:/auth/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        }
    }
}
