package org.openmrs.module.kenyaemr.cashier.rest.search;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DepositSearchHandler implements SearchHandler {
    private final SearchConfig searchConfig = new SearchConfig("default", RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/deposit",
            Arrays.asList("2.0.*", "2.1.*", "2.2.*", "2.3.*", "2.4.*"),
            Arrays.asList(
                    new SearchQuery.Builder("Find deposits by patient")
                            .withRequiredParameters("patient")
                            .build(),
                    new SearchQuery.Builder("Find deposit by reference number")
                            .withRequiredParameters("referenceNumber")
                            .build()
            ));

    @Override
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }

    @Override
    public PageableResult search(RequestContext context) throws ResponseException {
        String patientUuid = context.getParameter("patient");
        String referenceNumber = context.getParameter("referenceNumber");
        IDepositService service = Context.getService(IDepositService.class);

        if (StringUtils.isNotEmpty(patientUuid)) {
            Patient patient = Context.getService(PatientService.class).getPatientByUuid(patientUuid);
            if (patient != null) {
                List<Deposit> deposits = service.getDepositsByPatient(patient, null);
                return new AlreadyPaged<>(context, deposits, false);
            }
        } else if (StringUtils.isNotEmpty(referenceNumber)) {
            Deposit deposit = service.getDepositByReferenceNumber(referenceNumber);
            if (deposit != null) {
                return new AlreadyPaged<>(context, Arrays.asList(deposit), false);
            }
        }

        return new AlreadyPaged<>(context, Arrays.asList(), false);
    }
} 