package com.vestrel00.contacts.entities.operation

import com.vestrel00.contacts.AbstractField
import com.vestrel00.contacts.Fields
import com.vestrel00.contacts.entities.MimeType
import com.vestrel00.contacts.entities.MutableOrganization

internal class OrganizationOperation : AbstractDataOperation<MutableOrganization>() {

    override val mimeType = MimeType.ORGANIZATION

    override fun setData(
        data: MutableOrganization, setValue: (field: AbstractField, dataValue: Any?) -> Unit
    ) {
        setValue(Fields.Organization.Company, data.company)
        setValue(Fields.Organization.Title, data.title)
        setValue(Fields.Organization.Department, data.department)
        setValue(Fields.Organization.JobDescription, data.jobDescription)
        setValue(Fields.Organization.OfficeLocation, data.officeLocation)

        setValue(Fields.Organization.Symbol, data.symbol)
        setValue(Fields.Organization.PhoneticName, data.phoneticName)
    }
}