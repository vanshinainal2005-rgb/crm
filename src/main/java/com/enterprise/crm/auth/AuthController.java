package com.enterprise.crm.auth;

import com.enterprise.crm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JdbcTemplate jdbcTemplate;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JdbcTemplate jdbcTemplate) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

        Map<String, Object> userProfile = getUserProfile(username);
        return ApiResponse.success(userProfile, "Login successful");
    }

    @PostMapping("/demo-switch")
    public ApiResponse<Map<String, Object>> demoSwitch(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        if (username == null || username.trim().isEmpty()) {
            username = "superadmin";
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

        Map<String, Object> userProfile = getUserProfile(username);
        return ApiResponse.success(userProfile, "Switched role to: " + username);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Establish default session as superadmin for immediate full access
            UserDetails userDetails = userDetailsService.loadUserByUsername("superadmin");
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );

            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

            return ApiResponse.success(getUserProfile("superadmin"), "Default demo session established");
        }

        String username = authentication.getName();
        return ApiResponse.success(getUserProfile(username), "Current user profile");
    }

    @GetMapping("/demo-users")
    public ApiResponse<List<Map<String, Object>>> getDemoUsers() {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.email, u.department, r.role_name, r.display_name AS role_display " +
                     "FROM USERS u " +
                     "JOIN USER_ROLES ur ON u.user_id = ur.user_id " +
                     "JOIN ROLES r ON ur.role_id = r.role_id " +
                     "ORDER BY u.user_id ASC";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
        return ApiResponse.success(users, "Demo accounts available");
    }

    private Map<String, Object> getUserProfile(String username) {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.email, u.phone, u.department, u.status " +
                     "FROM USERS u WHERE LOWER(u.username) = LOWER(?)";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, username);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> profile = new HashMap<>(rows.get(0));
        Long userId = ((Number) profile.get("USER_ID")).longValue();

        String rolesSql = "SELECT r.role_name, r.display_name FROM ROLES r JOIN USER_ROLES ur ON r.role_id = ur.role_id WHERE ur.user_id = ?";
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(rolesSql, userId);
        profile.put("roles", roles);

        String notifSql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE user_id = ? AND is_read = 'N'";
        Integer unreadCount = jdbcTemplate.queryForObject(notifSql, Integer.class, userId);
        profile.put("unreadNotifications", unreadCount != null ? unreadCount : 0);

        return profile;
    }
}
