package com.zbor.repository;

import com.zbor.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    Page<User> findByEvents_id(Long eventId, Pageable pageable);

    boolean existsByTelegramId(Long telegramId);
}
