package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.apache.logging.log4j.util.Strings;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.*;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;

import java.util.ArrayList;
import java.util.List;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/billableService", supportedClass = BillableService.class,
        supportedOpenmrsVersions = {"2.0 - 2.*"})
public class BillableServiceResource extends BaseRestDataResource<BillableService> {

    @Override
    public BillableService newDelegate() {
        return new BillableService();
    }

    @Override
    public Class<? extends IEntityDataService<BillableService>> getServiceClass() {
        return IBillableItemsService.class;
    }

    @Override
    public BillableService getByUniqueId(String uuid) {
        return getService().getByUuid(uuid);
    }

    @Override
    public BillableService save(BillableService delegate) {
        return super.save(delegate);
    }

    @Override
    protected AlreadyPaged<BillableService> doSearch(RequestContext context) {
        Concept serviceType = context.getParameter("serviceType") != null ? Context.getConceptService().getConceptByUuid(
                context.getParameter("serviceType")) : null;
        Concept serviceCategory = context.getParameter("serviceCategory") != null ? Context.getConceptService().getConceptByUuid(
                context.getParameter("serviceCategory")) : null;
        String serviceStatus = context.getParameter("isDisabled");
        BillableServiceStatus status = BillableServiceStatus.ENABLED;
        if (Strings.isNotEmpty(serviceStatus)) {
            if (serviceStatus.equalsIgnoreCase("yes") || serviceStatus.equalsIgnoreCase("1")) {
                status = BillableServiceStatus.DISABLED;
            }
        }

        StockItem stockItem = context.getParameter("stockItem") != null ? Context.getService(StockManagementService.class).getStockItemByUuid(context.getParameter("stockItem"))  : null;
        BillableService searchTemplate = new BillableService();
        searchTemplate.setServiceType(serviceType);
        searchTemplate.setServiceCategory(serviceCategory);
        searchTemplate.setServiceStatus(status);
        if (stockItem != null) {
            searchTemplate.setStockItem(stockItem);
        }

        IBillableItemsService service = Context.getService(IBillableItemsService.class);
        return new AlreadyPaged<>(context, service.findServices(new BillableServiceSearch(searchTemplate, false)), false);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = super.getRepresentationDescription(rep);
        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("name");
            description.addProperty("shortName");
            description.addProperty("concept");
            description.addProperty("serviceType");
            description.addProperty("serviceCategory");
            description.addProperty("servicePrices");
            description.addProperty("serviceStatus");
            description.addProperty("stockItem");
        } else if (rep instanceof CustomRepresentation) {
            //For custom representation, must be null
            // - let the user decide which properties should be included in the response
            description = null;
        }
        return description;
    }

    @PropertyGetter(value = "servicePrices")
    public List<CashierItemPrice> getServicePrices(BillableService instance) {
        return new ArrayList<>(instance.getServicePrices());
    }

    @PropertySetter("servicePrices")
    public void setServicePrices(BillableService instance, List<CashierItemPrice> itemPrices) {
        if (instance.getServicePrices() == null) {
            instance.setServicePrices(new ArrayList<CashierItemPrice>(itemPrices.size()));
        }
        BaseRestDataResource.syncCollection(instance.getServicePrices(), itemPrices);
        for (CashierItemPrice itemPrice : instance.getServicePrices()) {
            itemPrice.setBillableService(instance);
        }
    }

    @PropertyGetter(value = "stockItem")
    public String getStockItem(BillableService instance) {
        try {
            StockItem stockItem = instance.getStockItem();
            return stockItem.getUuid() + ":" + stockItem.getCommonName();
        } catch (Exception e) {
            return "";
        }
    }
    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        return getRepresentationDescription(new DefaultRepresentation());
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        return getCreatableProperties();
    }
}