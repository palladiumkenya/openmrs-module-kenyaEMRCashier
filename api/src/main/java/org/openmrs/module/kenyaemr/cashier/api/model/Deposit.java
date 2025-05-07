package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Model class that represents a deposit made by a patient for expensive hospital billable items.
 */
public class Deposit extends BaseOpenmrsData {
    private Integer depositId;
    private Patient patient;
    private BigDecimal amount;
    private String depositType;
    private DepositStatus status;
    private String referenceNumber;
    private String description;
    private Set<DepositTransaction> transactions;

    public Deposit() {
        this.transactions = new HashSet<>();
    }

    @Override
    public Integer getId() {
        return depositId;
    }

    @Override
    public void setId(Integer id) {
        this.depositId = id;
    }

    public Integer getDepositId() {
        return depositId;
    }

    public void setDepositId(Integer depositId) {
        this.depositId = depositId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDepositType() {
        return depositType;
    }

    public void setDepositType(String depositType) {
        this.depositType = depositType;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public void setStatus(DepositStatus status) {
        this.status = status;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DepositTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<DepositTransaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(DepositTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("The transaction to add must be defined.");
        }

        if (this.transactions == null) {
            this.transactions = new HashSet<>();
        }

        transaction.setDeposit(this);
        this.transactions.add(transaction);
    }

    public void removeTransaction(DepositTransaction transaction) {
        if (transaction != null && this.transactions != null) {
            this.transactions.remove(transaction);
        }
    }

    public BigDecimal getAvailableBalance() {
        BigDecimal balance = amount;
        if (transactions != null) {
            for (DepositTransaction transaction : transactions) {
                if (!transaction.getVoided()) {
                    if (transaction.getTransactionType() == TransactionType.APPLY) {
                        balance = balance.subtract(transaction.getAmount());
                    } else if (transaction.getTransactionType() == TransactionType.REFUND) {
                        balance = balance.subtract(transaction.getAmount());
                    } else if (transaction.getTransactionType() == TransactionType.REVERSE) {
                        balance = balance.add(transaction.getAmount());
                    }
                }
            }
        }
        return balance;
    }

    public String getDisplay() {
        StringBuilder display = new StringBuilder();
        if (patient != null) {
            display.append(patient.getPersonName().getFullName());
        }
        display.append(" - ");
        display.append(amount);
        display.append(" - ");
        display.append(status);
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            display.append(" (");
            display.append(referenceNumber);
            display.append(")");
        }
        return display.toString();
    }
} 