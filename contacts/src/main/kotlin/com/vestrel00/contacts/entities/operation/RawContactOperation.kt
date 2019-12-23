package com.vestrel00.contacts.entities.operation

import android.accounts.Account
import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newInsert
import com.vestrel00.contacts.Fields
import com.vestrel00.contacts.entities.table.Table
import com.vestrel00.contacts.equalTo

private val TABLE_URI = Table.RAW_CONTACTS.uri

/**
 * Builds [ContentProviderOperation]s for [Table.RAW_CONTACTS].
 */
internal class RawContactOperation {

    fun insert(rawContactAccount: Account?): ContentProviderOperation = newInsert(TABLE_URI)
        /*
         * Passing in null account name and type is valid. It is the same behavior as the native
         * Android Contacts app when creating contacts when there are no available accounts. When an
         * account becomes available (or is already available), Android will automatically update
         * the RawContact name and type to an existing Account.
         */
        .withValue(Fields.RawContact.AccountName, rawContactAccount?.name)
        .withValue(Fields.RawContact.AccountType, rawContactAccount?.type)
        .build()

    fun deleteRawContact(rawContactId: Long): ContentProviderOperation = newDelete(TABLE_URI)
        .withSelection("${Fields.RawContact.Id equalTo rawContactId}", null)
        .build()

    /*
     * Deleting all of the RawContact rows matching the Contacts._ID will result in the automatic
     * deletion of the Contacts row and associated Data rows.
     */
    fun deleteContact(contactId: Long): ContentProviderOperation = newDelete(TABLE_URI)
        .withSelection("${Fields.RawContact.ContactId equalTo contactId}", null)
        .build()
}