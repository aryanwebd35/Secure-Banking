package net.javaguides.Banking_app.service;

import java.util.List;

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.DashboardResponse;

public interface AccountService {

    AccountDto createAccount(AccountDto accountDto);

    AccountDto getAccountById(Long id);

    AccountDto deposit(Long id, double amount);

    AccountDto withdraw(Long id, double amount);

    List<AccountDto> getAllAccounts();

    void deleteAccount(Long id);

    DashboardResponse getUserDashboard(String email);

    AccountDto getMyAccount(String email);

}
