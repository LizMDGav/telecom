package com.example.telecom.model

import java.util.UUID

data class ContactData(
    var id: String = UUID.randomUUID().toString(),
    var name:String,
    var phoneNumber:String,
    var photoUri: String? = null,
    var isImported: Boolean = false
)
