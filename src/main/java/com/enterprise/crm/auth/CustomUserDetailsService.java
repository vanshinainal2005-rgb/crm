package com.enterprise.crm.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String userSql = "SELECT user_id, username, password_hash, status FROM USERS WHERE LOWER(username) = LOWER(?)";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(userSql, username);

        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        Map<String, Object> user = rows.get(0);
        Long userId = ((Number) user.get("USER_ID")).longValue();
        String password = (String) user.get("PASSWORD_HASH");
        String status = (String) user.get("STATUS");

        boolean enabled = "ACTIVE".equalsIgnoreCase(status);

        String rolesSql = "SELECT r.role_name FROM ROLES r JOIN USER_ROLES ur ON r.role_id = ur.role_id WHERE ur.user_id = ?";
        List<String> roleNames = jdbcTemplate.query(rolesSql, (rs, rowNum) -> rs.getString("role_name"), userId);

        List<SimpleGrantedAuthority> authorities = roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return User.builder()
                .username(username)
                .password(password)
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }
}
