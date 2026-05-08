package com.uet.expensetracker.features.money.ui.account

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.uet.expensetracker.features.money.data.data_source.local.db.LocalDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.uet.expensetracker.R
import java.io.File

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : BaseViewModel<AccountState, AccountIntent, AccountEvent>() {

    private val databaseName = LocalDatabase.DATABASE_NAME

    override val _viewState = MutableStateFlow(AccountState())

    override fun processIntent(intent: AccountIntent) {
        when (intent) {
            AccountIntent.LoadProfile -> loadProfile()
            AccountIntent.SignIn -> signIn()
            is AccountIntent.GoogleSignIn -> handleGoogleSignIn(intent.idToken)
            is AccountIntent.SignInError -> emitEvent(AccountEvent.ShowError(intent.message))
            AccountIntent.SignOut -> signOut()
            is AccountIntent.SignOutWithBackup -> signOutWithBackup(intent.context)
            is AccountIntent.BackupData -> backupData(intent.context)
            is AccountIntent.RestoreData -> restoreData(intent.context)
        }
    }

    private fun handleGoogleSignIn(idToken: String?) {
        Log.i("signin", "handleGoogleSignIn: (\"Google sign-in successful\")")

        viewModelScope.launch {
            if (idToken.isNullOrBlank()) {
                _viewState.value = _viewState.value.copy(isSigningIn = false)
                emitEvent(AccountEvent.ShowError("No token received"))
                return@launch
            }
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        processIntent(AccountIntent.LoadProfile)
                    }
                    .addOnFailureListener { e: Exception ->
                        _viewState.value = _viewState.value.copy(isSigningIn = false)
                        emitEvent(AccountEvent.ShowError("Auth failed: ${e.localizedMessage}"))
                    }
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isSigningIn = false)
                emitEvent(AccountEvent.ShowError(e.localizedMessage ?: "Sign-in error"))
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val wasSignedIn = _viewState.value.isSignedIn
                _viewState.value = _viewState.value.copy(
                    isSignedIn = true,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString(),
                    isSigningIn = false
                )
                logFirebaseIdToken()
                if (!wasSignedIn) {
                    emitEvent(AccountEvent.ShowPostLoginRestorePrompt)
                }
            } else {
                // Signed out / not signed in
                _viewState.value = AccountState()
            }
        }
    }

    private fun signIn() {
        // Trigger UI sign-in flow
        _viewState.value = _viewState.value.copy(isSigningIn = true)
        emitEvent(AccountEvent.LaunchSignInFlow)
    }

    private fun signOut() {
        viewModelScope.launch {
            if (_viewState.value.isSigningOut) return@launch
            _viewState.value = _viewState.value.copy(isSigningOut = true)
            // Small delay so UI can show a sign-out effect (loading state)
            delay(350)
            auth.signOut()
            _viewState.value = AccountState()
            emitEvent(AccountEvent.SignOutSuccess)
        }
    }

    private fun signOutWithBackup(context: Context) {
        viewModelScope.launch {
            if (_viewState.value.isSigningOut) return@launch
            _viewState.value = _viewState.value.copy(isSigningOut = true, isBackupLoading = true)

            val user = auth.currentUser
            if (user == null) {
                _viewState.value = _viewState.value.copy(isSigningOut = false, isBackupLoading = false)
                emitEvent(AccountEvent.ShowError("Please sign in first"))
                return@launch
            }

            try {
                val dbFile = context.getDatabasePath(databaseName)
                val dbDir = dbFile.parentFile!!
                val walFile = File(dbDir, "${databaseName}-wal")
                val shmFile = File(dbDir, "${databaseName}-shm")
                val refDb = storage.reference.child("backups/${user.uid}/$databaseName")
                val refWal = storage.reference.child("backups/${user.uid}/${databaseName}-wal")
                val refShm = storage.reference.child("backups/${user.uid}/${databaseName}-shm")

                refDb.putFile(Uri.fromFile(dbFile))
                    .addOnSuccessListener {
                        refWal.putFile(Uri.fromFile(walFile))
                            .addOnSuccessListener {
                                refShm.putFile(Uri.fromFile(shmFile))
                                    .addOnSuccessListener {
                                        emitEvent(AccountEvent.BackupSuccess)
                                        // Give UI a moment to show success/transition, then sign out
                                        viewModelScope.launch {
                                            _viewState.value = _viewState.value.copy(isBackupLoading = false)
                                            delay(350)
                                            auth.signOut()
                                            _viewState.value = AccountState()
                                            emitEvent(AccountEvent.SignOutSuccess)
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        _viewState.value = _viewState.value.copy(isSigningOut = false, isBackupLoading = false)
                                        emitEvent(AccountEvent.ShowError(error.message ?: "Backup shm failed"))
                                    }
                            }
                            .addOnFailureListener { error ->
                                _viewState.value = _viewState.value.copy(isSigningOut = false, isBackupLoading = false)
                                emitEvent(AccountEvent.ShowError(error.message ?: "Backup wal failed"))
                            }
                    }
                    .addOnFailureListener { error ->
                        _viewState.value = _viewState.value.copy(isSigningOut = false, isBackupLoading = false)
                        emitEvent(AccountEvent.ShowError(error.message ?: "Backup db failed"))
                    }
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isSigningOut = false, isBackupLoading = false)
                emitEvent(AccountEvent.ShowError(e.localizedMessage ?: "Backup error"))
            }
        }
    }

    private fun backupData(context: Context) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(isBackupLoading = true)
            val user = auth.currentUser
            if (user == null) {
                emitEvent(AccountEvent.ShowError(context.getString(R.string.account_error_please_sign_in)))
                _viewState.value = _viewState.value.copy(isBackupLoading = false)
                return@launch
            }
            try {

                val dbFile = context.getDatabasePath(databaseName)
                val dbDir = dbFile.parentFile!!
                val walFile = File(dbDir, "${databaseName}-wal")
                val shmFile = File(dbDir, "${databaseName}-shm")
                val refDb = storage.reference.child("backups/${user.uid}/$databaseName")
                val refWal = storage.reference.child("backups/${user.uid}/${databaseName}-wal")
                val refShm = storage.reference.child("backups/${user.uid}/${databaseName}-shm")
                refDb.putFile(Uri.fromFile(dbFile))
                    .addOnSuccessListener {
                        refWal.putFile(Uri.fromFile(walFile))
                            .addOnSuccessListener {
                                refShm.putFile(Uri.fromFile(shmFile))
                                    .addOnSuccessListener {
                                        emitEvent(AccountEvent.BackupSuccess)
                                        _viewState.value = _viewState.value.copy(isBackupLoading = false)
                                    }
                                    .addOnFailureListener { error ->
                                        emitEvent(AccountEvent.ShowError(error.message ?: context.getString(R.string.account_error_backup_shm_failed)))
                                        _viewState.value = _viewState.value.copy(isBackupLoading = false)
                                    }
                            }
                            .addOnFailureListener { error ->
                                emitEvent(AccountEvent.ShowError(error.message ?: context.getString(R.string.account_error_backup_wal_failed)))
                                _viewState.value = _viewState.value.copy(isBackupLoading = false)
                            }
                    }
                    .addOnFailureListener { error ->
                        emitEvent(AccountEvent.ShowError(error.message ?: context.getString(R.string.account_error_backup_db_failed)))
                        _viewState.value = _viewState.value.copy(isBackupLoading = false)
                    }
            } catch (e: Exception) {
                emitEvent(AccountEvent.ShowError(e.localizedMessage ?: context.getString(R.string.account_error_backup)))
                _viewState.value = _viewState.value.copy(isBackupLoading = false)
            }
        }
    }

    private fun restoreData(context: Context) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(isRestoreLoading = true)
            val user = auth.currentUser
            if (user == null) {
                emitEvent(AccountEvent.ShowError(context.getString(R.string.account_error_please_sign_in)))
                _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                return@launch
            }
            try {
                val localFile = context.getDatabasePath(databaseName)
                val dbDir = localFile.parentFile!!
                val walFile = File(dbDir, "${databaseName}-wal")
                val shmFile = File(dbDir, "${databaseName}-shm")
                val refDb = storage.reference.child("backups/${user.uid}/$databaseName")
                val refWal = storage.reference.child("backups/${user.uid}/${databaseName}-wal")
                val refShm = storage.reference.child("backups/${user.uid}/${databaseName}-shm")
                refDb.getFile(localFile)
                    .addOnSuccessListener {
                        refWal.getFile(walFile)
                            .addOnSuccessListener {
                                refShm.getFile(shmFile)
                                    .addOnSuccessListener {
                                        emitEvent(AccountEvent.RestoreSuccess)
                                        _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                                    }
                                    .addOnFailureListener { error ->
                                        emitEvent(AccountEvent.ShowError(error.message ?: "Restore shm failed"))
                                        _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                                    }
                            }
                            .addOnFailureListener { error ->
                                emitEvent(AccountEvent.ShowError(error.message ?: "Restore wal failed"))
                                _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                            }
                    }
                    .addOnFailureListener { error ->
                        emitEvent(AccountEvent.ShowError(error.message ?: "Restore db failed"))
                        _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                    }
            } catch (e: Exception) {
                emitEvent(AccountEvent.ShowError(e.localizedMessage ?: "Restore error"))
                _viewState.value = _viewState.value.copy(isRestoreLoading = false)
            }
        }
    }

    /**
     * Fetch and log Firebase ID token to Logcat for debugging backend calls.
     * Token is short-lived (~1h); refreshes by using forceRefresh = true.
     */
    private fun logFirebaseIdToken() {
        val user = auth.currentUser ?: return
        user.getIdToken(true)
            .addOnSuccessListener { result ->
                Log.d("FirebaseIdToken", "ID token: ${result.token}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseIdToken", "Failed to get ID token: ${e.message}")
            }
    }
}
