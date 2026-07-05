package com.zbor.service;

import com.zbor.data.entity.User;
import com.zbor.repository.UserRepository;
import com.zbor.dto.request.TelegramUserData;
import com.zbor.dto.response.MyProfile;
import com.zbor.exceptions.UserNotFoundException;
import com.zbor.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public MyProfile getProfile(Long telegramId){
        return profileMapper.toProfile(userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId)));
    }

    public User getByTelegramId(Long telegramId){
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));
    }

    public void updateByTgData(TelegramUserData tgData) {
        var optionalUser = userRepository.findByTelegramId(tgData.getId());
        User user = optionalUser.orElseThrow(() -> new UserNotFoundException(tgData.getId()));
        user.setFirstName(tgData.getFirstName());
        user.setLastName(tgData.getLastName());
        user.setUsername(tgData.getUsername());
        user.setImageUrl(tgData.getPhotoUrl());
        userRepository.save(user);
    }

    public void createUser(User user) {
        userRepository.save(user);
    }

    public User findByTelegramId(Long telegramId){
        return userRepository.findByTelegramId(telegramId).orElseThrow(() -> new UserNotFoundException(telegramId));
    }

    public Page<User> getParticipants(Long eventId, Pageable pageable) {
        return userRepository.findByEvents_id(eventId, pageable);
    }
}