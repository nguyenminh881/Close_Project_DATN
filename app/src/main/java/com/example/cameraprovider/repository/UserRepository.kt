package com.example.cameraprovider.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val auth:FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore:FirebaseFirestore = FirebaseFirestore.getInstance()

    fun signUp(email: String, password: String, callback: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }
//
//    suspend fun login(email: String, password: String): Result<Unit> {
//        return withContext(Dispatchers.IO) {
//            try {
//                auth.signInWithEmailAndPassword(email, password).await()
//                Result.success(Unit)
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//        }
//    }
//
//    suspend fun getCurrentUser(): Result<User?> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val userId = auth.currentUser?.uid
//                if (userId != null) {
//                    val document = fireStore.collection("users").document(userId).get().await()
//                    val user = document.toObject(User::class.java)
//                    Result.success(user)
//                } else {
//                    Result.success(null)
//                }
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//        }
//    }
//
//    fun getCurrentUserId(): String? {
//        return auth.currentUser?.uid
//    }
//
//    fun logout(): Result<Unit> {
//        return try {
//            auth.signOut()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
}