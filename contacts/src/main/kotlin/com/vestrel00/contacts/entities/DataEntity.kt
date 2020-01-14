package com.vestrel00.contacts.entities

/**
 * [Entity] in the data table that belong to a [RawContact].
 */
interface DataEntity : Entity {

    /**
     * The id of the [RawContact] that this data entity is associated with.
     */
    val rawContactId: Long

    /**
     * The id of the [Contact] that this data entity is associated with.
     */
    val contactId: Long

    /**
     * Whether this is the primary entry of its kind for the [RawContact] it belongs to.
     *
     * ## Developer Notes
     *
     * This is immutable to prevent consumers from setting multiple data entities of the same
     * mimetype as primary. Consumers should use the DefaultContactData extension functions to
     * modify these values.
     */
    val isPrimary: Boolean

    /**
     * Whether this is the primary entry of its kind for the aggregate [Contact] it belongs to. Any
     * data record that is "super primary" must also be [isPrimary].
     *
     * ## Developer Notes
     *
     * This is immutable to prevent consumers from setting multiple data entities of the same
     * mimetype as primary. Consumers should use the DefaultContactData extension functions to
     * modify these values.
     */
    val isSuperPrimary: Boolean

    /**
     * True if [isSuperPrimary] is true.
     *
     * "Default" is the terminology used by the native Contacts app. Consumers should use the
     * DefaultContactData extension functions to set a data entity as default or not.
     */
    fun isDefault(): Boolean = isSuperPrimary
}

// Keep the MimeType internal with this mapping function.
// FIXME Remove this mapping and add mimeType val to DataEntity interface if mimetypes are to be
// made public.
internal fun DataEntity.mimeType(): MimeType = when (this) {
    is Address, is MutableAddress -> MimeType.ADDRESS
    is Company, is MutableCompany -> MimeType.COMPANY
    is Email, is MutableEmail -> MimeType.EMAIL
    is Event, is MutableEvent -> MimeType.EVENT
    is GroupMembership -> MimeType.GROUP_MEMBERSHIP
    is Im, is MutableIm -> MimeType.IM
    is Name, is MutableName -> MimeType.NAME
    is Nickname, is MutableNickname -> MimeType.NICKNAME
    is Note, is MutableNote -> MimeType.NOTE
    is Phone, is MutablePhone -> MimeType.PHONE
    is Relation, is MutableRelation -> MimeType.RELATION
    is SipAddress, is MutableSipAddress -> MimeType.SIP_ADDRESS
    is Website, is MutableWebsite -> MimeType.WEBSITE

    // There is currently no Photo class.
    // Throw this exception to prevent programmer error.
    else -> throw RuntimeException(
        "DataEntity ${this.javaClass.simpleName} is not mapped to a mimetype"
    )
}