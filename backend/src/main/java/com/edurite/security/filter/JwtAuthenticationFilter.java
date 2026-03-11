package com.edurite.security.filter; // declares the package path for this Java file

import com.edurite.security.service.CustomUserDetailsService; // imports a class so it can be used in this file
import com.edurite.security.service.JwtService; // imports a class so it can be used in this file
import jakarta.servlet.FilterChain; // imports a class so it can be used in this file
import jakarta.servlet.ServletException; // imports a class so it can be used in this file
import jakarta.servlet.http.HttpServletRequest; // imports a class so it can be used in this file
import jakarta.servlet.http.HttpServletResponse; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import org.springframework.http.HttpHeaders; // imports a class so it can be used in this file
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // imports a class so it can be used in this file
import org.springframework.security.core.context.SecurityContextHolder; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UserDetails; // imports a class so it can be used in this file
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // imports a class so it can be used in this file
import org.springframework.stereotype.Component; // imports a class so it can be used in this file
import org.springframework.web.filter.OncePerRequestFilter; // imports a class so it can be used in this file

@Component // marks this class as a Spring-managed component bean
/**
 * This class named JwtAuthenticationFilter is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter { // defines a class type

    private final JwtService jwtService; // handles authentication or authorization to protect secure access
    private final CustomUserDetailsService customUserDetailsService; // executes this statement as part of the application logic

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) { // handles authentication or authorization to protect secure access
        this.jwtService = jwtService; // handles authentication or authorization to protect secure access
        this.customUserDetailsService = customUserDetailsService; // executes this statement as part of the application logic
    } // ends the current code block

    @Override // adds metadata that Spring or Java uses at runtime
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) // declares a method that defines behavior for this class
            throws ServletException, IOException { // supports the surrounding application logic
        String header = request.getHeader(HttpHeaders.AUTHORIZATION); // handles authentication or authorization to protect secure access

        if (header == null || !header.startsWith("Bearer ")) { // checks a condition and runs this block only when true
            filterChain.doFilter(request, response); // executes this statement as part of the application logic
            return; // returns a value from this method to the caller
        } // ends the current code block

        String token = header.substring(7); // handles authentication or authorization to protect secure access
        String username; // executes this statement as part of the application logic

        try { // supports the surrounding application logic
            username = jwtService.extractUsername(token); // handles authentication or authorization to protect secure access
        } catch (RuntimeException ex) { // supports the surrounding application logic
            filterChain.doFilter(request, response); // executes this statement as part of the application logic
            return; // returns a value from this method to the caller
        } // ends the current code block

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) { // checks a condition and runs this block only when true
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username); // executes this statement as part of the application logic
            if (jwtService.isTokenValid(token, userDetails)) { // checks a condition and runs this block only when true
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken( // creates a new object instance and stores it in a variable
                        userDetails, // supports the surrounding application logic
                        null, // supports the surrounding application logic
                        userDetails.getAuthorities() // supports the surrounding application logic
                ); // executes this statement as part of the application logic
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // handles authentication or authorization to protect secure access
                SecurityContextHolder.getContext().setAuthentication(authenticationToken); // handles authentication or authorization to protect secure access
            } // ends the current code block
        } // ends the current code block

        filterChain.doFilter(request, response); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
