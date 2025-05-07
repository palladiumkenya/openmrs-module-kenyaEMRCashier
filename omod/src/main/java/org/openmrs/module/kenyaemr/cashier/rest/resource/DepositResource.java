package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.Patient;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/deposit",
        supportedClass = Deposit.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class DepositResource extends BaseRestDataResource<Deposit> {
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = super.getRepresentationDescription(rep);
        if (rep instanceof RefRepresentation) {
            description.addProperty("uuid");
            description.addProperty("display");
        } else if (rep instanceof DefaultRepresentation) {
            description.addProperty("patient", Representation.REF);
            description.addProperty("amount");
            description.addProperty("depositType");
            description.addProperty("status");
            description.addProperty("referenceNumber");
            description.addProperty("description");
            description.addProperty("transactions", Representation.DEFAULT);
            description.addProperty("dateCreated");
            description.addProperty("voided");
        } else if (rep instanceof FullRepresentation) {
            description.addProperty("patient", Representation.FULL);
            description.addProperty("amount");
            description.addProperty("depositType");
            description.addProperty("status");
            description.addProperty("referenceNumber");
            description.addProperty("description");
            description.addProperty("transactions", Representation.FULL);
            description.addProperty("dateCreated");
            description.addProperty("voided");
            description.addProperty("voidReason");
            description.addProperty("auditInfo");
            description.addProperty("availableBalance");
        } else if (rep instanceof CustomRepresentation) {
            // Return null for custom representation to let the user specify which properties they want
            description = null;
        }
        return description;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        return getRepresentationDescription(new DefaultRepresentation());
    }

    @Override
    public Deposit newDelegate() {
        return new Deposit();
    }

    @Override
    public Class<? extends IEntityDataService<Deposit>> getServiceClass() {
        return IDepositService.class;
    }

    @Override
    protected NeedsPaging<Deposit> doGetAll(RequestContext context) {
        IDepositService service = Context.getService(IDepositService.class);
        List<Deposit> deposits = service.getAll(context.getIncludeAll());
        return new NeedsPaging<>(deposits, context);
    }

    @Override
    protected void delete(Deposit delegate, String reason, RequestContext context) {
        IDepositService service = Context.getService(IDepositService.class);
        delegate.setVoided(true);
        delegate.setVoidReason(reason);
        service.save(delegate);
    }

    @Override
    public void purge(Deposit delegate, RequestContext context) {
        IDepositService service = Context.getService(IDepositService.class);
        service.purge(delegate);
    }

    @PropertySetter("amount")
    public void setAmount(Deposit instance, Object amount) {
        if (amount instanceof Integer) {
            int rawAmount = (Integer) amount;
            instance.setAmount(BigDecimal.valueOf(rawAmount));
        } else if (amount instanceof Double) {
            instance.setAmount(BigDecimal.valueOf((Double) amount));
        } else if (amount instanceof String) {
            instance.setAmount(new BigDecimal((String) amount));
        } else {
            throw new IllegalArgumentException("Unsupported amount type: " + amount.getClass().getName());
        }
    }

    @Override
    protected AlreadyPaged<Deposit> doSearch(RequestContext context) {
        String patientUuid = context.getParameter("patient");
        String includeAllStr = context.getParameter("includeAll");
        boolean includeAll = includeAllStr != null ? Boolean.parseBoolean(includeAllStr) : false;

        IDepositService service = Context.getService(IDepositService.class);
        List<Deposit> deposits;

        if (patientUuid != null) {
            Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
            if (patient == null) {
                return new AlreadyPaged<>(context, new ArrayList<>(), false);
            }
            deposits = service.getDepositsByPatient(patient, null);
        } else {
            deposits = service.getAll(includeAll);
        }

        return new AlreadyPaged<>(context, deposits, false);
    }
} 