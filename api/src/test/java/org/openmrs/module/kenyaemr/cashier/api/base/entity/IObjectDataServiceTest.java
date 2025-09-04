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
package org.openmrs.module.kenyaemr.cashier.api.base.entity;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action2;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, 
                     loader = GenericXmlContextLoader.class)
public abstract class IObjectDataServiceTest<S extends IObjectDataService<E>, E extends OpenmrsObject> {
	protected S service;

	/**
	 * Tests that the specified object are not null and that the {@link OpenmrsObject} properties are equal.
	 * @param expected The expected object properties
	 * @param actual The actual object properties
	 */
	public static void assertOpenmrsObject(OpenmrsObject expected, OpenmrsObject actual) {
		Assert.assertNotNull(expected);
		Assert.assertNotNull(actual);

		Assert.assertEquals(expected.getId(), actual.getId());
		Assert.assertEquals(expected.getUuid(), actual.getUuid());
	}

	public static <T> void assertCollection(Collection<T> expected, Collection<T> actual, Action2<T, T> test) {
		if (expected == null) {
			Assert.assertNull(actual);
		} else {
			Assert.assertEquals(expected.size(), actual.size());

			T[] expectedArray = (T[])new Object[expected.size()];
			expected.toArray(expectedArray);
			T[] actualArray = (T[])new Object[actual.size()];
			actual.toArray(actualArray);

			for (int i = 0; i < expected.size(); i++) {
				test.apply(expectedArray[i], actualArray[i]);
			}
		}
	}

	public abstract E createEntity(boolean valid);

	protected abstract int getTestEntityCount();

	protected abstract void updateEntityFields(E entity);

	protected void assertEntity(E expected, E actual) {
		assertOpenmrsObject(expected, actual);
	}

	protected S createService() {
		return Context.getService(getServiceClass());
	}

	@Before
	public void before() throws Exception {
		service = createService();
	}

	/**
	 * @verifies throw NullPointerException if the object is null
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#save(OpenmrsObject)
	 */
	@Test(expected = NullPointerException.class)
	public void save_shouldThrowNullPointerExceptionIfTheObjectIsNull() throws Exception {
		service.save(null);
	}

	/**
	 * @verifies validate the object before saving
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#save(OpenmrsObject)
	 */
	@Test(expected = APIException.class)
	public void save_shouldValidateTheObjectBeforeSaving() throws Exception {
		E entity = createEntity(false);

		service.save(entity);
	}

	/**
	 * @verifies return the saved object
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#save(OpenmrsObject)
	 */
	@Test
	public void save_shouldReturnSavedObject() throws Exception {
		E entity = createEntity(true);

		E result = service.save(entity);

		assertEntity(entity, result);
	}

	/**
	 * @verifies update the object successfully
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#save(OpenmrsObject)
	 */
	@Test
	public void save_shouldUpdateTheObjectSuccessfully() throws Exception {
		E entity = createEntity(true);
		entity = service.save(entity);

		updateEntityFields(entity);

		E result = service.save(entity);

		assertEntity(entity, result);
	}

	/**
	 * @verifies create the object successfully
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#save(OpenmrsObject)
	 */
	@Test
	public void save_shouldCreateTheObjectSuccessfully() throws Exception {
		E entity = createEntity(true);

		E result = service.save(entity);

		assertEntity(entity, result);
	}

	/**
	 * @verifies throw NullPointerException if the object is null
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#purge(OpenmrsObject)
	 */
	@Test(expected = NullPointerException.class)
	public void purge_shouldThrowNullPointerExceptionIfTheObjectIsNull() throws Exception {
		service.purge(null);
	}

	/**
	 * @verifies delete the specified object
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#purge(OpenmrsObject)
	 */
	@Test
	public void purge_shouldDeleteTheSpecifiedObject() throws Exception {
		E entity = createEntity(true);
		entity = service.save(entity);

		service.purge(entity);

		E result = service.getById(entity.getId());
		Assert.assertNull(result);
	}

	/**
	 * @verifies return all object records
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll()
	 */
	@Test
	public void getAll_shouldReturnAllObjectRecords() throws Exception {
		List<E> results = service.getAll();

		Assert.assertEquals(getTestEntityCount(), results.size());
	}

