package net.javaguides.Banking_app.service;

import net.javaguides.Banking_app.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto registerOrUpdateUser(String clerkUserId, String name, String email, String phoneNumber);
    UserDto setupPhone(String email, String phoneNumber);
    Page<UserDto> getAllUsers(Pageable pageable);
}

