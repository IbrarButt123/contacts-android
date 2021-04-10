package contacts.util

import android.content.Context
import contacts.GroupsFields
import contacts.`in`
import contacts.entities.Group
import contacts.entities.MutableGroup
import contacts.equalTo
import contacts.groups.GroupsInsert
import contacts.groups.GroupsQuery

/**
 * Returns the newly created [Group] or null if the insert operation failed.
 *
 * ## Permissions
 *
 * The [contacts.ContactsPermissions.READ_PERMISSION] is required. Otherwise, null will be returned
 * if the permission is not granted.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
@JvmOverloads
fun GroupsInsert.Result.group(
    context: Context, group: MutableGroup, cancel: () -> Boolean = { false }
): Group? = groupId(group)?.let { groupId ->
    GroupsQuery(context).where(GroupsFields.Id equalTo groupId).find(cancel).firstOrNull()
}

/**
 * Returns the newly created [Group]s (for those insert operations that succeeded).
 *
 * ## Permissions
 *
 * The [contacts.ContactsPermissions.READ_PERMISSION] is required. Otherwise, null will be returned
 * if the permission is not granted.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
@JvmOverloads
fun GroupsInsert.Result.groups(context: Context, cancel: () -> Boolean = { false }): List<Group> =
    if (groupIds.isEmpty()) {
        emptyList()
    } else {
        GroupsQuery(context).where(GroupsFields.Id `in` groupIds).find(cancel)
    }