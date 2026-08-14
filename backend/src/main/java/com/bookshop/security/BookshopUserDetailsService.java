package com.bookshop.security;

import com.bookshop.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the sign-in form's username (an email address) to an account. */
@Service
public class BookshopUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public BookshopUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(BookshopUserDetails::new)
                // Same generic wording as a wrong password: no account enumeration.
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
