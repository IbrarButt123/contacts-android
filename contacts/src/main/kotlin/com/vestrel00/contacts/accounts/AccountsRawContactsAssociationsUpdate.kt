package com.vestrel00.contacts.accounts

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import com.vestrel00.contacts.*
import com.vestrel00.contacts.entities.MimeType
import com.vestrel00.contacts.entities.RawContactEntity
import com.vestrel00.contacts.entities.cursor.rawContactsCursor
import com.vestrel00.contacts.entities.operation.newDelete
import com.vestrel00.contacts.entities.operation.newUpdate
import com.vestrel00.contacts.entities.operation.withSelection
import com.vestrel00.contacts.entities.operation.withValue
import com.vestrel00.contacts.entities.table.Table
import com.vestrel00.contacts.util.*

// TODO verify that Contacts that have been transferred to another Account are sync'ed.
// Transferring Contact A of Account X to Account Y results in the removal of Contact A from
// Account X and the addition to Account Y.
// - Dissociating RawContact A from device X does not dissociate RawContact A from other devices.
//   - RawContact A is no longer visible in the native Contacts app.
// - Associating RawContact A from original Account X to Account Y results in;
//     - No change in other devices.
//     - For Lollipop (API 22) and below, RawContact A is no longer visible in the native Contacts
//       app and syncing Account Y in system settings fails.
//     - For Marshmallow (API 23) and above, RawContact A is no longer visible in the native
//       Contacts app. RawContact A is automatically deleted locally at some point by the Contacts
//       Provider. Syncing Account Y in system settings succeeds.
//     - In short, changing the Account manually will either result in syncing issues or unwanted
//       side effects.

// TODO Update DEV_NOTES data required and groups / group membership sections.
// Contacts Provider automatically creates a group membership to the default group of the target Account when the account changes.
//     - This occurs even if the group membership already exists resulting in duplicates.
// Contacts Provider DOES NOT delete existing group memberships when the account changes.
//     - This has to be done manually to prevent duplicates.
// For Lollipop (API 22) and below, the Contacts Provider sets null account references to non-null asynchronously.
//     - Just add a note about this behavior.
//     - The Contacts Provider keeps performing this check routinely.

/**
 * TODO Documentation
 */
interface AccountsRawContactsAssociationsUpdate {

    // region ASSOCIATE

