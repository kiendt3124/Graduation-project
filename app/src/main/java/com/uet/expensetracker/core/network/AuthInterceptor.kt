package com.uet.expensetracker.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * OkHttp interceptor that attaches Firebase ID token as Bearer token.
 * If user is not signed in or token retrieval fails, the request proceeds without Authorization header.
 */
class AuthInterceptor(
    private val auth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val user = auth.currentUser
        if (user == null) {
            android.util.Log.w("AuthInterceptor", "No user logged in, proceeding without token")
            return chain.proceed(original)
        }

        return try {
            val token = Tasks.await(user.getIdToken(true), 5, TimeUnit.SECONDS)?.token
            if (token.isNullOrBlank()) {
                android.util.Log.w("AuthInterceptor", "Token is null or blank")
                chain.proceed(original)
            } else {
                android.util.Log.d("AuthInterceptor", "Token added: ${token.take(20)}...")
                val newReq = original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(newReq)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthInterceptor", "Error getting token: ${e.message}")
            chain.proceed(original)
        }
    }
}


