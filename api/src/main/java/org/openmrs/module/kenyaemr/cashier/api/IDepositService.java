package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;

import java.util.List;

/**
 * Interface that defines the deposit service.
 */
public interface IDepositService extends IEntityDataService<Deposit> {

    /**
     * Saves the deposit to the database.
     * 
     * @param deposit The deposit to save.
     * @return The saved deposit.
     * @should save the deposit
     */
    @Authorized(PrivilegeConstants.MANAGE_DEPOSITS)
    Deposit save(Deposit deposit);

    /**
     * Gets a deposit by its id.
     * 
     * @param depositId The deposit id.
     * @return The deposit or null if not found.
     * @should return the deposit with the specified id
     * @should return null if no deposit is found
     */
    @Authorized(PrivilegeConstants.VIEW_DEPOSITS)
    Deposit getById(Integer depositId);

    /**
     * Gets a deposit by its UUID.
     * 
     * @param uuid The deposit UUID.
     * @return The deposit or null if not found.
     * @should return the deposit with the specified uuid
     * @should return null if no deposit is found
     */
    @Authorized(PrivilegeConstants.VIEW_DEPOSITS)
    Deposit getByUuid(String uuid);

    /**
     * Gets all deposits for a patient.
     * 
     * @param patient    The patient.
     * @param pagingInfo The paging information.
     * @return A list of deposits.
     * @should return all deposits for the specified patient
     * @should return an empty list if the patient has no deposits
     */
    @Authorized(PrivilegeConstants.VIEW_DEPOSITS)
    List<Deposit> getDepositsByPatient(Patient patient, PagingInfo pagingInfo);

    /**
     * Gets all deposits for a patient by patient uuid.
     * 
     * @param patientUuid The patient uuid.
     * @param pagingInfo  The paging information.
     * @return A list of deposits.
     * @should return all deposits for the specified patient
     * @should return an empty list if the patient has no deposits
     */
    @Authorized(PrivilegeConstants.VIEW_DEPOSITS)
    List<Deposit> getDepositByPatientUuid(String patientUuid, PagingInfo pagingInfo);

    /**
     * Gets a deposit by its reference number.
     * 
     * @param referenceNumber The reference number.
     * @return The deposit or null if not found.
     * @should return the deposit with the specified reference number
     * @should return null if no deposit is found
     */
    @Authorized(PrivilegeConstants.VIEW_DEPOSITS)
    Deposit getDepositByReferenceNumber(String referenceNumber);

    /**
     * Adds a transaction to a deposit.
     * 
     * @param deposit     The deposit.
     * @param transaction The transaction to add.
     * @return The saved deposit.
     * @should add the transaction to the deposit
     * @should save the deposit
     */
    @Authorized(PrivilegeConstants.MANAGE_DEPOSITS)
    Deposit addTransaction(Deposit deposit, DepositTransaction transaction);

    /**
     * Voids a deposit transaction.
     * 
     * @param transaction The transaction to void.
     * @param reason      The reason for voiding.
     * @return The saved deposit.
     * @should void the transaction
     * @should save the deposit
     */
    @Authorized(PrivilegeConstants.MANAGE_DEPOSITS)
    Deposit voidTransaction(DepositTransaction transaction, String reason);

    /**
     * Purges a deposit from the database.
     * 
     * @param deposit The deposit to purge.
     * @should purge the deposit
     */
    @Authorized(PrivilegeConstants.PURGE_DEPOSITS)
    void purge(Deposit deposit);
}