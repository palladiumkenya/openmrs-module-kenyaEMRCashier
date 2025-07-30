package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;

import java.util.List;

/**
 * Interface that defines the deposit transaction service.
 */
public interface IDepositTransactionService extends IEntityDataService<DepositTransaction> {
    /**
     * Gets all deposit transactions.
     * @param includeVoided Whether to include voided transactions.
     * @return A list of deposit transactions.
     * @should return all deposit transactions
     * @should return an empty list if no transactions are found
     */
    List<DepositTransaction> getAll(Boolean includeVoided);

    /**
     * Saves the deposit transaction to the database.
     * @param depositTransaction The deposit transaction to save.
     * @return The saved deposit transaction.
     * @should save the deposit transaction
     */
    @Authorized(PrivilegeConstants.MANAGE_DEPOSITS)
    DepositTransaction save(DepositTransaction depositTransaction);

    /**
     * Gets a deposit transaction by its id.
     * @param depositTransactionId The deposit transaction id.
     * @return The deposit transaction or null if not found.
     * @should return the deposit transaction with the specified id
     * @should return null if no deposit transaction is found
     */
    DepositTransaction getById(Integer depositTransactionId);

    /**
     * Gets a deposit transaction by its UUID.
     * @param uuid The deposit transaction UUID.
     * @return The deposit transaction or null if not found.
     * @should return the deposit transaction with the specified uuid
     * @should return null if no deposit transaction is found
     */
    DepositTransaction getByUuid(String uuid);

    /**
     * Purges a deposit transaction from the database.
     * @param depositTransaction The deposit transaction to purge.
     * @should purge the deposit transaction
     */
    @Authorized(PrivilegeConstants.PURGE_DEPOSITS)
    void purge(DepositTransaction depositTransaction);
} 