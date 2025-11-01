package com.softuni.gms.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;

@Configuration
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String redirectUrl = request.getContextPath();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String role = grantedAuthority.getAuthority();

            redirectUrl = switch (role) {
                case "ROLE_ADMIN" -> "/dashboard/admin";
                case "ROLE_MECHANIC" -> "/dashboard/mechanic";
                case "ROLE_USER" -> "/dashboard";
                default -> redirectUrl + "/";
            };

            break;
        }

        response.sendRedirect(redirectUrl);
    }
}
