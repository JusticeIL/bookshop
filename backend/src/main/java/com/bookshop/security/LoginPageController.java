package com.bookshop.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the sign-in page used by the OAuth2 authorization endpoint.
 *
 * <p>This is the one deliberately server-rendered view in an otherwise
 * REST/SPA application: in the Authorization Code flow the user must enter
 * their credentials on the authorization server itself, never in the client.
 */
@Controller
public class LoginPageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
