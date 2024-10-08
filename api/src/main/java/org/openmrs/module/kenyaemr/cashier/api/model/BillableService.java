package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.module.stockmanagement.api.model.StockItem;

import java.util.ArrayList;
import java.util.List;

/**
 * This has been expanded to manage pricing of both services and commodity.
 * For commodity, the stockItem property is specified and this links to a commodity in the stock management module
 */
public class BillableService extends BaseOpenmrsData {

    public static final long serialVersionUID = 0L;

    private int billableServiceId;
    private String name;
    private String shortName;
    private Concept concept;
    private Concept serviceType;
    private StockItem stockItem;
    private Concept serviceCategory;
    private List<CashierItemPrice> servicePrices;
    private BillableServiceStatus serviceStatus = BillableServiceStatus.ENABLED;

    public int getBillableServiceId() {
        return billableServiceId;
    }

    public void setBillableServiceId(int billableServiceId) {
        this.billableServiceId = billableServiceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Concept getServiceType() {
        return serviceType;
    }

    public void setServiceType(Concept serviceType) {
        this.serviceType = serviceType;
    }

    public Concept getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(Concept serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public BillableServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(BillableServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public Integer getId() {
        return getBillableServiceId();
    }

    @Override
    public void setId(Integer integer) {
        setBillableServiceId(integer);
    }


    public List<CashierItemPrice> getServicePrices() {
        return servicePrices;
    }

    public void setServicePrices(List<CashierItemPrice> servicePrices) {
        this.servicePrices = servicePrices;
    }

    public Concept getConcept() {
        return concept;
    }

    public StockItem getStockItem() {
        return stockItem;
    }

    public void setStockItem(StockItem stockItem) {
        this.stockItem = stockItem;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public void addServicePrice(CashierItemPrice price) {
        if (price == null) {
            throw new NullPointerException("Service Price must be defined.");
        }

        if (this.servicePrices == null) {
            this.servicePrices = new ArrayList<CashierItemPrice>();
        }

        this.servicePrices.add(price);
        price.setBillableService(this);
    }
}