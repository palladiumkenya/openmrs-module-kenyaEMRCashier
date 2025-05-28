package org.openmrs.module.kenyaemr.cashier.rest.restmapper;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.ICashierItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentModeService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.openmrs.module.stockmanagement.api.StockManagementService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BillableServiceMapper {
    private String name;
    private String shortName;
    private String concept;
    private String serviceType;
    private String serviceCategory;
    private String stockItem;
    private List<CashierItemPriceMapper> servicePrices;
    private BillableServiceStatus serviceStatus = BillableServiceStatus.ENABLED;
    private String uuid;

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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public List<CashierItemPriceMapper> getServicePrices() {
        return servicePrices;
    }

    public void setServicePrices(List<CashierItemPriceMapper> servicePrices) {
        this.servicePrices = servicePrices;
    }

    public BillableServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(BillableServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getStockItem() {
        return stockItem;
    }

    public void setStockItem(String stockItem) {
        this.stockItem = stockItem;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public BillableService billableServiceMapper(BillableServiceMapper mapper) {
        BillableService service = new BillableService();
        List<CashierItemPrice> servicePrices = new ArrayList<>();
        service.setName(mapper.getName());
        service.setShortName(mapper.getShortName());
        service.setConcept(Context.getConceptService().getConceptByUuid(mapper.getConcept()));
        service.setServiceType(Context.getConceptService().getConceptByUuid(mapper.getServiceType()));
        service.setServiceCategory(Context.getConceptService().getConceptByUuid(mapper.getServiceCategory()));
        service.setServiceStatus(mapper.getServiceStatus());
        if (StringUtils.isNotBlank(mapper.getStockItem())) {
            service.setStockItem(Context.getService(StockManagementService.class).getStockItemByUuid(mapper.getStockItem()));
        }
        for (CashierItemPriceMapper itemPrice : mapper.getServicePrices()) {
            CashierItemPrice price = new CashierItemPrice();
            price.setName(itemPrice.getName());
            price.setPrice(itemPrice.getPrice());
            price.setPaymentMode(Context.getService(IPaymentModeService.class).getByUuid(itemPrice.getPaymentMode()));
            price.setBillableService(service);
            servicePrices.add(price);
        }
        service.setServicePrices(servicePrices);
        return service;
    }
    public BillableService billableServiceUpdateMapper(BillableServiceMapper mapper) {
        BillableService service = Context.getService(IBillableItemsService.class).getByUuid(mapper.getUuid());
        service.setName(mapper.getName());
        service.setShortName(mapper.getShortName());
        service.setConcept(Context.getConceptService().getConceptByUuid(mapper.getConcept()));
        service.setServiceType(Context.getConceptService().getConceptByUuid(mapper.getServiceType()));
        service.setServiceCategory(Context.getConceptService().getConceptByUuid(mapper.getServiceCategory()));
        service.setServiceStatus(mapper.getServiceStatus());
        if (StringUtils.isNotBlank(mapper.getStockItem())) {
            service.setStockItem(Context.getService(StockManagementService.class).getStockItemByUuid(mapper.getStockItem()));
        }
        // Pass the new prices from the mapper to syncCollection
        List<CashierItemPrice> updatedPrices = mapper.getServicePrices().stream().map(itemPrice -> {
            CashierItemPrice price = new CashierItemPrice();
            price.setName(itemPrice.getName());
            price.setPrice(itemPrice.getPrice());
            if (itemPrice.getUuid() != null) {
                CashierItemPrice existingItem = Context.getService(ICashierItemPriceService.class).getByUuid(itemPrice.getUuid());
                if (!existingItem.getPrice().equals(itemPrice.getPrice())) {
                    price.setOldPrice(existingItem.getPrice());
                } else {
                    //No change with the price of the item/ keep the old price
                    price.setOldPrice(existingItem.getOldPrice());
                }
            }

            price.setPaymentMode(Context.getService(IPaymentModeService.class).getByUuid(itemPrice.getPaymentMode()));
            price.setBillableService(service);
            return price;
        }).collect(Collectors.toList());

        service.setServicePrices(syncCollection(service.getServicePrices(), updatedPrices));
        return service;
    }

    private List<CashierItemPrice> syncCollection(List<CashierItemPrice> existingPrices, List<CashierItemPrice> newPrices) {
        Map<String, CashierItemPrice> newPricesMap = newPrices.stream()
                .collect(Collectors.toMap(
                        price -> price.getPaymentMode().getUuid(),
                        price -> price,
                        (existing, replacement) -> replacement
                ));

        Iterator<CashierItemPrice> iterator = existingPrices.iterator();
        while (iterator.hasNext()) {
            CashierItemPrice existingPrice = iterator.next();
            CashierItemPrice newPrice = newPricesMap.remove(existingPrice.getPaymentMode().getUuid());

            if (newPrice == null) {
                iterator.remove();
            } else {
                existingPrice.setName(newPrice.getName());
                existingPrice.setPrice(newPrice.getPrice());
                existingPrice.setOldPrice(newPrice.getOldPrice());
                existingPrice.setPaymentMode(newPrice.getPaymentMode());
                existingPrice.setBillableService(newPrice.getBillableService());
            }
        }

        // Add all new prices that weren't matched with existing ones
        existingPrices.addAll(newPricesMap.values());
        System.out.printf("Synchronized Collection: %s%n", existingPrices);
        return existingPrices;
    }
}
