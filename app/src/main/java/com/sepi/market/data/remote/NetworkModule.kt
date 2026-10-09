package com.sepi.market.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sepi.market.BuildConfig
import com.sepi.market.core.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Modul pembuat instance OkHttpClient, Gson, Retrofit, AuthApi, dan RestApi.
 */
class NetworkModule(
    private val sessionManager: SessionManager
) {

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val authInterceptor = AuthInterceptor(sessionManager)
    private val tokenAuthenticator = TokenAuthenticator(sessionManager, gson)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        // Wajib menyembunyikan kredensial rahasia dari logcat (PRD Bagian 12)
        redactHeader("Authorization")
        redactHeader("apikey")
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    private val baseUrl: String = if (BuildConfig.SUPABASE_URL.isNotBlank()) {
        BuildConfig.SUPABASE_URL.trimEnd('/') + "/"
    } else {
        "https://example.supabase.co/" // Fallback aman agar Retrofit tidak crash saat key kosong
    }

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val restApi: RestApi = retrofit.create(RestApi::class.java)
}
