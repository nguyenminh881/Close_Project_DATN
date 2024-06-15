package com.example.cameraprovider.repository

import android.net.Uri
import android.util.Log
import com.example.cameraprovider.model.Post
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.core.OrderBy
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.cameraprovider.model.Like
import kotlinx.coroutines.flow.Flow


class PostRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

//add pót
    suspend fun addPost(contentUri: Uri, content: String, isImage: Boolean): PostResult {
        return try {
            val uniqueID = UUID.randomUUID().toString()
            val fileName = "post_${auth.currentUser!!.uid}$uniqueID"

            val storageReference = if (isImage) {
                storage.reference.child("${auth.currentUser!!.uid}/post_image/$fileName.jpeg")
            } else {
                storage.reference.child("${auth.currentUser!!.uid}/post_voice/$fileName.aac")
            }
            val uploadTask = storageReference.putFile(contentUri).await()
            val downloadUrl = storageReference.downloadUrl.await()
            Log.d("TAGY", "Download URL obtained: $downloadUrl")
            val currentUID = auth.currentUser?.uid
            val timeStamp = Timestamp(Date())
            if (currentUID != null) {
                val docRef = fireStore.collection("users").document(currentUID).get().await()
                val currenName = docRef.getString("nameUser")
                val currentAvt = docRef.getString("avatarUser")
                val newPostRef = fireStore.collection("posts").document()
                val postId = newPostRef.id
                val post = Post(
                    postId = postId,
                    userId = currentUID,
                    userName = currenName,
                    userAvatar = currentAvt,
                    content = content,
                    imageURL = if (isImage) downloadUrl.toString() else "",
                    voiceURL = if (!isImage) downloadUrl.toString() else "",
                    createdAt = timeStamp,
                    likes = mutableListOf()
                )
                newPostRef.set(post).await()
                PostResult.Success(postId = "")
            } else {
                PostResult.Failure("User ID is null")
            }
        } catch (e: FirebaseNetworkException) {
            PostResult.Failure(e.message ?: "Unknown error")
        }
    }


//cau hinh paging3
    fun getPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10,
                initialLoadSize =  10,
                maxSize = 100,
                jumpThreshold = 10
            ),
            pagingSourceFactory = { PostPagingSource(fireStore, auth) }
        ).flow
    }

    suspend fun updateLikePost(postId: String, icon: String) {
        try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: return
            val postRef = fireStore.collection("posts").document(postId)

            fireStore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val post = snapshot.toObject(Post::class.java)// chuyen doi object sang post

                if (post != null) {
                    // tìm xem thang nay no like bai viet chua
                    var userLike = post.likes.find { it.userId == userId }
                    if (userLike == null) {
                        userLike = Like(userId = userId, reactions = mutableListOf(icon))
                        post.likes.add(userLike)
                    } else {
                        // Rotate reactions if there are already 4 reactions
                        if (userLike.reactions.size >= 4) {
                            rotateReactions(userLike.reactions, icon)
                        } else {
                            // Add icon to reactions if not already present
                            if (!userLike.reactions.contains(icon)) {
                                userLike.reactions.add(icon)
                            }
                        }
                    }

                    // Update the likes field in Firestore
                    transaction.set(postRef, post)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateReactions(reactions: MutableList<String>, newReaction: String) {
        // Remove the first reaction
        reactions.removeAt(0)
        // Add the new reaction to the end
        reactions.add(newReaction)
    }



    sealed class PostResult {
        data class Success(val postId: String) : PostResult()
        data class Failure(val error: String) : PostResult()
    }
}