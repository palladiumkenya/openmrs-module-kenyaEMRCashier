package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.restmapper.BillableServiceMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/api")
public class CashierRestController extends BaseRestController {

    private static final String GP_MAX_RESULTS_DEFAULT = "webservices.rest.maxResultsDefault";
    private static final int DEFAULT_MAX_RESULTS = 50;

    /**
     * Gets all billable services with pagination
     * 
     * @param startIndex the index to start from (default: 0)
     * @param limit the maximum number of results to return (default: from global property)
     * @return ResponseEntity with 200 status and paginated list of billable services
     * @throws ResponseStatusException if retrieval fails
     */
    @RequestMapping(method = RequestMethod.GET, path = "/billable-service")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(required = false) Integer limit) {
        try {
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            BillableServiceSearch search = new BillableServiceSearch();
            
            // Get default limit from global property
            int defaultLimit = getDefaultLimit();
            int actualLimit = limit != null ? limit : defaultLimit;
            
            // Get total count
            List<BillableService> allServices = service.findServices(search);
            int totalCount = allServices.size();
            
            // Apply pagination
            List<BillableService> paginatedServices = allServices.subList(
                Math.min(startIndex, totalCount),
                Math.min(startIndex + actualLimit, totalCount)
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", paginatedServices);
            response.put("totalCount", totalCount);
            response.put("startIndex", startIndex);
            response.put("limit", actualLimit);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve billable services", e);
        }
    }

    /**
     * Gets a specific billable service by UUID
     * 
     * @param uuid the uuid of the billable service to retrieve
     * @return ResponseEntity with 200 status and the billable service
     * @throws ResponseStatusException if service not found
     */
    @RequestMapping(method = RequestMethod.GET, path = "/billable-service/{uuid}")
    @ResponseBody
    public ResponseEntity<BillableService> getByUuid(@PathVariable String uuid) {
        try {
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            BillableService billableService = service.getByUuid(uuid);
            if (billableService == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Billable service not found");
            }
            return new ResponseEntity<>(billableService, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve billable service", e);
        }
    }

    /**
     * Searches for billable services based on query parameters with pagination
     * 
     * @param name optional name to search for
     * @param concept optional concept UUID to search for
     * @param serviceType optional service type UUID to search for
     * @param serviceCategory optional service category UUID to search for
     * @param startIndex the index to start from (default: 0)
     * @param limit the maximum number of results to return (default: from global property)
     * @return ResponseEntity with 200 status and paginated list of matching billable services
     * @throws ResponseStatusException if search fails
     */
    @RequestMapping(method = RequestMethod.GET, path = "/billable-service/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String concept,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String serviceCategory,
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(required = false) Integer limit) {
        try {
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            BillableServiceSearch search = new BillableServiceSearch();
            BillableService template = new BillableService();
            
            if (StringUtils.isNotBlank(name)) {
                template.setName(name);
            }
            if (StringUtils.isNotBlank(concept)) {
                template.setConcept(Context.getConceptService().getConceptByUuid(concept));
            }
            if (StringUtils.isNotBlank(serviceType)) {
                template.setServiceType(Context.getConceptService().getConceptByUuid(serviceType));
            }
            if (StringUtils.isNotBlank(serviceCategory)) {
                template.setServiceCategory(Context.getConceptService().getConceptByUuid(serviceCategory));
            }
            
            search.setTemplate(template);
            
            // Get default limit from global property
            int defaultLimit = getDefaultLimit();
            int actualLimit = limit != null ? limit : defaultLimit;
            
            // Get total count
            List<BillableService> allServices = service.findServices(search);
            int totalCount = allServices.size();
            
            // Apply pagination
            List<BillableService> paginatedServices = allServices.subList(
                Math.min(startIndex, totalCount),
                Math.min(startIndex + actualLimit, totalCount)
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", paginatedServices);
            response.put("totalCount", totalCount);
            response.put("startIndex", startIndex);
            response.put("limit", actualLimit);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search billable services", e);
        }
    }

    /**
     * Creates a new billable service
     * 
     * @param request the billable service data to create
     * @return ResponseEntity with 201 status and the created billable service
     * @throws ResponseStatusException if validation fails or service already exists
     */
    @RequestMapping(method = RequestMethod.POST, path = "/billable-service")
    @ResponseBody
    public ResponseEntity<BillableService> create(@RequestBody BillableServiceMapper request) {
        try {
            // Validate required fields
            if (StringUtils.isBlank(request.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
            }
            if (StringUtils.isBlank(request.getConcept())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept is required");
            }
            if (request.getServicePrices() == null || request.getServicePrices().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one service price is required");
            }

            // Check for duplicate service with same concept
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            BillableServiceSearch search = new BillableServiceSearch();
            BillableService template = new BillableService();
            template.setConcept(Context.getConceptService().getConceptByUuid(request.getConcept()));
            search.setTemplate(template);
            
            List<BillableService> existingServices = service.findServices(search);
            if (!existingServices.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "A billable service with this concept already exists. Please use the update endpoint to modify the existing service."
                );
            }

            // Create new service
            BillableService billableService = request.billableServiceMapper(request);
            BillableService savedService = service.save(billableService);
            return new ResponseEntity<>(savedService, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create billable service", e);
        }
    }

    /**
     * Updates an existing billable service
     * 
     * @param uuid the uuid of the billable service to update
     * @param request the updated billable service data
     * @return ResponseEntity with 200 status and the updated billable service
     * @throws ResponseStatusException if billable service not found or update fails
     */
    @RequestMapping(method = RequestMethod.PUT, path = "/billable-service/{uuid}")
    @ResponseBody
    public ResponseEntity<BillableService> update(@PathVariable String uuid, @RequestBody BillableServiceMapper request) {
        try {
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            BillableService existingService = service.getByUuid(uuid);
            if (existingService == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Billable service not found");
            }
            
            BillableService updatedService = request.billableServiceUpdateMapper(request);
            BillableService savedService = service.save(updatedService);
            return new ResponseEntity<>(savedService, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update billable service", e);
        }
    }

    /**
     * Deletes a billable service
     * 
     * @param uuid the uuid of the billable service to delete
     * @return ResponseEntity with 204 status on successful deletion
     * @throws ResponseStatusException if billable service not found
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "/billable-service/{uuid}")
    @ResponseBody
    public ResponseEntity<Object> delete(@PathVariable String uuid) {
        IBillableItemsService service = Context.getService(IBillableItemsService.class);
        try {
            BillableService billableService = service.getByUuid(uuid);
            if (billableService == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Billable service not found");
            }
            service.voidEntity(billableService, "Deleted via REST API");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete billable service", e);
        }
    }

    /**
     * Gets the default limit from global property
     * 
     * @return the default limit value
     */
    private int getDefaultLimit() {
        String maxResultsDefault = Context.getAdministrationService().getGlobalProperty(GP_MAX_RESULTS_DEFAULT);
        try {
            return Integer.parseInt(maxResultsDefault);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_RESULTS;
        }
    }
}
