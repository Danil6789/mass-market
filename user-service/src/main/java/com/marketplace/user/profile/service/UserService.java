package com.marketplace.user.profile.service;

import com.marketplace.user.entity.User;
import com.marketplace.user.exception.UserNotFoundException;
import com.marketplace.user.mapper.UserMapper;
import com.marketplace.user.profile.dto.UpdateProfileRequest;
import com.marketplace.user.profile.dto.UserProfileResponse;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.marketplace.user.constant.ExceptionMessages.USER_NOT_FOUND;

/**
 * Profile-level operations: read current user, update own profile, fetch a
 * specific user (self or ADMIN).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrent(Long userId) {
        return getById(userId);
    }

    @Transactional
    public UserProfileResponse update(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setName(request.getName());
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);
        log.info("User profile updated: id={}", saved.getId());
        return userMapper.toResponse(saved);
    }
}