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

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.search.BaseDataTemplateSearch;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.util.OpenmrsUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * A search template class for the {@link Bill} model.
 */
public class BillSearch extends BaseDataTemplateSearch<Bill> {
	private Date createdOnOrBefore;
	private Date createdOnOrAfter;
	public BillSearch() {
		this(new Bill(), false);
	}

	public BillSearch(Bill template) {
		this(template, false);
	}

	public BillSearch(Bill template, Boolean includeRetired) {
		super(template, includeRetired);
	}

	public BillSearch(Bill template, Date createdOnOrAfter, Date createdOnOrBefore, Boolean includeRetired) {
		super(template, includeRetired);
		this.createdOnOrAfter = createdOnOrAfter;
		this.createdOnOrBefore = createdOnOrBefore;
	}

	@Override
	public void updateCriteria(Criteria criteria) {
		super.updateCriteria(criteria);

		Bill bill = getTemplate();
		if (bill.getCashier() != null) {
			criteria.add(Restrictions.eq("cashier", bill.getCashier()));
		}
		if (bill.getCashPoint() != null) {
			criteria.add(Restrictions.eq("cashPoint", bill.getCashPoint()));
		}
		if (bill.getPatient() != null) {
			criteria.add(Restrictions.eq("patient", bill.getPatient()));
		}
		if (bill.getStatus() != null) {
			criteria.add(Restrictions.eq("status", bill.getStatus()));
		}

		if (getCreatedOnOrBefore() != null) {
			// set the date's time to the last millisecond of the date
			Calendar cal = Calendar.getInstance();
			cal.setTime(getCreatedOnOrBefore());
			criteria.add(Restrictions.le("dateCreated", OpenmrsUtil.getLastMomentOfDay(cal.getTime())));
		}
		if (getCreatedOnOrAfter() != null) {
			// set the date's time to 00:00:00.000
			Calendar cal = Calendar.getInstance();
			cal.setTime(getCreatedOnOrAfter());
			criteria.add(Restrictions.ge("dateCreated", OpenmrsUtil.firstSecondOfDay(cal.getTime())));
		}
	}

	public Date getCreatedOnOrBefore() {
		return createdOnOrBefore;
	}

	public void setCreatedOnOrBefore(Date createdOnOrBefore) {
		this.createdOnOrBefore = createdOnOrBefore;
	}

	public Date getCreatedOnOrAfter() {
		return createdOnOrAfter;
	}

	public void setCreatedOnOrAfter(Date createdOnOrAfter) {
		this.createdOnOrAfter = createdOnOrAfter;
	}
}