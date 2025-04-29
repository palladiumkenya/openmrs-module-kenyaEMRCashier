package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.apache.commons.lang3.StringUtils;
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
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
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
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.*;

/**
 * {@link Resource} for {@link BillableService}, supporting CRUD operations
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
        + "/billableService", supportedClass = BillableService.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillableServiceResource extends BaseRestDataResource<BillableService> {

    /**
     * Creates a new instance of BillableService
     * 
     * @return a new BillableService instance
     */
    @Override
    public BillableService newDelegate() {
        return new BillableService();
    }

    /**
     * Gets the service class for this resource
     * 
     * @return the service class
     */
    @Override
    public Class<? extends IEntityDataService<BillableService>> getServiceClass() {
        return IBillableItemsService.class;
    }

    /**
     * Gets a billable service by uuid
     * 
     * @param uuid the uuid of the billable service
     * @return the billable service
     * @throws ObjectNotFoundException if service not found
     */
    @Override
    public BillableService getByUniqueId(String uuid) {
        BillableService service = getService().getByUuid(uuid);
        if (service == null) {
            throw new ObjectNotFoundException();
        }
        return service;
    }

    /**
     * Saves a billable service
     * 
     * @param delegate the billable service to save
     * @return the saved billable service
     * @throws ResourceDoesNotSupportOperationException if validation fails or
     *                                                  duplicate service exists
     */
    @Override
    public BillableService save(BillableService delegate) {
        // Validate before saving
        validateBillableService(delegate);

        // Get the service
        IBillableItemsService billableItemsService = Context.getService(IBillableItemsService.class);

        if (delegate.getId() == null) {
            BillableServiceSearch search = new BillableServiceSearch();
            search.getTemplate().setName(delegate.getName());

            List<BillableService> existingServices = billableItemsService.findServices(search);

            String normalizedNewName = delegate.getName().trim().toLowerCase();

            for (BillableService existingService : existingServices) {
                String normalizedExistingName = existingService.getName().trim().toLowerCase();

                if (normalizedExistingName.equals(normalizedNewName)) {
                    throw new ResourceDoesNotSupportOperationException(
                            "Cannot save billable service. A service with name '" +
                                    delegate.getName() + "' already exists (UUID: " +
                                    existingService.getUuid() + "). Please use a different name.");
                }
            }
        } else {

            String normalizedNewName = delegate.getName().trim().toLowerCase();
            BillableServiceSearch search = new BillableServiceSearch();
            search.getTemplate().setName(delegate.getName());
            List<BillableService> existingServices = billableItemsService.findServices(search);

            for (BillableService existingService : existingServices) {
                if (existingService.getId().equals(delegate.getId())) {
                    continue;
                }

                String normalizedExistingName = existingService.getName().trim().toLowerCase();

                if (normalizedExistingName.equals(normalizedNewName)) {
                    throw new ResourceDoesNotSupportOperationException(
                            "Cannot update billable service. Another service with name '" +
                                    delegate.getName() + "' already exists (UUID: " +
                                    existingService.getUuid() + "). Please use a different name.");
                }
            }
        }

        return billableItemsService.save(delegate);
    }

    /**
     * Validates a billable service
     * 
     * @param service the service to validate
     * @throws ResourceDoesNotSupportOperationException if validation fails
     */
    private void validateBillableService(BillableService service) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(service.getName())) {
            errors.add("Billable service name is required");
        }

        if (service.getStockItem() == null && service.getServiceType() == null) {
            errors.add("Service type is required when no stock item is specified");
        }

        if (service.getConcept() == null) {
            errors.add("Concept is required");
        }

        if (!errors.isEmpty()) {
            throw new ResourceDoesNotSupportOperationException("Validation failed: " + StringUtils.join(errors, ", "));
        }
    }

    /**
     * Searches for billable services based on the provided parameters
     * 
     * @param context the request context containing search parameters
     * @return paged results
     * @throws ConversionException if search fails
     */
    @Override
    protected AlreadyPaged<BillableService> doSearch(RequestContext context) {
        try {
            String serviceTypeUuid = context.getParameter("serviceType");
            String serviceCategoryUuid = context.getParameter("serviceCategory");
            String serviceStatus = context.getParameter("status");
            String conceptUuid = context.getParameter("concept");
            String stockItemUuid = context.getParameter("stockItem");
            String name = context.getParameter("name");

            BillableServiceSearch search = new BillableServiceSearch();

            if (StringUtils.isNotBlank(name)) {
                search.getTemplate().setName(name);
            }

            if (StringUtils.isNotBlank(serviceTypeUuid)) {
                Concept serviceType = Context.getConceptService().getConceptByUuid(serviceTypeUuid);
                if (serviceType == null) {
                    throw new ObjectNotFoundException();
                }
                search.getTemplate().setServiceType(serviceType);
            }

            if (StringUtils.isNotBlank(serviceCategoryUuid)) {
                Concept serviceCategory = Context.getConceptService().getConceptByUuid(serviceCategoryUuid);
                if (serviceCategory == null) {
                    throw new ObjectNotFoundException();
                }
                search.getTemplate().setServiceCategory(serviceCategory);
            }

            if (StringUtils.isNotBlank(serviceStatus)) {
                BillableServiceStatus status = parseServiceStatus(serviceStatus);
                search.getTemplate().setServiceStatus(status);
            } else {
                // Default to enabled services if not specified
                search.getTemplate().setServiceStatus(BillableServiceStatus.ENABLED);
            }

            if (StringUtils.isNotBlank(stockItemUuid)) {
                StockItem stockItem = Context.getService(StockManagementService.class)
                        .getStockItemByUuid(stockItemUuid);
                if (stockItem == null) {
                    throw new ObjectNotFoundException();
                }
                search.getTemplate().setStockItem(stockItem);
            }

            Integer startIndex = context.getStartIndex();
            Integer limit = context.getLimit();

            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            List<BillableService> results = service.findServices(search);

            Integer totalCount = results.size();

            return new AlreadyPaged<BillableService>(context, results, false);
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ConversionException("Error searching billable services: " + e.getMessage(), e);
        }
    }

    /**
     * Parses service status from request parameter
     * 
     * @param statusParam the status parameter value
     * @return the BillableServiceStatus enum value
     */
    private BillableServiceStatus parseServiceStatus(String statusParam) {
        if ("disabled".equalsIgnoreCase(statusParam) ||
                "yes".equalsIgnoreCase(statusParam) ||
                "true".equalsIgnoreCase(statusParam) ||
                "1".equals(statusParam)) {
            return BillableServiceStatus.DISABLED;
        }
        return BillableServiceStatus.ENABLED;
    }

    /**
     * Gets representation description for billable service
     * 
     * @param rep the representation
     * @return the representation description
     */
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("uuid");
        description.addProperty("display");

        if (rep instanceof DefaultRepresentation) {
            description.addProperty("name");
            description.addProperty("shortName");
            description.addProperty("serviceType", Representation.REF);
            description.addProperty("serviceCategory", Representation.REF);
            description.addProperty("serviceStatus");
            description.addProperty("stockItem", Representation.REF);
            description.addProperty("servicePrices", Representation.REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof FullRepresentation) {
            description.addProperty("name");
            description.addProperty("shortName");
            description.addProperty("concept", Representation.REF);
            description.addProperty("serviceType", Representation.DEFAULT);
            description.addProperty("serviceCategory", Representation.DEFAULT);
            description.addProperty("serviceStatus");
            description.addProperty("stockItem", Representation.DEFAULT);
            description.addProperty("servicePrices", Representation.DEFAULT);
            description.addProperty("auditInfo");
            description.addSelfLink();
            return description;
        } else if (rep instanceof CustomRepresentation) {
            return null;
        }

        return description;
    }

    /**
     * Gets display string for billable service
     * 
     * @param instance the billable service instance
     * @return the display string
     */
    @PropertyGetter("display")
    public String getDisplayString(BillableService instance) {
        return instance.getName();
    }

    /**
     * Gets audit information for billable service
     * 
     * @param instance the billable service instance
     * @return SimpleObject containing audit information
     */
    public SimpleObject getAuditInfo(BillableService instance) {
        SimpleObject ret = new SimpleObject();
        ret.put("creator",
                ConversionUtil.getPropertyWithRepresentation(instance.getCreator(), "uuid", Representation.REF));
        ret.put("dateCreated",
                ConversionUtil.convertToRepresentation(instance.getDateCreated(), Representation.DEFAULT));

        if (instance.getChangedBy() != null) {
            ret.put("changedBy",
                    ConversionUtil.getPropertyWithRepresentation(instance.getChangedBy(), "uuid", Representation.REF));
            ret.put("dateChanged",
                    ConversionUtil.convertToRepresentation(instance.getDateChanged(), Representation.DEFAULT));
        }

        if (instance.isVoided()) {
            ret.put("voidedBy",
                    ConversionUtil.getPropertyWithRepresentation(instance.getVoidedBy(), "uuid", Representation.REF));
            ret.put("dateVoided",
                    ConversionUtil.convertToRepresentation(instance.getDateVoided(), Representation.DEFAULT));
            ret.put("voidReason", instance.getVoidReason());
        }

        return ret;
    }

    /**
     * Safely converts a collection to a new ArrayList, returning an empty list if
     * the input is null
     * 
     * @param collection the collection to convert
     * @return a new ArrayList containing the collection's elements, or an empty
     *         list if input is null
     */
    private <T> List<T> safeToList(Collection<T> collection) {
        if (collection == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(collection);
    }

    @PropertyGetter("servicePrices")
    public List<CashierItemPrice> getServicePrices(BillableService instance) {
        return safeToList(instance.getServicePrices());
    }

    /**
     * Sets service prices for billable service
     * 
     * @param instance   the billable service instance
     * @param itemPrices the list of service prices
     */
    @PropertySetter("servicePrices")
    public void setServicePrices(BillableService instance, List<CashierItemPrice> itemPrices) {
        if (instance.getServicePrices() == null) {
            instance.setServicePrices(new ArrayList<>(itemPrices.size()));
        }

        BaseRestDataResource.syncCollection(instance.getServicePrices(), itemPrices);

        for (CashierItemPrice itemPrice : instance.getServicePrices()) {
            itemPrice.setBillableService(instance);
        }
    }

    /**
     * Gets stock item reference for billable service
     * 
     * @param instance the billable service instance
     * @return map containing stock item reference
     */
    @PropertyGetter("stockItem")
    public Map<String, Object> getStockItem(BillableService instance) {
        StockItem stockItem = instance.getStockItem();
        if (stockItem == null) {
            return null;
        }

        Map<String, Object> stockItemRef = new HashMap<>();
        stockItemRef.put("uuid", stockItem.getUuid());
        stockItemRef.put("display", stockItem.getCommonName());

        // Add resource link
        Map<String, String> links = new HashMap<>();
        links.put("self", RestConstants.URI_PREFIX + "stockmanagement/stockitem/" + stockItem.getUuid());
        stockItemRef.put("links", links);

        return stockItemRef;
    }

    @PropertySetter("stockItem")
    public void setStockItem(BillableService instance, String stockItemUuid) {
        if (StringUtils.isBlank(stockItemUuid)) {
            instance.setStockItem(null);
            return;
        }
        
        StockItem stockItem = Context.getService(StockManagementService.class).getStockItemByUuid(stockItemUuid);
        if (stockItem == null) {
            throw new ObjectNotFoundException();
        }
        instance.setStockItem(stockItem);
    }

    /**
     * Gets creatable properties for billable service
     * 
     * @return the property description
     */
    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("name");
        description.addProperty("shortName");
        description.addProperty("concept");
        description.addProperty("serviceType");
        description.addProperty("serviceCategory");
        description.addProperty("servicePrices");
        description.addProperty("serviceStatus");
        description.addProperty("stockItem");
        return description;
    }

    /**
     * Gets updatable properties for billable service
     * 
     * @return the property description
     * @throws ResourceDoesNotSupportOperationException if operation not supported
     */
    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        return getCreatableProperties();
    }

    /**
     * Voids a billable service
     * 
     * @param delegate the billable service to void
     * @param reason   the reason for voiding
     * @param context  the request context
     * @throws ResponseException if voiding fails
     */
    @Override
    public void delete(BillableService delegate, String reason, RequestContext context) throws ResponseException {
        if (delegate == null) {
            throw new ObjectNotFoundException();
        }

        if (StringUtils.isBlank(reason)) {
            throw new ResourceDoesNotSupportOperationException("Reason for voiding is required");
        }

        getService().voidEntity(delegate, reason);
    }

    /**
     * Purges a billable service
     * 
     * @param delegate the billable service to purge
     * @param context  the request context
     * @throws ResponseException if purging fails
     */
    @Override
    public void purge(BillableService delegate, RequestContext context) throws ResponseException {
        if (delegate == null) {
            throw new ObjectNotFoundException();
        }

        getService().purge(delegate);
    }

    /**
     * Handles ObjectNotFoundException
     * 
     * @param ex the exception
     * @return error response
     */
    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SimpleObject handleObjectNotFoundException(ObjectNotFoundException ex) {
        SimpleObject error = new SimpleObject();
        error.put("error", "Entity Not Found");
        error.put("message", ex.getMessage());
        return error;
    }

    /**
     * Handles ResourceDoesNotSupportOperationException
     * 
     * @param ex the exception
     * @return error response
     */
    @ExceptionHandler(ResourceDoesNotSupportOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SimpleObject handleResourceDoesNotSupportOperationException(ResourceDoesNotSupportOperationException ex) {
        SimpleObject error = new SimpleObject();
        error.put("error", "Invalid Request");
        error.put("message", ex.getMessage());
        return error;
    }
}