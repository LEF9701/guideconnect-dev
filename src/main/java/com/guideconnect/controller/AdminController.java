package com.guideconnect.controller;

import com.guideconnect.model.AccountStatus;
import com.guideconnect.model.User;
import com.guideconnect.service.AdminService;
import com.guideconnect.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for administrative operations.
 *
 * <p>Provides an admin dashboard with platform metrics, paginated user
 * management with search and status actions (suspend, ban, activate), and
 * dispute resolution capabilities.</p>
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    /**
     * Constructs an {@code AdminController} with the required service dependencies.
     *
     * @param adminService the service for admin-specific operations and metrics
     * @param userService  the service for user lookup
     */
    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    /**
     * Displays the admin dashboard with platform-wide metrics.
     *
     * @param principal the currently authenticated user's security principal
     * @param model     the Thymeleaf model
     * @return the admin dashboard view name
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        java.util.Map<String, Object> metrics = adminService.getDashboardMetrics();
        model.addAllAttributes(metrics);
        model.addAttribute("recentUsers", adminService.getRecentRegistrations(10));
        model.addAttribute("openDisputes", adminService.getActiveDisputes(5));
        return "admin/dashboard";
    }

    /**
     * Displays a paginated list of users with optional search filtering.
     *
     * @param search optional search term to filter users by name or email
     * @param page   the zero-based page index; defaults to 0
     * @param size   the page size; defaults to 20
     * @param model  the Thymeleaf model
     * @return the admin users view name
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String search,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        if (search != null && !search.isBlank()) {
            model.addAttribute("users", userService.searchUsers(search, pageable));
        } else {
            model.addAttribute("users", userService.findAllUsers(pageable));
        }
        model.addAttribute("search", search);
        return "admin/users";
    }

    /**
     * Suspends a user account.
     *
     * @param id the ID of the user to suspend
     * @return a redirect to the admin users page
     */
    @PostMapping("/users/{id}/suspend")
    public String suspendUser(@PathVariable Long id) {
        userService.updateUserStatus(id, AccountStatus.SUSPENDED);
        return "redirect:/admin/users";
    }

    /**
     * Bans a user account.
     *
     * @param id the ID of the user to ban
     * @return a redirect to the admin users page
     */
    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable Long id) {
        userService.updateUserStatus(id, AccountStatus.BANNED);
        return "redirect:/admin/users";
    }

    /**
     * Reactivates a suspended or banned user account.
     *
     * @param id the ID of the user to activate
     * @return a redirect to the admin users page
     */
    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id) {
        userService.updateUserStatus(id, AccountStatus.ACTIVE);
        return "redirect:/admin/users";
    }

    /**
     * Displays a paginated list of disputes.
     *
     * @param page  the zero-based page index; defaults to 0
     * @param size  the page size; defaults to 20
     * @param model the Thymeleaf model
     * @return the admin disputes view name
     */
    @GetMapping("/disputes")
    public String listDisputes(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("disputes", adminService.getAllDisputes(pageable));
        return "admin/disputes";
    }

    @GetMapping("/disputes/{id}")
    public String showDispute(@PathVariable Long id, Model model) {
        model.addAttribute("dispute", adminService.getDisputeById(id));
        return "admin/dispute-detail";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAllAttributes(adminService.getReportMetrics());
        model.addAttribute("recentUsers", adminService.getRecentRegistrations(10));
        model.addAttribute("openDisputes", adminService.getActiveDisputes(10));
        return "admin/reports";
    }

    /**
     * Resolves a dispute with a resolution text provided by the admin.
     *
     * @param id         the ID of the dispute to resolve
     * @param resolution the resolution text entered by the admin
     * @param action     optional action to take against the offender (BAN, SUSPEND, or null)
     * @return a redirect to the admin disputes page
     */
    @PostMapping("/disputes/{id}/resolve")
    public String resolveDispute(@PathVariable Long id,
                                 @RequestParam String resolution,
                                 @RequestParam(required = false) String action) {
        adminService.resolveDispute(id, resolution, action);
        return "redirect:/admin/disputes";
    }
}
