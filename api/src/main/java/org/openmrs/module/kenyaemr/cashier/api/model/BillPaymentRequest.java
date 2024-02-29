package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;

public class BillPaymentRequest extends BaseOpenmrsData {
    public static final long serialVersionUID = 0L;
    private int billPaymentRequestId;
    private Bill bill;
    private String merchantRequestID;
    private String checkoutRequestID;
    private boolean requestProcessed;
    public int getBillPaymentRequestId() {
        return billPaymentRequestId;
    }

    public void setBillPaymentRequestId(int billPaymentRequestId) {
        this.billPaymentRequestId = billPaymentRequestId;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getMerchantRequestID() {
        return merchantRequestID;
    }

    public void setMerchantRequestID(String merchantRequestID) {
        this.merchantRequestID = merchantRequestID;
    }

    public String getCheckoutRequestID() {
        return checkoutRequestID;
    }

    public void setCheckoutRequestID(String checkoutRequestID) {
        this.checkoutRequestID = checkoutRequestID;
    }

    public boolean getRequestProcessed() {
        return requestProcessed;
    }

    public void setRequestProcessed(boolean requestProcessed) {
        this.requestProcessed = requestProcessed;
    }

    @Override
    public Integer getId() {
        return billPaymentRequestId;
    }

    @Override
    public void setId(Integer integer) {
        setBillPaymentRequestId(integer);
    }
}
