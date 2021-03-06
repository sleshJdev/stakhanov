package com.icanmining.web.controller;

import com.icanmining.dto.ApplicationUserDto;
import com.icanmining.dto.RoleDto;
import com.icanmining.security.AuthenticationProperties;
import com.icanmining.security.Credentials;
import com.icanmining.security.JwtService;
import com.icanmining.service.ApplicationUserService;
import com.icanmining.dto.AuthInfo;
import com.icanmining.enums.RoleName;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
public class AuthenticationController {
    private final ApplicationUserService applicationUserService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticationProperties authenticationProperties;

    public AuthenticationController(ApplicationUserService applicationUserService, AuthenticationManager authenticationManager,
                                    JwtService jwtService, AuthenticationProperties authenticationProperties) {
        this.applicationUserService = applicationUserService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticationProperties = authenticationProperties;
    }

    @PostMapping("${auth.signInUrl}")
    public AuthInfo signIn(@RequestBody Credentials credentials) {
        ApplicationUserDto user = applicationUserService.findByUsername(credentials.getUsername());
        if (user == null) {
            throw new AuthenticationServiceException("User not found");
        }
        List<String> roleNames = user.getRoles().stream()
                .map(RoleDto::getName).map(Enum::toString).collect(toList());

        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        credentials.getUsername(),
                        credentials.getPassword(),
                        roleNames.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(toList())));

        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String rawToken = jwtService.generateToken(user);
        return new AuthInfo(
                new AuthInfo.Token(authenticationProperties.getHeaderString(), authenticationProperties.getTokenPrefix() + rawToken),
                new AuthInfo.User(user.getUsername(), roleNames.stream().map(RoleName::valueOf).collect(toList())));
    }

    @PostMapping("${auth.signUpUrl}")
    public void signUp(@RequestBody Credentials credentials) {
        applicationUserService.saveUser(credentials);
    }
}
