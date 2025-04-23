package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.openmrs.Concept;
import org.openmrs.api.APIException;
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
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.List;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/billableService", 
        supportedClass = BillableService.class,
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
        if (StringUtils.isBlank(uuid)) {
            throw new APIException("UUID cannot be empty");
        }
        BillableService service = getService().getByUuid(uuid);
        if (service == null) {
            throw new APIException("Billable service not found with UUID: " + uuid);
        }
        return service;
    }

    @Override
    public BillableService save(BillableService delegate) {
        validateBillableService(delegate);
        checkForDuplicateService(delegate);
        return super.save(delegate);
    }

    @Override
    protected AlreadyPaged<BillableService> doSearch(RequestContext context) {
        try {
            BillableService searchTemplate = createSearchTemplate(context);
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            List<BillableService> results = service.findServices(new BillableServiceSearch(searchTemplate, false));
            return new AlreadyPaged<>(context, results, false);
        } catch (Exception e) {
            throw new APIException("Error searching billable services: " + e.getMessage(), e);
        }
    }

    private BillableService createSearchTemplate(RequestContext context) {
        BillableService searchTemplate = new BillableService();
        
        // Handle service type
        String serviceTypeUuid = context.getParameter("serviceType");
        if (StringUtils.isNotBlank(serviceTypeUuid)) {
            Concept serviceType = Context.getConceptService().getConceptByUuid(serviceTypeUuid);
            if (serviceType == null) {
                throw new APIException("Invalid service type UUID: " + serviceTypeUuid);
            }
            searchTemplate.setServiceType(serviceType);
        }

        // Handle service category
        String serviceCategoryUuid = context.getParameter("serviceCategory");
        if (StringUtils.isNotBlank(serviceCategoryUuid)) {
            Concept serviceCategory = Context.getConceptService().getConceptByUuid(serviceCategoryUuid);
            if (serviceCategory == null) {
                throw new APIException("Invalid service category UUID: " + serviceCategoryUuid);
            }
            searchTemplate.setServiceCategory(serviceCategory);
        }

        // Handle service status
        String serviceStatus = context.getParameter("isDisabled");
        if (Strings.isNotEmpty(serviceStatus)) {
            searchTemplate.setServiceStatus(
                serviceStatus.equalsIgnoreCase("yes") || serviceStatus.equalsIgnoreCase("1") 
                ? BillableServiceStatus.DISABLED 
                : BillableServiceStatus.ENABLED
            );
        }

        // Handle stock item
        String stockItemUuid = context.getParameter("stockItem");
        if (StringUtils.isNotBlank(stockItemUuid)) {
            StockItem stockItem = Context.getService(StockManagementService.class).getStockItemByUuid(stockItemUuid);
            if (stockItem == null) {
                throw new APIException("Invalid stock item UUID: " + stockItemUuid);
            }
            searchTemplate.setStockItem(stockItem);
        }

        return searchTemplate;
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
            description.addProperty("creator", Representation.REF);
            description.addProperty("dateCreated");
            description.addProperty("changedBy", Representation.REF);
            description.addProperty("dateChanged");
            description.addProperty("voided");
            description.addProperty("voidedBy", Representation.REF);
            description.addProperty("dateVoided");
            description.addProperty("voidReason");
        } else if (rep instanceof CustomRepresentation) {
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
        if (itemPrices == null) {
            throw new APIException("Service prices cannot be null");
        }
        if (instance.getServicePrices() == null) {
            instance.setServicePrices(new ArrayList<>(itemPrices.size()));
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
            return stockItem != null ? stockItem.getUuid() + ":" + stockItem.getCommonName() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = getRepresentationDescription(new DefaultRepresentation());
        description.addRequiredProperty("name");
        description.addRequiredProperty("concept");
        description.addRequiredProperty("servicePrices");
        return description;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        return getCreatableProperties();
    }

    private void validateBillableService(BillableService service) {
        if (service == null) {
            throw new APIException("Billable service cannot be null");
        }
        if (StringUtils.isBlank(service.getName())) {
            throw new APIException("Name is required");
        }
        if (service.getConcept() == null) {
            throw new APIException("Concept is required");
        }
        if (service.getServicePrices() == null || service.getServicePrices().isEmpty()) {
            throw new APIException("At least one service price is required");
        }
    }

    private void checkForDuplicateService(BillableService service) {
        if (service.getConcept() != null) {
            BillableServiceSearch search = new BillableServiceSearch();
            BillableService template = new BillableService();
            template.setConcept(service.getConcept());
            search.setTemplate(template);
            
            List<BillableService> existingServices = getService().findServices(search);
            if (!existingServices.isEmpty() && 
                (service.getId() == null || !existingServices.get(0).getId().equals(service.getId()))) {
                throw new APIException("A billable service with this concept already exists");
            }
        }
    }
}