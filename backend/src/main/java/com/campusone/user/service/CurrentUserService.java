package com.campusone.user.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.entity.User;
import com.campusone.user.mapper.CurrentUserMapper;
import com.campusone.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CurrentUserMapper currentUserMapper;

    public CurrentUserService(
            UserRepository userRepository,
            CurrentUserMapper currentUserMapper) {
        this.userRepository = userRepository;
        this.currentUserMapper = currentUserMapper;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findDetailedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return currentUserMapper.toResponse(user);
    }
}
