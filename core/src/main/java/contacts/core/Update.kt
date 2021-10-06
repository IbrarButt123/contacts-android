package contacts.core

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import contacts.core.accounts.Accounts
import contacts.core.entities.MutableContact
import contacts.core.entities.MutableRawContact
import contacts.core.entities.custom.CustomDataCountRestriction
import contacts.core.entities.custom.CustomDataRegistry
import contacts.core.entities.operation.*
import contacts.core.util.applyBatch
import contacts.core.util.unsafeLazy

/**
 * Updates one or more raw contacts' rows in the data table.
 *
 * This does not support updating user Profile Contact. For Profile updates, use
 * [contacts.core.profile.ProfileUpdate].
 *
 * ## Permissions
 *
 * The [ContactsPermissions.WRITE_PERMISSION] and
 * [contacts.core.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION] are assumed to have been
 * granted already in these examples for brevity. All updates will do nothing if these permissions
 * are not granted.
 *
 * ## Accounts
 *
 * These operations ensure that only [MutableRawContact.groupMemberships] belonging to the same
 * account as the raw contact are inserted.
 *
 * ## Usage
 *
 * To update a raw contact's name to "john doe" and add an email "john@doe.com";
 *
 * In Kotlin,
 *
 * ```kotlin
 * val mutableRawContact = rawContact.toMutableRawContact().apply {
 *      name = MutableName().apply {
 *          givenName = "john"
 *          familyName = "doe"
 *      }
 *      emails.add(MutableEmail().apply {
 *          type = Email.Type.HOME
 *          address = "john@doe.com"
 *      })
 * }
 *
 * val result = update
 *      .rawContacts(mutableRawContact)
 *      .commit()
 * ```
 *
 * Java,
 *
 * ```java
 * MutableName name = new MutableName();
 * name.setGivenName("john");
 * name.setFamilyName("doe");
 *
 * MutableEmail email = new MutableEmail();
 * email.setType(Email.Type.HOME);
 * email.setAddress("john@doe.com");
 *
 * MutableRawContact mutableRawContact = rawContact.toMutableRawContact();
 * mutableRawContact.setName(name);
 * mutableRawContact.getEmails().add(email);
 *
 * Update.Result result = update
 *      .rawContacts(mutableRawContact)
 *      .commit();
 * ```
 */
interface Update {

    /**
     * If [deleteBlanks] is set to true, then updating blank RawContacts
     * ([MutableRawContact.isBlank]) or blank Contacts ([MutableContact.isBlank]) will result in
     * their deletion. Otherwise, blanks will not be deleted and will result in a failed operation.
     * This flag is set to true by default.
     *
     * The Contacts Providers allows for RawContacts that have no rows in the Data table (let's call
     * them "blanks") to exist. The native Contacts app does not allow insertion of new RawContacts
     * without at least one data row. It also deletes blanks on update. Despite seemingly not
     * allowing blanks, the native Contacts app shows them.
     */
    fun deleteBlanks(deleteBlanks: Boolean): Update

    /**
     * Adds the given [rawContacts] to the update queue, which will be updated on [commit].
     *
     * Only existing [rawContacts] that have been retrieved via a query will be added to the
     * update queue. Those that have been manually created via a constructor will be ignored and
     * result in a failed operation.
     */
    fun rawContacts(vararg rawContacts: MutableRawContact): Update

    /**
     * See [Update.rawContacts].
     */
    fun rawContacts(rawContacts: Collection<MutableRawContact>): Update

    /**
     * See [Update.rawContacts].
     */
    fun rawContacts(rawContacts: Sequence<MutableRawContact>): Update

    /**
     * Adds the [MutableRawContact]s of the given [contacts] to the update queue, which will be
     * updated on [commit].
     *
     * See [rawContacts].
     */
    fun contacts(vararg contacts: MutableContact): Update

    /**
     * See [Update.contacts].
     */
    fun contacts(contacts: Collection<MutableContact>): Update

    /**
     * See [Update.contacts].
     */
    fun contacts(contacts: Sequence<MutableContact>): Update

    /**
     * Updates the [MutableRawContact]s in the queue (added via [rawContacts] and [contacts]) and
     * returns the [Result].
     *
     * ## Permissions
     *
     * Requires [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun commit(): Result

    /**
     * Updates the [MutableRawContact]s in the queue (added via [rawContacts] and [contacts]) and
     * returns the [Result].
     *
     * ## Permissions
     *
     * Requires [ContactsPermissions.WRITE_PERMISSION].
     *
     * ## Cancellation
     *
     * To cancel at any time, the [cancel] function should return true.
     *
     * This is useful when running this function in a background thread or coroutine.
     *
     * **Cancelling does not undo updates. This means that depending on when the cancellation
     * occurs, some if not all of the RawContacts in the update queue may have already been
     * updated.**
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    // @JvmOverloads cannot be used in interface methods...
    // fun commit(cancel: () -> Boolean = { false }): Result
    fun commit(cancel: () -> Boolean): Result

    interface Result {

        /**
         * True if all Contacts and RawContacts have successfully been updated. False if even one
         * update failed.
         */
        val isSuccessful: Boolean

