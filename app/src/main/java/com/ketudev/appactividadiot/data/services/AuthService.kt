package com.ketudev.appactividadiot.data.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.ketudev.appactividadiot.models.User
import kotlinx.coroutines.tasks.await

class AuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val lucesService = LucesService()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    suspend fun createAccount(email: String, password: String, displayName: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user
        user?.updateProfile(
            userProfileChangeRequest { this.displayName = displayName }
        )?.await()
        if (user != null) {
            try {
                lucesService.saveUser(User(id = user.uid, nombre = displayName, email = email))
            } catch (e: Exception) {
                // Do not block authentication success if Firestore save encounters issue
                e.printStackTrace()
            }
        }
        return user
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user
        if (user != null) {
            try {
                lucesService.saveUser(
                    User(
                        id = user.uid,
                        nombre = user.displayName ?: "",
                        email = user.email ?: ""
                    )
                )
            } catch (e: Exception) {
                // Do not block authentication success if Firestore save encounters issue
                e.printStackTrace()
            }
        }
        return user
    }

    fun signOut() {
        auth.signOut()
    }
}
