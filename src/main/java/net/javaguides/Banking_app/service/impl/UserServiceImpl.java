package net.javaguides.Banking_app.service.impl;

import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.entity.Account;
import net.javaguides.Banking_app.entity.AccountStatus;
import net.javaguides.Banking_app.entity.Role;
import net.javaguides.Banking_app.entity.User;
import net.javaguides.Banking_app.exception.UserNotFoundException;
import net.javaguides.Banking_app.mapper.UserMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.RoleRepository;
import net.javaguides.Banking_app.repository.UserRepository;
import net.javaguides.Banking_app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public UserDto registerOrUpdateUser(String clerkUserId, String name, String email, String phoneNumber) {
        log.info("Processing Clerk user login/sync for email: {}", email);

        // Ensure ROLE_USER exists in database
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        Optional<User> existingUser = userRepository.findByClerkUserId(clerkUserId);
        User user;

        if (existingUser.isPresent()) {
            // Update existing user info (name/email may change in Clerk)
            user = existingUser.get();
            user.setName(name);
            user.setEmail(email);
            // Only update phone if a real phone is provided and user doesn't already have one
            if (phoneNumber != null && !phoneNumber.isBlank() && user.getPhoneNumber() == null) {
                user.setPhoneNumber(phoneNumber);
            }
            log.info("Updated existing user ID: {}", user.getId());
        } else {
            // Create new user — phone is intentionally left null here;
            // the user will provide their real phone number via the setup-phone step.
            user = new User();
            user.setClerkUserId(clerkUserId);
            user.setName(name);
            user.setEmail(email);
            user.setPhoneNumber(null); // Phone collected separately during onboarding
            user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
            user = userRepository.save(user);
            log.info("Created new user ID: {} (phone setup pending)", user.getId());
            // Note: bank account is created AFTER the user sets their phone number
        }

        user = userRepository.save(user);
        return UserMapper.mapToUserDto(user);
    }

    @Override
    @Transactional
    public UserDto setupPhone(String email, String phoneNumber) {
        log.info("Setting up phone number for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));

        // Check phone uniqueness
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("This phone number is already registered with another account. Please use a different number.");
        }

        user.setPhoneNumber(phoneNumber);
        user = userRepository.save(user);

        // Now create the banking account if it doesn't already exist
        if (accountRepository.findByUserEmail(email).isEmpty()) {
            Account account = new Account();
            account.setAccountHolderName(user.getName());
            account.setBalance(0.0);
            account.setUser(user);
            account.setAccountStatus(AccountStatus.ACTIVE);
            accountRepository.save(account);
            log.info("Created banking account for user ID: {} with phone: {}", user.getId(), phoneNumber);
        }

        return UserMapper.mapToUserDto(user);
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::mapToUserDto);
    }
}