    /**
     * Associates the given [rawContacts] with the given [account].
     *
     * RawContacts that were already associated with an Account will no longer be associated with
     * that Account if this call succeeds. Existing group memberships will be deleted. A group
     * membership to the default group of the given [account] will be created automatically by the
     * Contacts Provider upon successful operation.
     *
     * Only existing RawContacts that have been retrieved via a query will be processed. Those that
     * have been manually created via a constructor will be ignored.
     *
     * This operation will fail if the given [account] is not in the system or if no existing
     * RawContacts are provided.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContacts(
        account: Account, vararg rawContacts: RawContactEntity
    ): Boolean

    /**
     * See [associateAccountWithRawContacts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContacts(
        account: Account, rawContacts: Collection<RawContactEntity>
    ): Boolean

    /**
     * See [associateAccountWithRawContacts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContacts(
        account: Account, rawContacts: Sequence<RawContactEntity>
    ): Boolean

    /**
     * Associates all local RawContacts with the given [account].
     *
     * Local RawContacts are those that are not associated with any Account. A group membership to
     * the default group of the given [account] will be created automatically by the Contacts
     * Provider upon successful operation.
     *
     * Only existing RawContacts that have been retrieved via a query will be processed. Those that
     * have been manually created via a constructor will be ignored.
     *
     * This operation will fail if the given [account] is not in the system.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithLocalRawContacts(account: Account): Boolean

    /**
     * Associates / transfers the RawContacts associated with the [srcAccounts] to the [dstAccount].
     *
     * RawContacts associated with the [srcAccounts] will no longer be associated with those
     * Accounts if this call succeeds. Existing group memberships will be deleted. A group
     * membership to the default group of the given [dstAccount] will be created automatically by
     * the Contacts Provider upon successful operation.
     *
     * This operation will fail if the given [dstAccount] is not in the system or if no
     * [srcAccounts] are provided. In the case where there are no associated RawContacts with any
     * of the [srcAccounts], this operation succeeds.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account, vararg srcAccounts: Account
    ): Boolean

    /**
     * See [associateAccountWithRawContactsFromAccounts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account, srcAccounts: Collection<Account>
    ): Boolean

    /**
     * See [associateAccountWithRawContactsFromAccounts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account, srcAccounts: Sequence<Account>
    ): Boolean

    /**
     * Associates / transfers the RawContacts associated with any Account to the [dstAccount].
     * This does not include local RawContacts, which are not associated with an Account.
     *
     * RawContacts associated with an Account will no longer be associated with those Accounts if
     * this call succeeds. Existing group memberships will be deleted. A group membership to the
     * default group of the given [dstAccount] will be created automatically by the Contacts
     * Provider upon successful operation.
     *
     * This operation will fail if the given [dstAccount] is not in the system. In the case where
     * there are no associated RawContacts with any existing Accounts, this operation succeeds.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithRawContactsFromAllAccounts(dstAccount: Account): Boolean

    /**
     * Associates / transfers all RawContacts from all Accounts including local RawContacts to the
     * [dstAccount].
     *
     * RawContacts associated with an Account will no longer be associated with those Accounts if
     * this call succeeds. Existing group memberships will be deleted. A group membership to the
     * default group of the given [dstAccount] will be created automatically by the Contacts
     * Provider upon successful operation.
     *
     * This operation will fail if the given [dstAccount] is not in the system. In the case where
     * there are no RawContacts, this operation succeeds.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun associateAccountWithAllRawContacts(dstAccount: Account): Boolean

    // endregion

    // region DISSOCIATE

    /**
     * Dissociates the given [rawContacts] from associations with any Account.
     *
     * RawContacts that were already associated with an Account will no longer be associated with
     * that Account if this call succeeds. Existing group memberships will be deleted. RawContacts
     * not associated with an Account are local to the device.
     *
     * Only existing RawContacts that have been retrieved via a query will be processed. Those that
     * have been manually created via a constructor will be ignored.
     *
     * This operation will fail if no existing RawContacts are provided.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContacts(vararg rawContacts: RawContactEntity): Boolean

    /**
     * See [dissociateRawContacts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContacts(rawContacts: Collection<RawContactEntity>): Boolean

    /**
     * See [dissociateRawContacts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContacts(rawContacts: Sequence<RawContactEntity>): Boolean

    /**
     * Dissociates the RawContacts associated with any of the given [accounts].
     *
     * RawContacts associated with any of the [accounts] will no longer be associated with those
     * Accounts if this call succeeds. Existing group memberships will be deleted. RawContacts
     * not associated with an Account are local to the device.
     *
     * In the case where there are no associated RawContacts with any of the [accounts], this
     * operation succeeds.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContactsFromAccounts(vararg accounts: Account): Boolean

    /**
     * See [dissociateRawContactsFromAccounts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContactsFromAccounts(accounts: Collection<Account>): Boolean

    /**
     * See [dissociateRawContactsFromAccounts].
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContactsFromAccounts(accounts: Sequence<Account>): Boolean

    /**
     * Dissociates the RawContacts associated with any Account.
     *
     * RawContacts associated with any Account will no longer be associated with those Accounts if
     * this call succeeds. Existing group memberships will be deleted. RawContacts not associated
     * with an Account are local to the device.
     *
     * In the case where there are no associated RawContacts with any Account, this operation
     * succeeds.
     *
     * ## Permissions
     *
     * Requires [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
     * [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun dissociateRawContactsFromAllAccounts(): Boolean

    // endregion
}

@Suppress("FunctionName")
internal fun AccountsRawContactsAssociationsUpdate(context: Context):
        AccountsRawContactsAssociationsUpdate = AccountsRawContactsAssociationsUpdateImpl(
    context, AccountsPermissions(context)
)

private class AccountsRawContactsAssociationsUpdateImpl(
    private val context: Context,
    private val permissions: AccountsPermissions
) : AccountsRawContactsAssociationsUpdate {

    // region ASSOCIATE

    override fun associateAccountWithRawContacts(
        account: Account, vararg rawContacts: RawContactEntity
    ) = associateAccountWithRawContacts(account, rawContacts.asSequence())

    override fun associateAccountWithRawContacts(
        account: Account, rawContacts: Collection<RawContactEntity>
    ) = associateAccountWithRawContacts(account, rawContacts.asSequence())

    override fun associateAccountWithRawContacts(
        account: Account, rawContacts: Sequence<RawContactEntity>
    ): Boolean {

        // Only existing RawContacts can be associated with an Account.
        val nonNullRawContactIds = rawContacts.map { it.id }.filterNotNull()

        return nonNullRawContactIds.isNotEmpty()
                && permissions.canUpdateRawContactsAssociations()
                && account.isInSystem(context)
                && context.contentResolver.updateRawContactsAccount(account, nonNullRawContactIds)
    }

    override fun associateAccountWithLocalRawContacts(account: Account): Boolean =
        permissions.canUpdateRawContactsAssociations()
                && account.isInSystem(context)
                && context.contentResolver.updateLocalRawContactsAccounts(account)

    override fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account,
        vararg srcAccounts: Account
    ) = associateAccountWithRawContactsFromAccounts(dstAccount, srcAccounts.asSequence())

    override fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account,
        srcAccounts: Collection<Account>
    ) = associateAccountWithRawContactsFromAccounts(dstAccount, srcAccounts.asSequence())

    override fun associateAccountWithRawContactsFromAccounts(
        dstAccount: Account,
        srcAccounts: Sequence<Account>
    ): Boolean {
        if (!permissions.canUpdateRawContactsAssociations()
            || srcAccounts.isEmpty()
            || dstAccount.isNotInSystem(context)
        ) {
            return false
        }

        val rawContactIdsFromSrcAccounts =
            context.contentResolver.rawContactIdsWhere(srcAccounts.toRawContactsWhere())

        // Succeed if there are no RawContacts from the source Accounts.
        // Using the || operator here is important because if it is true, then the update does
        // not occur. If && is used instead, the update will occur even if it is true.
        return rawContactIdsFromSrcAccounts.isEmpty() || context.contentResolver
            .updateRawContactsAccount(dstAccount, rawContactIdsFromSrcAccounts)
    }

    override fun associateAccountWithRawContactsFromAllAccounts(dstAccount: Account): Boolean {
        if (!permissions.canUpdateRawContactsAssociations() || dstAccount.isNotInSystem(context)) {
            return false
        }

        val rawContactIdsAssociatedWithAnAccount = context.contentResolver.rawContactIdsWhere(
            RawContactsFields.AccountName.isNotNull() and RawContactsFields.AccountType.isNotNull()
        )

        // Succeed if there are no RawContacts associated with any Account.
        // Using the || operator here is important because if it is true, then the update does
        // not occur. If && is used instead, the update will occur even if it is true.
        return rawContactIdsAssociatedWithAnAccount.isEmpty() || context.contentResolver
            .updateRawContactsAccount(dstAccount, rawContactIdsAssociatedWithAnAccount)
    }

    override fun associateAccountWithAllRawContacts(dstAccount: Account): Boolean =
        permissions.canUpdateRawContactsAssociations()
                && dstAccount.isInSystem(context)
                && context.contentResolver.updateRawContactsAccount(
            dstAccount,
            // Delete all group memberships.
            Fields.MimeType equalTo MimeType.GROUP_MEMBERSHIP,
            // Associate all existing RawContacts.
            RawContactsFields.ContactId.isNotNull()
        )

    // endregion

    // region DISSOCIATE

    override fun dissociateRawContacts(vararg rawContacts: RawContactEntity) =
        dissociateRawContacts(rawContacts.asSequence())

    override fun dissociateRawContacts(rawContacts: Collection<RawContactEntity>) =
        dissociateRawContacts(rawContacts.asSequence())

    override fun dissociateRawContacts(rawContacts: Sequence<RawContactEntity>): Boolean {
        // Only existing RawContacts can be processed.
        val nonNullRawContactIds = rawContacts.map { it.id }.filterNotNull()

        return nonNullRawContactIds.isNotEmpty()
                && permissions.canUpdateRawContactsAssociations()
                && context.contentResolver.updateRawContactsAccount(null, nonNullRawContactIds)
    }

    override fun dissociateRawContactsFromAccounts(vararg accounts: Account) =
        dissociateRawContactsFromAccounts(accounts.asSequence())

    override fun dissociateRawContactsFromAccounts(accounts: Collection<Account>) =
        dissociateRawContactsFromAccounts(accounts.asSequence())

    override fun dissociateRawContactsFromAccounts(accounts: Sequence<Account>): Boolean {
        if (!permissions.canUpdateRawContactsAssociations() || accounts.isEmpty()) {
            return false
        }

        val rawContactIdsFromAccounts =
            context.contentResolver.rawContactIdsWhere(accounts.toRawContactsWhere())

        // Succeed if there are no RawContacts from the source Accounts.
        // Using the || operator here is important because if it is true, then the update does
        // not occur. If && is used instead, the update will occur even if it is true.
        return rawContactIdsFromAccounts.isEmpty() || context.contentResolver
            .updateRawContactsAccount(null, rawContactIdsFromAccounts)
    }

    override fun dissociateRawContactsFromAllAccounts() =
        permissions.canUpdateRawContactsAssociations()
                && context.contentResolver.updateRawContactsAccount(
            null,
            // Delete all group memberships.
            Fields.MimeType equalTo MimeType.GROUP_MEMBERSHIP,
            // Dissociate all existing RawContacts.
            RawContactsFields.ContactId.isNotNull()
        )

    // endregion
}

private fun ContentResolver.updateRawContactsAccount(
    account: Account?, rawContactIds: Set<Long>
): Boolean = updateRawContactsAccount(account, rawContactIds.asSequence())

private fun ContentResolver.updateRawContactsAccount(
    account: Account?, rawContactIds: Sequence<Long>
): Boolean = updateRawContactsAccount(
    account,
    (Fields.RawContact.Id `in` rawContactIds)
            and (Fields.MimeType equalTo MimeType.GROUP_MEMBERSHIP),
    RawContactsFields.Id `in` rawContactIds
)

/**
 * Deletes existing group memberships in the Data table matching [dataWhere] and then updates the
 * sync columns in the RawContacts table matching [rawContactsWhere] with the given [account]. These
 * two operations are done in a batch so either both succeed or both fail.
 */
private fun ContentResolver.updateRawContactsAccount(
    account: Account?, dataWhere: Where, rawContactsWhere: Where
): Boolean = applyBatch(
    // First delete existing group memberships.
    newDelete(Table.DATA)
        .withSelection(dataWhere)
        .build(),
    // Then update the sync columns.
    newUpdate(Table.RAW_CONTACTS)
        .withSelection(rawContactsWhere)
        .withValue(RawContactsFields.AccountName, account?.name)
        .withValue(RawContactsFields.AccountType, account?.type)
        .build()
) != null

private fun ContentResolver.updateLocalRawContactsAccounts(account: Account): Boolean = applyBatch(
    // No need to delete existing group memberships because local RawContacts are not associated
    // with an Account and therefore do not have any group memberships.

    // Update the sync columns of RawContacts without an associated Account. This does not include
    // RawContacts associated with invalid Accounts.
    newUpdate(Table.RAW_CONTACTS)
        .withSelection(
            RawContactsFields.AccountName.isNull() or RawContactsFields.AccountType.isNull()
        )
        .withValue(RawContactsFields.AccountName, account.name)
        .withValue(RawContactsFields.AccountType, account.type)
        .build()
) != null

private fun ContentResolver.rawContactIdsWhere(where: Where?):
        Set<Long> = query(Table.RAW_CONTACTS, Include(RawContactsFields.Id), where) {
    val rawContactIds = mutableSetOf<Long>()
    val rawContactsCursor = it.rawContactsCursor()

    while (it.moveToNext()) {
        rawContactsCursor.rawContactId?.let(rawContactIds::add)
    }

    rawContactIds
} ?: emptySet()