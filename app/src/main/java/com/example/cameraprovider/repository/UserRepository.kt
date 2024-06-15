package com.example.cameraprovider.repository

import android.net.Uri
import android.util.Log
import com.example.cameraprovider.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    suspend fun signUp(email: String, password: String): Result<Boolean> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()

            val userId = auth.currentUser?.uid
            if (userId != null) {
                val user = User(
                    UserId = userId,
                    phoneNumber = "",
                    emailUser = email,
                    avatarUser = "",
                    nameUser = "",
                    passwordUser = "",
                    friendUser = mutableListOf()
                )
                fireStore.collection("users").document(userId).set(user).await()
                Result.success(true)
            } else {
                Result.failure(Exception("User ID is null"))
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAvtandNameUser(imageUri: Uri, name: String): Result<Boolean> {
        return try {
            val uniqueID = UUID.randomUUID().toString()
            val fileName = "avt_${auth.currentUser!!.uid}_$uniqueID"
            val storageReference =
                storage.reference.child("${auth.currentUser!!.uid}/avatar/$fileName")
            val uploadTask = storageReference.putFile(imageUri).await()
            val downloadUrl = storageReference.downloadUrl.await()

            val userId = auth.currentUser?.uid
            if (userId != null) {
                val updates = mapOf(
                    "avatarUser" to downloadUrl.toString(),
                    "nameUser" to name
                )
                fireStore.collection("users").document(userId).update(updates).await()
                Result.success(true)
            } else {
                Result.failure(Exception("User ID is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Mật khẩu không đúng
            Result.failure(Exception("Tài khoản hoặc mật khẩu không chính xác"))
        } catch (e: FirebaseAuthInvalidUserException) {
            // Không tìm thấy người dùng với email này
            Result.failure(Exception("Tài khoản không tồn tại"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Lỗi mạng khi đăng nhập"))
        } catch (e: FirebaseTooManyRequestsException) {
            // Tài khoản bị vô hiệu hóa do nhiều lần thử đăng nhập không thành công
            Result.failure(Exception("Tài khoản đã bị vô hiệu hóa do nhiều lần đăng nhập không thành công"))
        } catch (e: Exception) {
            // Lỗi chung
            Result.failure(Exception("Vui lòng nhập đầy đủ tài khoản và mật khẩu"))
        }
    }

     fun isUserLoggedIn(): Boolean {
        val current = auth.currentUser
         return current!=null
     }

    private var isDataLoaded = false
    private var cachedUser: User? = null
    suspend fun getInfoUser(): Result<User> {
        if (isDataLoaded) {
            return cachedUser?.let { Result.success(it) } ?: Result.failure(Exception("No cached data"))
        }
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid

                val userDocumentRef = fireStore.collection("users").document(userId)
                val userSnapshot = userDocumentRef.get().await()

                userDocumentRef.addSnapshotListener { documentSnapshot, e ->
                    if (e != null) {
                        // Xử lý lỗi
                        return@addSnapshotListener
                    }
                }
                Log.d("TAGY", "tt: $userSnapshot")
                if (userSnapshot.exists()) {
                    val user = userSnapshot.toObject(User::class.java)
                    isDataLoaded = true
                    cachedUser = user
                    Log.d("TAGY", "User data: $user")
                    if (user != null) {
                        Result.success(user)
                    } else {
                        Result.failure(Exception("loi k ep sang user"))
                    }
                } else {
                    Result.failure(Exception("User document does not exist"))
                }
            } else {
                Log.e("getInfoUser", "Current user is null")
                Result.failure(Exception("Current user is null"))
            }
        } catch (e: Exception) {
            Log.e("getInfoUser", "Exception: ${e.message}")
            Result.failure(e)
        }
    }


    fun logout() {
        auth.signOut()
    }
}