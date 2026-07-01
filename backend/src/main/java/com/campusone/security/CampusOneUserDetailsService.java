package com.campusone.security;

import com.campusone.common.util.EmailNormalizer;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampusOneUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CampusOneUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public CampusOneUserPrincipal loadUserById(UUID userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return toPrincipal(user);
    }

    private CampusOneUserPrincipal toPrincipal(User user) {
        return CampusOneUserPrincipal.from(user);
    }
}
