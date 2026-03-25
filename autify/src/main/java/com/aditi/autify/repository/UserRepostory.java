package com.aditi.autify.repository;

import com.aditi.autify.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepostory extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String Email);

    Boolean existsByEmail(String email);

}
