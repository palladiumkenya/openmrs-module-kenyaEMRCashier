/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentAttributeService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data service implementation class for {@link PaymentAttribute}s.
 */
@Transactional
public class PaymentAttributeServiceImpl extends BaseEntityDataServiceImpl<PaymentAttribute> implements IPaymentAttributeService {

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return new IEntityAuthorizationPrivileges() {
			@Override
			public String getVoidPrivilege() {
				return null;
			}

			@Override
			public String getSavePrivilege() {
				return null;
			}

			@Override
			public String getPurgePrivilege() {
				return null;
			}

			@Override
			public String getGetPrivilege() {
				return null;
			}
		};
	}

	@Override
	protected void validate(PaymentAttribute entity) {
		validateUniqueValue(entity);
	}

	@Override
	public void validateUniqueValue(PaymentAttribute paymentAttribute) {
		if (paymentAttribute == null || paymentAttribute.getOwner() == null || 
			paymentAttribute.getAttributeType() == null || 
			StringUtils.isBlank(paymentAttribute.getValue())) {
			return; // Skip validation for incomplete entities
		}

		// Search for existing attributes with the same payment, attribute type, and value
		List<PaymentAttribute> existingAttributes = executeCriteria(PaymentAttribute.class, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.eq("owner.id", paymentAttribute.getOwner().getId()));
				criteria.add(Restrictions.eq("attributeType.id", paymentAttribute.getAttributeType().getId()));
				criteria.add(Restrictions.eq("value", paymentAttribute.getValue()));
				
				// Exclude the current entity if it's being updated
				if (paymentAttribute.getId() != null) {
					criteria.add(Restrictions.ne("id", paymentAttribute.getId()));
				}
			}
		});
		
		if (!existingAttributes.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("A payment attribute with value '%s' already exists for payment %d and attribute type '%s'",
					paymentAttribute.getValue(),
					paymentAttribute.getOwner().getId(),
					paymentAttribute.getAttributeType().getName()));
		}
	}
} 