	/**
	 * @verifies return an empty list if there are no objects
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll()
	 */
	@Test
	public void getAll_shouldReturnAnEmptyListIfThereAreNoObjects() throws Exception {
		// This test assumes that there are no entities in the database
		// You may need to clear the database or use a different approach
		List<E> results = service.getAll();

		Assert.assertNotNull(results);
		Assert.assertTrue(results.isEmpty());
	}

	/**
	 * @verifies return the object with the specified id
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getById(Integer)
	 */
	@Test
	public void getById_shouldReturnTheObjectWithTheSpecifiedId() throws Exception {
		E entity = createEntity(true);
		entity = service.save(entity);

		E result = service.getById(entity.getId());

		assertEntity(entity, result);
	}

	/**
	 * @verifies return null if no object can be found
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getById(Integer)
	 */
	@Test
	public void getById_shouldReturnNullIfNoObjectCanBeFound() throws Exception {
		E result = service.getById(Integer.MAX_VALUE);

		Assert.assertNull(result);
	}

	/**
	 * @verifies find the object with the specified uuid
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getByUuid(String)
	 */
	@Test
	public void getByUuid_shouldFindTheObjectWithTheSpecifiedUuid() throws Exception {
		E entity = createEntity(true);
		entity = service.save(entity);

		E result = service.getByUuid(entity.getUuid());

		assertEntity(entity, result);
	}

	/**
	 * @verifies return null if no object is found
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getByUuid(String)
	 */
	@Test
	public void getByUuid_shouldReturnNullIfNoObjectIsFound() throws Exception {
		E result = service.getByUuid("invalid-uuid");

		Assert.assertNull(result);
	}

	/**
	 * @verifies throw IllegalArgumentException if uuid is null
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getByUuid(String)
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getByUuid_shouldThrowIllegalArgumentExceptionIfUuidIsNull() throws Exception {
		service.getByUuid(null);
	}

	/**
	 * @verifies throw IllegalArgumentException if uuid is empty
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getByUuid(String)
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getByUuid_shouldThrowIllegalArgumentExceptionIfUuidIsEmpty() throws Exception {
		service.getByUuid("");
	}

	/**
	 * @verifies return all object records if paging is null
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll(PagingInfo)
	 */
	@Test
	public void getAll_shouldReturnAllObjectRecordsIfPagingIsNull() throws Exception {
		List<E> results = service.getAll(null);

		Assert.assertEquals(getTestEntityCount(), results.size());
	}

	/**
	 * @verifies return all object records if paging page or size is less than one
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll(PagingInfo)
	 */
	@Test
	public void getAll_shouldReturnAllObjectRecordsIfPagingPageOrSizeIsLessThanOne() throws Exception {
		PagingInfo paging = new PagingInfo(0, 0);

		List<E> results = service.getAll(paging);

		Assert.assertEquals(getTestEntityCount(), results.size());
	}

	/**
	 * @verifies set the paging total records to the total number of object records
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll(PagingInfo)
	 */
	@Test
	public void getAll_shouldSetThePagingTotalRecordsToTheTotalNumberOfObjectRecords() throws Exception {
		PagingInfo paging = new PagingInfo(1, 1);

		service.getAll(paging);

		Assert.assertEquals(getTestEntityCount(), paging.getTotalRecordCount().intValue());
	}

	/**
	 * @verifies not get the total paging record count if it is more than zero
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll(PagingInfo)
	 */
	@Test
	public void getAll_shouldNotGetTheTotalPagingRecordCountIfItIsMoreThanZero() throws Exception {
		PagingInfo paging = new PagingInfo(1, 1);
		paging.setTotalRecordCount(5L);

		service.getAll(paging);

		Assert.assertEquals(5, paging.getTotalRecordCount().intValue());
	}

	/**
	 * @verifies return paged object records if paging is specified
	 * @see org.openmrs.module.openhmis.commons.api.entity.IObjectDataService#getAll(PagingInfo)
	 */
	@Test
	public void getAll_shouldReturnPagedObjectRecordsIfPagingIsSpecified() throws Exception {
		PagingInfo paging = new PagingInfo(1, 1);

		List<E> results = service.getAll(paging);

		Assert.assertEquals(1, results.size());
	}

	@SuppressWarnings("unchecked")
	protected Class<S> getServiceClass() {
		ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();
		return (Class<S>)parameterizedType.getActualTypeArguments()[0];
	}
}
