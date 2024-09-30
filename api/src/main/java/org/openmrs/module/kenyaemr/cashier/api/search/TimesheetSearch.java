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
package org.openmrs.module.kenyaemr.cashier.api.search;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.search.BaseDataTemplateSearch;
import org.openmrs.module.kenyaemr.cashier.api.model.Timesheet;
import org.springframework.stereotype.Component;

/**
 * Search handler for {@link Timesheet}s.
 */
@Component
public class TimesheetSearch extends BaseDataTemplateSearch<Timesheet> {

	private Date startDatetime;
	private Date endDatetime;

	public TimesheetSearch() {
		this(new Timesheet(), false);
	}
	public TimesheetSearch(Timesheet template) {
		this(template, false);
	}

	public TimesheetSearch(Timesheet template, Boolean includeVoided) {
		super(template, includeVoided);
	}

	public TimesheetSearch(Timesheet template, Boolean includeVoided, Date startDatetime, Date endDatetime) {
		super(template, includeVoided);
		this.startDatetime = startDatetime;
		this.endDatetime = endDatetime;
	}

	@Override
	public void updateCriteria(Criteria criteria) {
		super.updateCriteria(criteria);

		Timesheet timesheet = getTemplate();
		if (timesheet.getCashier() != null) {
			criteria.add(Restrictions.eq("cashier", timesheet.getCashier()));
		}
		if (timesheet.getCashPoint() != null) {
			criteria.add(Restrictions.eq("cashPoint", timesheet.getCashPoint()));
		}


		if (getStartDatetime() != null) {
			// set the date's time to the last millisecond of the date
			Calendar cal = Calendar.getInstance();
			cal.setTime(getStartDatetime());
			criteria.add(Restrictions.ge("clockIn", cal.getTime()));
		}
		if (getEndDatetime() != null) {
			// set the date's time to 00:00:00.000
			Calendar cal = Calendar.getInstance();
			cal.setTime(getEndDatetime());
			criteria.add(Restrictions.le("clockOut", cal.getTime()));
		}
	}

	public Date getStartDatetime() {
		return startDatetime;
	}

	public void setStartDatetime(Date startDatetime) {
		this.startDatetime = startDatetime;
	}

	public Date getEndDatetime() {
		return endDatetime;
	}

	public void setEndDatetime(Date endDatetime) {
		this.endDatetime = endDatetime;
	}
}
