package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;

import java.math.BigDecimal;

/**
 * Model class that represents a transaction on a deposit.
 */
public class DepositTransaction extends BaseOpenmrsData {
    private Integer depositTransactionId;
    private Deposit deposit;
    private BillLineItem billLineItem;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String reason;
    private String receiptNumber;

    @Override
    public Integer getId() {
        return depositTransactionId;
    }

    @Override
    public void setId(Integer id) {
        this.depositTransactionId = id;
    }

    public Integer getDepositTransactionId() {
        return depositTransactionId;
    }

    public void setDepositTransactionId(Integer depositTransactionId) {
        this.depositTransactionId = depositTransactionId;
    }

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit deposit) {
        this.deposit = deposit;
    }

    public BillLineItem getBillLineItem() {
        return billLineItem;
    }

    public void setBillLineItem(BillLineItem billLineItem) {
        this.billLineItem = billLineItem;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }
} 