        /**
         * True if the [rawContact] has been successfully updated. False otherwise.
         */
        fun isSuccessful(rawContact: MutableRawContact): Boolean

        /**
         * True if all of the [MutableContact.rawContacts] has been successfully updated. False
         * otherwise.
         *
         * ## Important
         *
         * If this [contact] has as [MutableRawContact] that has not been updated, then this will
         * return false. This may occur if only some (not all) of the [MutableRawContact] in
         * [MutableContact.rawContacts] has been added to the update queue via [Update.rawContacts].
         */
        fun isSuccessful(contact: MutableContact): Boolean
    }
}

@Suppress("FunctionName")
internal fun Update(
    context: Context, customDataRegistry: CustomDataRegistry
): Update = UpdateImpl(
    context.applicationContext,
    ContactsPermissions(context),
    customDataRegistry
)

private class UpdateImpl(
    private val applicationContext: Context,
    private val permissions: ContactsPermissions,
    private val customDataRegistry: CustomDataRegistry,

    private var deleteBlanks: Boolean = true,
    private val rawContacts: MutableSet<MutableRawContact> = mutableSetOf()
) : Update {

    override fun toString(): String =
        """
            Update {
                deleteBlanks: $deleteBlanks
                rawContacts: $rawContacts
            }
        """.trimIndent()

    override fun deleteBlanks(deleteBlanks: Boolean): Update = apply {
        this.deleteBlanks = deleteBlanks
    }

    override fun rawContacts(vararg rawContacts: MutableRawContact) =
        rawContacts(rawContacts.asSequence())

    override fun rawContacts(rawContacts: Collection<MutableRawContact>) =
        rawContacts(rawContacts.asSequence())

    override fun rawContacts(rawContacts: Sequence<MutableRawContact>): Update = apply {
        this.rawContacts.addAll(rawContacts)
    }

    override fun contacts(vararg contacts: MutableContact) =
        contacts(contacts.asSequence())

    override fun contacts(contacts: Collection<MutableContact>) =
        contacts(contacts.asSequence())

    override fun contacts(contacts: Sequence<MutableContact>): Update =
        rawContacts(contacts.flatMap { it.rawContacts.asSequence() })

    override fun commit(): Update.Result = commit { false }

    override fun commit(cancel: () -> Boolean): Update.Result {
        if (rawContacts.isEmpty() || !permissions.canUpdateDelete() || cancel()) {
            return UpdateFailed()
        }

        val results = mutableMapOf<Long, Boolean>()
        for (rawContact in rawContacts) {
            if (cancel()) {
                break
            }

            if (rawContact.id != null) {
                results[rawContact.id] = if (rawContact.isProfile) {
                    // Intentionally fail the operation to ensure that this is only used for
                    // non-profile updates. Otherwise, operation can succeed. This is only done to
                    // enforce API design.
                    false
                } else if (rawContact.isBlank && deleteBlanks) {
                    applicationContext.contentResolver.deleteRawContactWithId(rawContact.id)
                } else {
                    applicationContext.updateRawContact(
                        applicationContext,
                        customDataRegistry,
                        rawContact
                    )
                }
            } else {
                results[INVALID_ID] = false
            }
        }
        return UpdateResult(results)
    }

    private companion object {
        // A failed entry in the results so that Result.isSuccessful returns false.
        const val INVALID_ID = -1L
    }
}

/**
 * Updates a raw contact's data rows.
 *
 * If a raw contact attribute is null or the attribute's values are all null, then the
 * corresponding data row (if any) will be deleted.
 *
 * If only some of a raw contact's attribute's values are null, then a data row will be created
 * if it does not yet exist.
 */
