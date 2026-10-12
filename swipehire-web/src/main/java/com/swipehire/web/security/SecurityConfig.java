package com.swipehire.web.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * URL rules per role, form login, BCrypt. CSRF protection stays ON: every JSP form includes the
 * token through the &lt;sh:csrf/&gt; tag.
 */
@Configuration
public class SecurityConfig {

    /** Key under which the user id is stored in the HttpSession, read by the WebSocket endpoint. */
    public static final String SESSION_USER_ID = "userId";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Load the CSRF token on every request (not lazily) so JSPs can always render it.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(c -> c.csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth
                        // JSP rendering is an internal FORWARD; error pages are an ERROR dispatch.
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.INCLUDE).permitAll()
                        .requestMatchers("/", "/login", "/signup", "/static/**", "/error").permitAll()
                        .requestMatchers("/applicant/**").hasRole("APPLICANT")
                        .requestMatchers("/hirer/**").hasRole("HIRER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(roleBasedSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    /** After login: remember the user id for WebSockets and send each role to its home page. */
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            AppUserPrincipal me = (AppUserPrincipal) authentication.getPrincipal();
            HttpSession session = request.getSession();
            session.setAttribute(SESSION_USER_ID, me.getId());
            String target = switch (me.getRole()) {
                case APPLICANT -> "/applicant/deck";
                case HIRER -> "/hirer/dashboard";
                case ADMIN -> "/admin";
            };
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}
