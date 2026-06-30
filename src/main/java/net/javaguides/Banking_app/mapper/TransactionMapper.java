package net.javaguides.Banking_app.mapper;
// ↑ Belongs to the "mapper" package — mappers convert between Entity (DB) and DTO (API) objects.

import net.javaguides.Banking_app.dto.TransactionDto;
// ↑ TransactionDto is the data container used in API responses.

import net.javaguides.Banking_app.entity.Transaction;
// ↑ Transaction is the database entity (the "transactions" table row).

// ============================================================
// WHAT IS TransactionMapper?
// A simple utility class that converts Transaction entities → TransactionDto objects.
//
// WHY DO WE NEED THIS?
// The database Transaction entity might contain internal fields that shouldn't be exposed
// (or the DTO might have different field names). The mapper is the bridge between them.
//
// Note: We only need ONE direction here (Entity → DTO) because:
//   - Transactions are CREATED by the service layer (not directly from DTO).
//   - Transactions are NEVER updated via an API endpoint (they are immutable records).
//
// No @Component or @Service annotation — this is a STATIC utility class.
// "static" methods means you call it as: TransactionMapper.mapToTransactionDto(transaction)
// (No need to create an instance with "new TransactionMapper()")
// ============================================================
public class TransactionMapper {

    // Convert a Transaction entity (from database) → TransactionDto (for API response)
    // "static" → Call this without creating an instance: TransactionMapper.mapToTransactionDto(t)
    public static TransactionDto mapToTransactionDto(Transaction transaction) {
        if (transaction == null) {
            return null; // Null-safety: if input is null, return null to avoid NullPointerException
        }

        TransactionDto dto = new TransactionDto(); // Create a blank DTO object

        dto.setId(transaction.getId());             // Copy the transaction ID
        dto.setTransactionType(transaction.getTransactionType().name());
        // .name() on an enum returns its string name: TransactionType.DEPOSIT → "DEPOSIT"
        // This converts the Enum to a plain String for the JSON response.

        dto.setAmount(transaction.getAmount());                 // Copy the amount (e.g., 500.0)
        dto.setSenderAccountId(transaction.getSenderAccountId()); // Can be null for deposits
        dto.setReceiverAccountId(transaction.getReceiverAccountId()); // Can be null for withdrawals
        dto.setStatus(transaction.getStatus().name());          // "SUCCESS" or "FAILED"
        dto.setTimestamp(transaction.getTimestamp());           // The transaction datetime
        dto.setRemarks(transaction.getRemarks());               // Optional note (can be null)

        return dto; // Return the fully populated DTO ready for JSON serialization
    }
    // NOTE: There is no mapToTransaction() (DTO → Entity) method here because
    //       transactions are always created in the service layer, never from a DTO directly.
}
