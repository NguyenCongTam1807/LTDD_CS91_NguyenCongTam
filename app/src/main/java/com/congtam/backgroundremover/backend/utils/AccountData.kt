package com.congtam.backgroundremover.backend.utils

import com.google.gson.annotations.SerializedName

class AccountData (
    @SerializedName("data")
    val data: Data
) {
    class Data(
    @SerializedName("attributes")
    val attributes: Attribute,
    ) {
        class Attribute(
            @SerializedName("credits")
            val credits: Credit,
            @SerializedName("api")
            val api: Api
        ) {
            class Credit(
                @SerializedName("total")
                val total: Int,
                @SerializedName("subscription")
                val subscription: Int,
                @SerializedName("payg")
                val payg: Int,
                @SerializedName("enterprise")
                val enterprise: Int,
            )

            class Api(
                @SerializedName("free_calls")
                val freeCalls: Int,
                @SerializedName("sizes")
                val sizes: String,
            )
        }
    }
}

