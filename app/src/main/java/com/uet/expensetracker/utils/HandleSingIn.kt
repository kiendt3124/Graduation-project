package com.uet.expensetracker.utils

import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

fun handleSignIn(credential: Credential, firebaseAuthWithGoogle: (idToken: String) -> Unit, errorHandler: (error: String) -> Unit){
    // Check if credential is of type Google ID

    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        // Create Google ID Token
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

        // Sign in to Firebase with using the token
        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
    } else {
        errorHandler("Credential is not of type Google ID!")
    }
}