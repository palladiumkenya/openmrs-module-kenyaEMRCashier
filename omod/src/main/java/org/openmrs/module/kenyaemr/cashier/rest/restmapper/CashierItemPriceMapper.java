package org.openmrs.module.kenyaemr.cashier.rest.restmapper;

import java.math.BigDecimal;

public class CashierItemPriceMapper {
    private String name;
    private BigDecimal price;
    private String paymentMode;
    private String uuid;
    public CashierItemPriceMapper() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
