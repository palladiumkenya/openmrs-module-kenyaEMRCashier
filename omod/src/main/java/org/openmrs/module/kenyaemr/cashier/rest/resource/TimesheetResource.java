/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.ICashPointService;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.api.ITimesheetService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.Timesheet;
import org.openmrs.module.kenyaemr.cashier.api.search.TimesheetSearch;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.util.LocaleUtility;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * REST resource representing a {@link Timesheet}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/timesheet", supportedClass = Timesheet.class,
        supportedOpenmrsVersions = { "2.0 - 2.*" })
public class TimesheetResource extends BaseRestDataResource<Timesheet> {
	@Override
	public Timesheet newDelegate() {
		return new Timesheet();
	}

	@Override
	public Class<? extends IEntityDataService<Timesheet>> getServiceClass() {
		return ITimesheetService.class;
	}

	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		description.addProperty("cashier", Representation.REF);
		description.addProperty("cashPoint", Representation.REF);
		description.addProperty("clockIn");
		description.addProperty("clockOut");
		if (rep instanceof RefRepresentation) {
			description.addProperty("id");
			description.addProperty("uuid");
		}

		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = super.getCreatableProperties();
		description.addProperty("cashier");
		description.addProperty("cashpoint");
		return description;
	}

	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		return getCreatableProperties();
	}
	public String getDisplayString(Timesheet instance) {
		DateFormat dateFormat =
		        DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, LocaleUtility.getDefaultLocale());
		return dateFormat.format(instance.getClockIn()) + " to "
		        + (instance.getClockOut() != null ? dateFormat.format(instance.getClockOut())
		                : " open");
	}

	@Override
	protected AlreadyPaged<Timesheet> doSearch(RequestContext context) {
		String cashierUuid = context.getRequest().getParameter("cashier");
		String cashPointUuid = context.getRequest().getParameter("cashPoint");
		String startDatetimeStr = context.getRequest().getParameter("startDatetime");
		String endDatetimeStr = context.getRequest().getParameter("endDatetime");
		String getProviderOpenTimesheet = context.getRequest().getParameter("getProviderOpenTimesheet");
		Provider provider = Strings.isNotEmpty(cashierUuid) ? Context.getProviderService().getProviderByUuid(cashierUuid) : null;

		ITimesheetService service = Context.getService(ITimesheetService.class);

		if (Strings.isNotEmpty(getProviderOpenTimesheet) && getProviderOpenTimesheet.equalsIgnoreCase("true")) {
			Timesheet result = service.getCurrentTimesheet(provider);
			return new AlreadyPaged<>(context, Arrays.asList(result), false);
		}

		CashPoint cashPoint = Strings.isNotEmpty(cashPointUuid) ? Context.getService(ICashPointService.class).getByUuid(cashPointUuid) : null;
		Date startDatetime = StringUtils.isNotBlank(startDatetimeStr) ?
				(Date) ConversionUtil.convert(startDatetimeStr, Date.class) : null;
		Date endDatetime = StringUtils.isNotBlank(endDatetimeStr) ?
				(Date) ConversionUtil.convert(endDatetimeStr, Date.class) : null;

		Timesheet searchTemplate = new Timesheet();
		searchTemplate.setCashier(provider);
		searchTemplate.setCashPoint(cashPoint);

		List<Timesheet> result = service.getTimesheets(new TimesheetSearch(searchTemplate, false, startDatetime, endDatetime));
		return new AlreadyPaged<>(context, result, false);
	}
}
