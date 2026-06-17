package net.javaguides.Banking_app.mapper;

import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.entity.Transaction;

public class TransactionMapper {

    public static TransactionDto mapToTransactionDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionType(transaction.getTransactionType().name());
        dto.setAmount(transaction.getAmount());
        dto.setSenderAccountId(transaction.getSenderAccountId());
        dto.setReceiverAccountId(transaction.getReceiverAccountId());
        dto.setStatus(transaction.getStatus().name());
        dto.setTimestamp(transaction.getTimestamp());
        dto.setRemarks(transaction.getRemarks());
        return dto;
    }
}
