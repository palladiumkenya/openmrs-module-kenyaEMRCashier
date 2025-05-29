package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;

import java.math.BigDecimal;
import java.util.ArrayList;

@SubResource(parent = DepositResource.class, path = "transaction", supportedClass = DepositTransaction.class,
        supportedOpenmrsVersions = { "2.0 - 2.*" })
public class DepositTransactionResource extends DelegatingSubResource<DepositTransaction, Deposit, DepositResource> {
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("uuid");

        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("billLineItem", Representation.REF);
            description.addProperty("amount");
            description.addProperty("transactionType");
            description.addProperty("reason");
            description.addProperty("dateCreated");
            description.addProperty("voided");
        }

        return description;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("billLineItem");
        description.addProperty("amount");
        description.addProperty("transactionType");
        description.addProperty("reason");

        return description;
    }

    @PropertySetter("amount")
    public void setAmount(DepositTransaction instance, Object amount) {
        if (amount instanceof Integer) {
            instance.setAmount(BigDecimal.valueOf((Integer) amount));
        } else if (amount instanceof Double) {
            instance.setAmount(BigDecimal.valueOf((Double) amount));
        } else if (amount instanceof BigDecimal) {
            instance.setAmount((BigDecimal) amount);
        } else if (amount instanceof String) {
            instance.setAmount(new BigDecimal((String) amount));
        }
    }

    @PropertySetter("billLineItem")
    public void setBillLineItem(DepositTransaction instance, Object billLineItem) {
        if (billLineItem instanceof String) {
            String uuid = (String) billLineItem;
            BillLineItemService service = Context.getService(BillLineItemService.class);
            instance.setBillLineItem(service.getByUuid(uuid));
        }
    }

    @Override
    public DepositTransaction save(DepositTransaction delegate) {
        IDepositService service = Context.getService(IDepositService.class);
        Deposit deposit = delegate.getDeposit();
        service.addTransaction(deposit, delegate);

        return delegate;
    }

    @Override
    protected void delete(DepositTransaction delegate, String reason, RequestContext context) {
        IDepositService service = Context.getService(IDepositService.class);
        service.voidTransaction(delegate, reason);
    }

    @Override
    public void purge(DepositTransaction delegate, RequestContext context) {
        throw new UnsupportedOperationException("Purging deposit transactions is not supported.");
    }

    @Override
    public PageableResult doGetAll(Deposit parent, RequestContext context) {
        return new AlreadyPaged<>(context, new ArrayList<>(parent.getTransactions()), false);
    }

    @Override
    public DepositTransaction getByUniqueId(String uniqueId) {
        for (Deposit deposit : Context.getService(IDepositService.class).getAll(false)) {
            for (DepositTransaction transaction : deposit.getTransactions()) {
                if (transaction.getUuid().equals(uniqueId)) {
                    return transaction;
                }
            }
        }
        return null;
    }

    @Override
    public Deposit getParent(DepositTransaction instance) {
        return instance.getDeposit();
    }

    @Override
    public void setParent(DepositTransaction instance, Deposit parent) {
        instance.setDeposit(parent);
    }

    @Override
    public DepositTransaction newDelegate() {
        return new DepositTransaction();
    }
} 