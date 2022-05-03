package com.congtam.backgroundremover.backend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.congtam.backgroundremover.backend.utils.AccountData
import com.congtam.backgroundremover.backend.utils.CountingFileRequestBody
import com.congtam.backgroundremover.backend.utils.ErrorResponse
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.File
import java.io.IOException


object RemoveBg {
    private const val BASE_URL = "https://api.remove.bg/v1.0"
    private const val REMOVE_BG = "/removebg"
    private const val ACCOUNT = "/account"
    private var apiKey: String? = null
    private lateinit var parent: Fragment
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    private val gson by lazy {
        GsonBuilder().create()
    }

    /**
     * To initialize the apikey. Should be called before calling from method.
     */
    fun init(apiKey: String, parent: Fragment) {
        RemoveBg.apiKey = apiKey
        this.parent = parent
    }


    /**
     * To remove background from the given image file.
     */
    fun from(file: File, callback: RemoveBgCallback) {
        require(apiKey != null) { "You must call RemoveBg.init before calling RemoveBg.from" }

        // file
        val filePart = CountingFileRequestBody(
            file,
            "image/png",
            object : CountingFileRequestBody.ProgressListener {
                override fun transferred(percentage: Float) {
                    callback.onUploadProgress(percentage)
                    if (percentage >= 100) {
                        callback.onProcessing()
                    }
                }
            })

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("size", "auto")
            .addFormDataPart("image_file", file.name, filePart)
            .build()

        // new request
        val request = Request.Builder()
            .url(BASE_URL + REMOVE_BG)
            .addHeader("X-Api-Key", apiKey!!)
            .post(body)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // success, converting to bitmap
                    response.body()!!.byteStream().let { bytesStream ->
                        val bmp = BitmapFactory.decodeStream(bytesStream)
                        callback.onSuccess(bmp)
                    }
                    val request = Request.Builder()
                        .url(BASE_URL + ACCOUNT)
                        .addHeader("X-Api-Key", apiKey!!)
                        .get()
                        .build()
                    okHttpClient.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {}

                        override fun onResponse(call: Call, response: Response) {
                            val jsonResp = response.body()!!.string()
                            val responseObject = gson.fromJson(jsonResp, AccountData::class.java)
                            PreferenceManager.getDefaultSharedPreferences(parent.requireContext()).edit()
                                .putString("freeCalls",responseObject.data.attributes.api.freeCalls.toString()).apply()
                        }

                    })
                } else {
                    // error, parsing error.
                    val jsonResp = response.body()!!.string()
                    val errorResp = gson.fromJson(jsonResp, ErrorResponse::class.java)
                    callback.onError(errorResp.errors)
                }
            }
        })
    }

    interface RemoveBgCallback {
        fun onUploadProgress(progress: Float)
        fun onProcessing()
        fun onSuccess(bitmap: Bitmap)
        fun onError(errors: List<ErrorResponse.Error>)
    }
}