internal fun Context.updateRawContact(
    context: Context,
    customDataRegistry: CustomDataRegistry,
    rawContact: MutableRawContact
): Boolean {
    if (rawContact.id == null) {
        return false
    }

    val isProfile = rawContact.isProfile
    val account = Accounts(context, isProfile).query().accountFor(rawContact)

    val operations = arrayListOf<ContentProviderOperation>()

    operations.addAll(
        AddressOperation(isProfile).updateInsertOrDelete(
            rawContact.addresses, rawContact.id, contentResolver
        )
    )

    operations.addAll(
        EmailOperation(isProfile).updateInsertOrDelete(
            rawContact.emails, rawContact.id, contentResolver
        )
    )

    if (account != null) {
        // I'm not sure why the native Contacts app hides events from the UI for local raw contacts.
        // The Contacts Provider does support having events for local raw contacts. Anyways, let's
        // follow in the footsteps of the native Contacts app...
        operations.addAll(
            EventOperation(isProfile).updateInsertOrDelete(
                rawContact.events, rawContact.id, contentResolver
            )
        )
    }

    if (account != null) {
        // Groups require an Account. Therefore, memberships to groups cannot exist without groups.
        // It should not be possible for consumers to get access to group memberships.
        // The Contacts Provider does support having events for local raw contacts.
        operations.addAll(
            GroupMembershipOperation(isProfile).updateInsertOrDelete(
                rawContact.groupMemberships, rawContact.id, this
            )
        )
    }

    operations.addAll(
        ImOperation(isProfile).updateInsertOrDelete(
            rawContact.ims, rawContact.id, contentResolver
        )
    )

    operations.add(
        NameOperation(isProfile).updateInsertOrDelete(
            rawContact.name, rawContact.id, contentResolver
        )
    )

    operations.add(
        NicknameOperation(isProfile).updateInsertOrDelete(
            rawContact.nickname, rawContact.id, contentResolver
        )
    )

    operations.add(
        NoteOperation(isProfile).updateInsertOrDelete(
            rawContact.note, rawContact.id, contentResolver
        )
    )

    operations.add(
        OrganizationOperation(isProfile).updateInsertOrDelete(
            rawContact.organization, rawContact.id, contentResolver
        )
    )

    operations.addAll(
        PhoneOperation(isProfile).updateInsertOrDelete(
            rawContact.phones, rawContact.id, contentResolver
        )
    )

    if (account != null) {
        // I'm not sure why the native Contacts app hides relations from the UI for local raw
        // contacts. The Contacts Provider does support having events for local raw contacts.
        // Anyways, let's follow in the footsteps of the native Contacts app...
        operations.addAll(
            RelationOperation(isProfile).updateInsertOrDelete(
                rawContact.relations, rawContact.id, contentResolver
            )
        )
    }

    operations.add(
        SipAddressOperation(isProfile).updateInsertOrDelete(
            rawContact.sipAddress, rawContact.id, contentResolver
        )
    )

    operations.addAll(
        WebsiteOperation(isProfile).updateInsertOrDelete(
            rawContact.websites, rawContact.id, contentResolver
        )
    )

    // Process custom data
    operations.addAll(
        rawContact.customDataUpdateInsertOrDeleteOperations(contentResolver, customDataRegistry)
    )

    /*
     * Atomically update all of the associated Data rows. All of the above operations will
     * either succeed or fail.
     */
    return contentResolver.applyBatch(operations) != null
}

private fun MutableRawContact.customDataUpdateInsertOrDeleteOperations(
    contentResolver: ContentResolver,
    customDataRegistry: CustomDataRegistry
): List<ContentProviderOperation> = mutableListOf<ContentProviderOperation>().apply {
    if (id == null) {
        return@apply
    }

    for ((mimeTypeValue, customDataEntityHolder) in customDataEntities) {
        val customDataEntry = customDataRegistry.entryOf(mimeTypeValue)

        val countRestriction = customDataEntry.countRestriction
        val customDataOperation = customDataEntry.operationFactory.create(isProfile)

        when (countRestriction) {
            CustomDataCountRestriction.AT_MOST_ONE -> {
                customDataOperation
                    .updateInsertOrDelete(
                        customDataEntityHolder.entities.firstOrNull(), id, contentResolver
                    ).let(::add)
            }
            CustomDataCountRestriction.NO_LIMIT -> {
                customDataOperation
                    .updateInsertOrDelete(customDataEntityHolder.entities, id, contentResolver)
                    .let(::addAll)
            }
        }
    }
}

private class UpdateResult(private val rawContactIdsResultMap: Map<Long, Boolean>) : Update.Result {

    override val isSuccessful: Boolean by unsafeLazy { rawContactIdsResultMap.all { it.value } }

    override fun isSuccessful(rawContact: MutableRawContact): Boolean = isSuccessful(rawContact.id)

    override fun isSuccessful(contact: MutableContact): Boolean {
        for (rawContactId in contact.rawContacts.asSequence().map { it.id }) {
            if (!isSuccessful(rawContactId)) {
                return false
            }
        }
        return true
    }

    private fun isSuccessful(rawContactId: Long?): Boolean = rawContactId != null
            && rawContactIdsResultMap.getOrElse(rawContactId) { false }
}

private class UpdateFailed : Update.Result {

    override val isSuccessful: Boolean = false

    override fun isSuccessful(rawContact: MutableRawContact): Boolean = false

    override fun isSuccessful(contact: MutableContact): Boolean = false
}