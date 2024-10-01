package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IInsuranceBenefitsPackageService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.*;
import org.openmrs.module.kenyaemr.cashier.api.search.InsuranceBenefitsPackageServiceSearch;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;


@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/insuranceBenefitsPackage", supportedClass = InsuranceBenefitsPackageService.class,
        supportedOpenmrsVersions = {"2.0 - 2.*"})
public class InsuranceBenefitsPackageServiceResource extends BaseRestDataResource<InsuranceBenefitsPackageService> {

    @Override
    public InsuranceBenefitsPackageService newDelegate() {
        return new InsuranceBenefitsPackageService();
    }

    @Override
    public Class<? extends IEntityDataService<InsuranceBenefitsPackageService>> getServiceClass() {
        return IInsuranceBenefitsPackageService.class;
    }

    @Override
    public InsuranceBenefitsPackageService getByUniqueId(String uuid) {
        return getService().getByUuid(uuid);
    }

    @Override
    public InsuranceBenefitsPackageService save(InsuranceBenefitsPackageService delegate) {
        return super.save(delegate);
    }

    @Override
    protected AlreadyPaged<InsuranceBenefitsPackageService> doSearch(RequestContext context) {
        
        String packageCode = context.getParameter("packageCode");
        String packageName = context.getParameter("packageName");
        String subCategory = context.getParameter("subCategory");
        String shaCode = context.getParameter("shaCode");
        
        InsuranceBenefitsPackageService searchTemplate = new InsuranceBenefitsPackageService();
        searchTemplate.setPackageCode(packageCode);
        searchTemplate.setPackageName(packageName);
        searchTemplate.setShaCode(shaCode);
        searchTemplate.setSubCategory(subCategory);

        IInsuranceBenefitsPackageService service = Context.getService(IInsuranceBenefitsPackageService.class);
        return new AlreadyPaged<>(context, service.findServices(new InsuranceBenefitsPackageServiceSearch(searchTemplate, false)), false);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        
        description.addProperty("uuid");
        description.addProperty("interventionName");
        description.addProperty("shaCode");
        description.addProperty("description");
        description.addProperty("interventionType");
        description.addProperty("requiresPreAuth");
        description.addProperty("benefitDistribution");
        description.addProperty("tariff");
        description.addProperty("lowerAgeLimit");
        description.addProperty("upperAgeLimit");
        description.addProperty("needsReferral");
        description.addProperty("packageName");
        description.addProperty("packageCode");
        description.addProperty("insurer");
        description.addProperty("gender");
        description.addProperty("accessPoint");
        description.addProperty("subCategory");

        if (rep instanceof FullRepresentation) {
            description.addProperty("auditInfo");
        }

        return description;
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