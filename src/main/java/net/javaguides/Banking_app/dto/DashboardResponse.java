package net.javaguides.Banking_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private double accountBalance;
    private double totalDeposits;
    private double totalWithdrawals;
    private List<TransactionDto> recentTransactions;
    // Account info for frontend display
    private Long accountId;
    private String accountHolderName;
    private String phoneNumber;
    private String accountStatus;
}
