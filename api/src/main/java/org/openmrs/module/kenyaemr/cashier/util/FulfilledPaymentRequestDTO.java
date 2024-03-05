package org.openmrs.module.kenyaemr.cashier.util;

public class FulfilledPaymentRequestDTO {
    private int id;
    private String merchant_request_id;
    private String checkout_request_id;
    private String business_short_code;
    private String amount;
    private String client_phone;
    private String paybill_reference_number;
    private String transaction_reference_number;
    private String datetime;
    private String mpesa_payload;
    private String createdAt;
    private String updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMerchant_request_id() {
        return merchant_request_id;
    }

    public void setMerchant_request_id(String merchant_request_id) {
        this.merchant_request_id = merchant_request_id;
    }

    public String getCheckout_request_id() {
        return checkout_request_id;
    }

    public void setCheckout_request_id(String checkout_request_id) {
        this.checkout_request_id = checkout_request_id;
    }

    public String getBusiness_short_code() {
        return business_short_code;
    }

    public void setBusiness_short_code(String business_short_code) {
        this.business_short_code = business_short_code;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getClient_phone() {
        return client_phone;
    }

    public void setClient_phone(String client_phone) {
        this.client_phone = client_phone;
    }

    public String getPaybill_reference_number() {
        return paybill_reference_number;
    }

    public void setPaybill_reference_number(String paybill_reference_number) {
        this.paybill_reference_number = paybill_reference_number;
    }

    public String getTransaction_reference_number() {
        return transaction_reference_number;
    }

    public void setTransaction_reference_number(String transaction_reference_number) {
        this.transaction_reference_number = transaction_reference_number;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getMpesa_payload() {
        return mpesa_payload;
    }

    public void setMpesa_payload(String mpesa_payload) {
        this.mpesa_payload = mpesa_payload;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
