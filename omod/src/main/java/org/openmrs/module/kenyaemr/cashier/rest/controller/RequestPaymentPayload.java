package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class help to quickly assemble the payload required to trigger an stk push
 */
public class RequestPaymentPayload {
    @JsonProperty("BusinessShortCode")
    private int BusinessShortCode;
    @JsonProperty("Password")
    private String Password;
    @JsonProperty("Timestamp")
    private String Timestamp;
    @JsonProperty("TransactionType")
    private String TransactionType;
    @JsonProperty("Amount")
    private int Amount;
    @JsonProperty("PartyA")
    private String PartyA;
    @JsonProperty("PartyB")
    private int PartyB;
    @JsonProperty("PhoneNumber")
    private String PhoneNumber;
    @JsonProperty("CallBackURL")
    private String CallBackURL;
    @JsonProperty("AccountReference")
    private String AccountReference;
    @JsonProperty("TransactionDesc")
    private String TransactionDesc;

    public RequestPaymentPayload setBusinessShortCode(int businessShortCode) {
        this.BusinessShortCode = businessShortCode;
        return this;
    }

    public RequestPaymentPayload setPassword(String password) {
        this.Password = password;
        return this;
    }

    public RequestPaymentPayload setTimestamp(String timestamp) {
        this.Timestamp = timestamp;
        return this;
    }

    public RequestPaymentPayload setTransactionType(String transactionType) {
        this.TransactionType = transactionType;
        return this;
    }

    public RequestPaymentPayload setAmount(int amount) {
        this.Amount = amount;
        return this;
    }

    public RequestPaymentPayload setPartyA(String partyA) {
        this.PartyA = partyA;
        return this;
    }

    public RequestPaymentPayload setPartyB(int partyB) {
        this.PartyB = partyB;
        return this;
    }

    public RequestPaymentPayload setPhoneNumber(String phoneNumber) {
        this.PhoneNumber = phoneNumber;
        return this;
    }

    public RequestPaymentPayload setCallBackURL(String callBackURL) {
        this.CallBackURL = callBackURL;
        return this;
    }

    public RequestPaymentPayload setAccountReference(String accountReference) {
        this.AccountReference = accountReference;
        return this;
    }

    public RequestPaymentPayload setTransactionDesc(String transactionDesc) {
        this.TransactionDesc = transactionDesc;
        return this;
    }

    public int getBusinessShortCode() {
        return BusinessShortCode;
    }

    public String getPassword() {
        return Password;
    }

    public String getTimestamp() {
        return Timestamp;
    }

    public String getTransactionType() {
        return TransactionType;
    }

    public int getAmount() {
        return Amount;
    }

    public String getPartyA() {
        return PartyA;
    }

    public int getPartyB() {
        return PartyB;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public String getCallBackURL() {
        return CallBackURL;
    }

    public String getAccountReference() {
        return AccountReference;
    }

    public String getTransactionDesc() {
        return TransactionDesc;
    }
}
