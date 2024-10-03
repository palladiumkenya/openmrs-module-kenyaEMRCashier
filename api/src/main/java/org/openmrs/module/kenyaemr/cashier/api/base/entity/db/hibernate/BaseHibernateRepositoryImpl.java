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
package org.openmrs.module.kenyaemr.cashier.api.base.entity.db.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides access to a data source through hibernate.
 */
public class BaseHibernateRepositoryImpl implements BaseHibernateRepository {
	private DbSessionFactory sessionFactory;

	public BaseHibernateRepositoryImpl(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public DbSessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Query createQuery(String query) {
		DbSession session = sessionFactory.getCurrentSession();
		return session.createQuery(query);
	}

	@Override
	public <E extends OpenmrsObject> Criteria createCriteria(Class<E> cls) {
		DbSession session = sessionFactory.getCurrentSession();
		return session.createCriteria(cls);
	}

	@Override
	public <E extends OpenmrsObject> E save(E entity) {
		DbSession session = sessionFactory.getCurrentSession();

		try {
			session.saveOrUpdate(entity);
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to add a " + entity.getClass().getSimpleName()
			        + " entity.", ex);
		}

		return entity;
	}

	@Override
	@Transactional
	public void saveAll(Collection<? extends OpenmrsObject> collection) {
		DbSession session = sessionFactory.getCurrentSession();
		try {

			if (collection != null && !collection.isEmpty()) {
				for (OpenmrsObject obj : collection) {
					session.saveOrUpdate(obj);
				}
			}
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to add a entity.", ex);
		}
	}

	@Override
	public <E extends OpenmrsObject> void delete(E entity) {
		DbSession session = sessionFactory.getCurrentSession();
		try {
			session.delete(entity);
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to delete a "
			        + entity.getClass().getSimpleName() + " entity.", ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T selectValue(Criteria criteria) {
		try {
			return (T)criteria.uniqueResult();
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to selecting a value.", ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T selectValue(Query query) {
		try {
			return (T)query.uniqueResult();
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to selecting a value.", ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends OpenmrsObject> E selectSingle(Class<E> cls, Serializable id) {
		DbSession session = sessionFactory.getCurrentSession();

		try {
			return (E)session.get(cls, id);
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to select a single " + cls.getSimpleName()
			        + " entity with ID" + " " + id.toString() + ".", ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		// Get the current Hibernate session from DbSessionFactory
        DbSession session = sessionFactory.getCurrentSession();
        
        // Ensure no caching is used by ignoring the cache
        session.setCacheMode(CacheMode.IGNORE);

		String sqlQuery = "SELECT cbp.bill_payment_id, cbp.uuid, cbp.bill_id, cbp.payment_mode_id, cbp.amount_tendered, cbp.amount FROM cashier_bill cb inner join cashier_bill_payment cbp on cbp.bill_id = cb.bill_id and cb.bill_id =:billId";

		// Execute the query and fetch the result
        List<Object[]> resultList = session.createSQLQuery(sqlQuery)
                                           .setParameter("billId", billId)
                                           .list();
		
		System.out.println("RMS Sync Cashier Module: Payments got SQL payments: " + resultList.size());
										   
		// Create a Set to hold the resulting Patient objects
        Set<Payment> payments = new HashSet<>();

        // Iterate through the results and map them to Patient objects
        for (Object[] row : resultList) {
            Payment payment = new Payment();
            payment.setId((Integer) row[0]);  // payment_id
			// System.out.println("RMS Sync Cashier Module: Payments got SQL payments: injecting ID" + (Integer) row[0]);
            payment.setUuid((String) row[1]); // payment uuid
			// System.out.println("RMS Sync Cashier Module: Payments got SQL payments: injecting UUID" + (String) row[1]);
			Bill newBill = new Bill();
			newBill.setId(billId);
			payment.setBill(newBill); // bill
			PaymentMode newPaymentMode = new PaymentMode();
			newPaymentMode.setId((Integer) row[3]);
			payment.setInstanceType(newPaymentMode); // payment mode
			payment.setAmountTendered((BigDecimal) row[4]); //Amount Tendered
			payment.setAmount((BigDecimal) row[5]); //Total Amount
            payments.add(payment);
        }

		return(payments);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends OpenmrsObject> E selectSingle(Class<E> cls, Criteria criteria) {
		E result = null;
		try {
			List<E> results = criteria.list();

			if (!results.isEmpty()) {
				result = results.get(0);
			}
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to select a single " + cls.getSimpleName()
			        + " entity.", ex);
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends OpenmrsObject> List<E> select(Class<E> cls) {
		DbSession session = sessionFactory.getCurrentSession();

		try {
			Criteria search = session.createCriteria(cls);

			return search.list();
		} catch (Exception ex) {
			throw new APIException("An exception occurred while attempting to get " + cls.getSimpleName() + " entities.", //
			        ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends OpenmrsObject> List<E> select(Class<E> cls, Criteria criteria) {
		// If the criteria is not defined just use the default select method
		if (criteria == null) {
			return select(cls);
		}

		List<E> results;

		try {
			results = criteria.list();
		} catch (Exception ex) {
			throw new APIException(
			        "An exception occurred while attempting to select " + cls.getSimpleName() + " entities.", ex);
		}

		return results;
	}
}
