package net.javaguides.Banking_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String transactionType;
    private double amount;
    private Long senderAccountId;
    private Long receiverAccountId;
    private String status;
    private LocalDateTime timestamp;
    private String remarks;
}
