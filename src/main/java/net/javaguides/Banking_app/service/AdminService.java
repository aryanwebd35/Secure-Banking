package net.javaguides.Banking_app.service;

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminService {
    Page<UserDto> getAllUsers(Pageable pageable);
    Page<AccountDto> getAllAccounts(Pageable pageable);
    AccountDto updateAccountStatus(Long accountId, String status);
    Map<String, Object> getAdminDashboardStats();
}
