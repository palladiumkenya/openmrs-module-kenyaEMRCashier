package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.module.stockmanagement.api.model.StockItem;

import java.math.BigDecimal;

public class CashierItemPrice extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;

	private Integer itemPriceId;

	private String name;
	private BigDecimal price;
	private PaymentMode paymentMode;
	private BillableService billableService;
	private BigDecimal oldPrice;

	public CashierItemPrice() {

	}

	public CashierItemPrice(String name, BigDecimal price, BigDecimal old_Price, StockItem item, BillableService billableService) {
		this.name = name;
		this.price = price;
		this.oldPrice = old_Price;
		this.billableService = billableService;
	}

	public CashierItemPrice(BigDecimal price, BigDecimal oldPrice , String name) {
		super();

		this.price = price;
		this.oldPrice = oldPrice;
		setName(name);
	}

	@Override
	public Integer getId() {
		return itemPriceId;
	}

	@Override
	public void setId(Integer id) {
		itemPriceId = id;
	}

	public PaymentMode getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(PaymentMode paymentMode) {
		this.paymentMode = paymentMode;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getOldPrice() {
		return oldPrice;
	}

	public void setOldPrice(BigDecimal oldPrice) {
		this.oldPrice = oldPrice;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public BillableService getBillableService() {
		return billableService;
	}

	public void setBillableService(BillableService billableService) {
		this.billableService = billableService;
	}
}
