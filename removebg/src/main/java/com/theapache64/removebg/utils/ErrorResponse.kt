package com.theapache64.removebg.utils


import com.google.gson.annotations.SerializedName

class ErrorResponse(
    @SerializedName("errors")
    val errors: List<Error>
) {

    class Error(
        @SerializedName("title")
        val title: String,
        @SerializedName("detail")
        val detail: String,
        @SerializedName("code")
        val code: String
    )
}