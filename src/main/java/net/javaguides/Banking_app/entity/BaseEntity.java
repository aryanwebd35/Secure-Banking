package net.javaguides.Banking_app.entity;
// ↑ "package" declares the folder where this file belongs.

import jakarta.persistence.Column;
// ↑ @Column is used to customize how a Java field maps to a database column (e.g., its name, size).

import jakarta.persistence.EntityListeners;
// ↑ @EntityListeners wires a "listener class" that gets called when database events happen (save, update, etc.)

import jakarta.persistence.MappedSuperclass;
// ↑ @MappedSuperclass tells JPA: "This class is NOT its own database table.
//   Instead, its fields (createdAt, updatedAt) get INHERITED by all child entity tables."

import lombok.Getter;
import lombok.Setter;
// ↑ Lombok annotations: @Getter auto-generates getCreatedAt(), getUpdatedAt() methods.
//                       @Setter auto-generates setCreatedAt(), setUpdatedAt() methods.
//   Without Lombok, you'd have to write all these boilerplate getter/setter methods yourself.

import org.springframework.data.annotation.CreatedDate;
// ↑ @CreatedDate tells Spring: "When a new record is saved to DB for the FIRST TIME,
//   automatically fill this field with the current timestamp."

import org.springframework.data.annotation.LastModifiedDate;
// ↑ @LastModifiedDate tells Spring: "EVERY TIME a record is updated and saved again,
//   automatically update this field with the latest timestamp."

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
// ↑ This is the listener class that actually PERFORMS the auto-date filling.
//   It listens for save/update events and populates @CreatedDate and @LastModifiedDate fields.

import java.time.LocalDateTime;
// ↑ Java's modern date-time class. Stores date AND time (e.g., 2024-06-19T10:30:00).

// ============================================================
// WHAT IS BaseEntity?
// It's a PARENT CLASS that every database entity in this project extends from.
// Instead of writing "createdAt" and "updatedAt" in EVERY table (Account, Transaction, User),
// we write it ONCE here and all tables automatically inherit it.
//
// ANALOGY: Think of it like a template form that every table inherits from.
// ============================================================

// @Getter → Lombok generates getCreatedAt() and getUpdatedAt() for us automatically
@Getter
// @Setter → Lombok generates setCreatedAt() and setUpdatedAt() for us automatically
@Setter
// @MappedSuperclass → This class does NOT become a table itself.
//   But Account, Transaction, User tables WILL have createdAt + updatedAt columns
//   because they extend this class.
@MappedSuperclass
// @EntityListeners(AuditingEntityListener.class) → Attach the Spring auditing listener.
//   This listener is responsible for auto-setting @CreatedDate and @LastModifiedDate values.
//   NOTE: For this to work, JpaConfig must have @EnableJpaAuditing (see config/JpaConfig.java)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    // "abstract" means you can NEVER create a BaseEntity object directly.
    // It MUST be extended (inherited) by another class like Account, Transaction, etc.

    // @CreatedDate → Spring automatically fills this with the current time when a record is first inserted.
    // @Column(name = "created_at", updatable = false) → 
    //   - name = "created_at"  : The actual column name in MySQL table will be "created_at"
    //   - updatable = false    : This value is set ONLY once (at creation) and NEVER changed after that.
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // @LastModifiedDate → Spring automatically updates this to the current time every time the record is saved.
    // @Column(name = "updated_at") → The actual column name in MySQL table will be "updated_at"
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
