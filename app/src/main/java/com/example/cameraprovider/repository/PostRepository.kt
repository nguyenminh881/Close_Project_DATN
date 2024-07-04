package com.example.cameraprovider.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.cameraprovider.model.Post
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.cameraprovider.*
import com.example.cameraprovider.model.Like
import com.example.cameraprovider.model.LikeStatus
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime


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
                    hiddenForUsers = emptyList(),
                    viewedBy = emptyList()
                )
                newPostRef.set(post).await()
                PostResult.Success(postId = "")
            } else {
                PostResult.Failure("User ID is null")
            }
        } catch (e: FirebaseNetworkException) {
            PostResult.Failure(e.message ?: "Kiểm tra kết nối mạng")
        }
    }

    fun iscurrentId(): String {
        return auth.currentUser!!.uid
    }

    //cau hinh paging3
    fun getPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10,
                initialLoadSize = 10,
                maxSize = 100,
                jumpThreshold = 10
            ),
            pagingSourceFactory = { PostPagingSource(fireStore, auth) }
        ).flow
    }


    private val likeMutex = Mutex()

    suspend fun updateLikePost(postId: String, icon: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        val fireStore = FirebaseFirestore.getInstance()
        val timeStamp = Timestamp(Date())

        likeMutex.withLock {
            try {
                fireStore.runTransaction { transaction ->
                    // Xem bản ghi "like" hiện có
                    val likeQuerySnapshot = Tasks.await(
                        fireStore.collection("likes")
                            .whereEqualTo("postId", postId)
                            .whereEqualTo("userId", userId)
                            .get()
                    )

                    if (likeQuerySnapshot.isEmpty) {
                        // Tạo bản ghi "like" mới nếu chưa tồn tại
                        val postDocument =
                            transaction.get(fireStore.collection("posts").document(postId))
                        val ownerId = postDocument.getString("userId") ?: ""
                        val newLike = Like(
                            postId = postId,
                            userId = userId,
                            ownerId = ownerId,
                            reactions = listOf(icon),
                            createdAt = timeStamp,
                            status = LikeStatus.NEW
                        )
                        transaction.set(fireStore.collection("likes").document(), newLike)
                    } else {
                        // Cập nhật bản ghi "like" hiện có
                        val likeDocumentSnapshot = likeQuerySnapshot.documents[0]
                        val existingLike = likeDocumentSnapshot.toObject(Like::class.java)

                        if (existingLike != null) {
                            val updatedReactions = if (existingLike.reactions.size >= 4) {
                                val updatedList = existingLike.reactions.toMutableList()
                                updateReactions(updatedList, icon)
                                updatedList
                            } else {
                                existingLike.reactions.toMutableList().apply { add(icon) }
                            }

                            transaction.update(
                                likeDocumentSnapshot.reference, mapOf(
                                    "reactions" to updatedReactions,
                                    "createdAt" to timeStamp
                                )
                            )
                        } else {
                            throw IllegalStateException("Lỗi")
                        }
                    }
                }.await() // Đây là nơi bạn chờ đợi giao dịch hoàn thành
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateReactions(reactions: MutableList<String>, newIcon: String) {
        if (reactions.size >= 4) {
            reactions.removeAt(0)
        }
        reactions.add(newIcon)
    }


    fun getlikepost(postId: String, listinfolike: (List<Pair<String, List<String>>>) -> Unit) {
        val likesRef = fireStore.collection("likes").whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        likesRef.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            val reactionList = mutableListOf<Pair<String, List<String>>>()

            querySnapshot?.documents?.forEach { likeDoc ->
                val like = likeDoc.toObject(Like::class.java)
                like?.let {
                    val userId = like.userId
                    //laya username
                    fireStore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            if (userSnapshot.exists()) {
                                val userName = userSnapshot.getString("nameUser") ?: ""
                                val reactions = like.reactions

                                // Tạo pair userName và reactions
                                val userReactions = Pair(userName, reactions)
                                reactionList.add(userReactions)

                                if (reactionList.size == querySnapshot.size()) {
                                    listinfolike(reactionList)
                                }
                            }
                        }.addOnFailureListener { exception ->
                            Log.e(
                                "PostRepository",
                                "Lỗi khi lấy like: ${exception.message}",
                                exception
                            )
                        }
                }
            }
        }
    }

    private val imageGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 0.8f
            maxOutputTokens = 120
            topK = 40
        }
    )

    val currentTime = LocalTime.now()
    val currentDate = LocalDate.now()
    val dayOfWeek = when (currentDate.dayOfWeek) {
        DayOfWeek.MONDAY -> "thứ Hai"
        DayOfWeek.TUESDAY -> "thứ Ba"
        DayOfWeek.WEDNESDAY -> "thứ Tư"
        DayOfWeek.THURSDAY -> "thứ Năm"
        DayOfWeek.FRIDAY -> "thứ Sáu"
        DayOfWeek.SATURDAY -> "thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ nhật"
    }
    val dayOfMonth = currentDate.dayOfMonth

    val month = when (currentDate.monthValue) {
        1 -> "Tháng Một"
        2 -> "Tháng Hai"
        3 -> "Tháng Ba"
        4 -> "Tháng Tư"
        5 -> "Tháng Năm"
        6 -> "Tháng Sáu"
        7 -> "Tháng Bảy"
        8 -> "Tháng Tám"
        9 -> "Tháng Chín"
        10 -> "Tháng Mười"
        11 -> "Tháng Mười Một"
        else -> "Tháng Mười Hai"
    }
    val year = currentDate.year
    val timeOfDay = when {
        currentTime.isAfter(LocalTime.of(4, 0)) && currentTime.isBefore(
            LocalTime.of(
                11,
                0
            )
        ) -> "buổi sáng thứ $dayOfWeek, ngày $dayOfMonth tháng $month"

        currentTime.isAfter(LocalTime.of(11, 0)) && currentTime.isBefore(
            LocalTime.of(
                13,
                30
            )
        ) -> "buổi trưa thứ $dayOfWeek, ngày $dayOfMonth tháng $month"

        currentTime.isAfter(LocalTime.of(13, 30)) && currentTime.isBefore(
            LocalTime.of(
                18,
                30
            )
        ) -> "buổi chiều thứ $dayOfWeek, ngày $dayOfMonth tháng $month"

        currentTime.isAfter(LocalTime.of(18, 30)) && currentTime.isBefore(
            LocalTime.of(
                23,
                59
            )
        ) -> "buổi tối thứ $dayOfWeek, ngày $dayOfMonth tháng $month"

        else -> "đêm khuya thứ $dayOfWeek, ngày $dayOfMonth tháng $month"
    }

    suspend fun generateContentFromImage(imageBytes: Bitmap): String {
        val inputContent = content {
            image(imageBytes)
            text(
                " Này AI bạn ơi, giúp mình viết caption thật deep cho bức ảnh này với! ✨ [Mô tả ngắn gọn nội dung chính của bức ảnh] " +
                        "Thể hiện tâm trạng của mình trong ảnh đi, kiểu như nỗi buồn man mác, sự cô đơn, hoặc những suy tư sâu lắng... nhưng mà pha thêm chút hài hước nhẹ nhàng, châm biếm cho nó thú vị nhé! \uD83D\uDE02 " +
                        "Viết bằng Tiếng Việt, ngôi thứ nhất, như mình đang tâm sự với bạn bè vậy." +
                        "Sử dụng ngôn ngữ hình ảnh, ẩn dụ, và thêm emoji ở mỗi câu phù hợp để caption thêm phần sâu sắc và sinh động. \uD83D\uDE09 " +
                        "Phải phù hợp với việc chia sẻ đến mọi người" +
                        " Đừng tạo hashtag, và chỉ dùng thông tin về thời gian ($timeOfDay) hoặc ngày tháng ($dayOfWeek, ngày $dayOfMonth tháng $month năm $year) khi thực sự cần thiết thôi nhé! \uD83D\uDC4C\n" +
                        "  Caption dưới 80 ký tự . \uD83D\uDE0E , ưu tiên ngắn nhé !"
            )
        }
        val response = imageGenerativeModel.generateContent(inputContent)
        return response.text.toString()
    }

    suspend fun generateContentFromText(prompt: String): String {
        val adddaucau = """
        Bạn là một trợ lý AI thông minh, có khả năng phân tích và thêm dấu câu vào đoạn văn để đảm bảo ngữ pháp chính xác và dễ đọc. Khi thêm dấu câu, hãy tuân theo các quy tắc sau:
        1. Sử dụng dấu chấm (.) để kết thúc một câu hoàn chỉnh. 
        2. Sử dụng dấu phẩy (,) để ngắt câu, tách các thành phần liệt kê hoặc bổ sung thông tin. 
        3. Sử dụng dấu chấm than (!) để thể hiện sự ngạc nhiên, cảm xúc mạnh. 
        4. Sử dụng dấu chấm hỏi (?) cho câu hỏi. 
        5. Đảm bảo rằng mỗi câu có cấu trúc ngữ pháp hoàn chỉnh với chủ ngữ và vị ngữ
     

        Sau khi thêm dấu câu, hãy phân tích đoạn văn để xác định xem nó có phải là bài hát, câu chuyện, thơ hay drama. Đối với bài hát, hãy chú ý đến nhịp điệu và cách sử dụng từ ngữ lặp lại.
        đây là đoạn văn $prompt
          hãy chỉ trả về đoạn văn đã được thêm dấu câu
          hãy chỉ trả về đoạn văn đã được thêm dấu câu. Nếu không thể phân tích được, hãy trả về nguyên văn bản gốc..
    """
        val analysisInputContent = content { text(adddaucau) }
        val analysisResponse = imageGenerativeModel.generateContent(analysisInputContent)
        val punctuatedPrompt = analysisResponse.text.toString().substringAfter("Đoạn văn đã được thêm dấu câu: ")

        val inputContent = content {
            text(
                """
        Viết caption cho đoạn ghi âm sau khi đã phân tích: "$punctuatedPrompt" (đã được thêm dấu câu).
        Đây là đoạn ghi âm [mô tả ngắn gọn về nội dung ghi âm, ví dụ: tôi hát một bài hát, kể một câu chuyện drama, hoặc nói về những chuyện xàm xí].
        Hãy phân tích nội dung ghi âm và viết caption phù hợp, cho biết đây là bài hát, câu chuyện drama, hay chuyện xàm xí.
        Caption cần mang phong cách giật gân, giật tít để thu hút người nghe, không cần thêm bất kỳ lời dẫn nào của bản thân AI.
        Dùng các từ ngữ xưng hô phù hợp với chia sẻ cho nhóm bạn bè tự nhiên nhất có thể
        Viết kiểu thân mật, hài hước, dưới 200 ký tự, ưu tiên tầm khoảng 100 ký tự.
        Thêm 1-2 emoji phù hợp vào mỗi câu và không sử dụng hashtag.
        Có thể thêm thông tin về thời gian ($timeOfDay) hoặc ngày tháng ($dayOfWeek, ngày $dayOfMonth tháng $month năm $year) nếu phù hợp, nhưng đừng lạm dụng.
        """
            )
        }
        val response = imageGenerativeModel.generateContent(inputContent)
        return response.text.toString()
    }


    sealed class PostResult {
        data class Success(val postId: String) : PostResult()
        data class Failure(val error: String) : PostResult()
    }


    fun observePost(postId: String) {
        val postRef = fireStore.collection("posts").document(postId)
        postRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val post = snapshot.toObject(Post::class.java)


            } else {
                Log.d("postrepo", "Current data: null")
            }
        }
    }


    suspend fun getLatestPost(): Post? {
        val snapshot = fireStore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return if (!snapshot.isEmpty) {
            snapshot.documents[0].toObject(Post::class.java)
        } else {
            null
        }
    }


    suspend fun deletePost(postId: String): Boolean {
        val postRef = fireStore.collection("posts").document(postId)
        val currentId = auth.currentUser?.uid

        return if (currentId != null) {
            try {
                val postSnapshot = postRef.get().await()
                if (postSnapshot.exists()) {
                    val post = postSnapshot.toObject(Post::class.java)
                    if (post != null) {
                        if (post.userId == currentId) {
                            val decodedImageUrl = URLDecoder.decode(post.imageURL, "UTF-8")
                            val decodedVoiceUrl = URLDecoder.decode(post.imageURL, "UTF-8")
                            when {
                                post.imageURL == "" && post.voiceURL != "" -> {
                                    val voiceRef = storage.getReferenceFromUrl(decodedVoiceUrl)
                                    Log.d("XOAVOICE", "delete URL obtained: $voiceRef")
                                    voiceRef.delete().await()
                                }

                                post.imageURL != "" && post.voiceURL == "" -> {
                                    val imageRef = storage.getReferenceFromUrl(decodedImageUrl)
                                    imageRef.delete().await()
                                }
                            }

                            //xoa like
                            val likesSnapshot = fireStore.collection("likes")
                                .whereEqualTo("postId", postId)
                                .get()
                                .await()
                            if (!likesSnapshot.isEmpty) {
                                likesSnapshot.documents.forEach { it.reference.delete().await() }
                            }

                            // xoa noi dung cmt
                            val messagesSnapshot = fireStore.collection("messages")
                                .whereEqualTo("postId", postId)
                                .get()
                                .await()
                            if (!messagesSnapshot.isEmpty) {
                                messagesSnapshot.documents.forEach {
                                    it.reference.update(
                                        mapOf(
                                            "postId" to "",
                                            "imageUrl" to "",
                                            "voiceUrl" to "",
                                            "timestamp" to "",
                                            "content" to "",
                                            "avtpost" to ""
                                        )
                                    ).await()
                                }
                            }


                            postRef.delete().await()
                            true
                        } else {
                            postRef.update("hiddenForUsers", FieldValue.arrayUnion(currentId))
                                .await()
                            true
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.d("PostRepository", "Error deleting post: ${e}")
                false
            }
        } else {
            false
        }
    }


    fun updateViewedBy(postId: String, onResult: (Boolean) -> Unit) {
        val postRef = fireStore.collection("posts").document(postId)
        val currentId = auth.currentUser?.uid
        postRef.update("viewedBy", FieldValue.arrayUnion(currentId))
            .addOnSuccessListener {
                Log.d("PostRepository", "Post $postId viewed by $currentId")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("PostRepository", "Error $postId add viewed", e)
                onResult(false)
            }
    }


    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun listenForNewPosts(onNewPostCount: (Int) -> Unit) {
        repositoryScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val friendIds = getFriendIds(currentUserId)

            if (friendIds.isNotEmpty()) {


                val postRef = fireStore.collection("posts")
                    .whereIn("userId", friendIds)
                    .orderBy("createdAt", Query.Direction.DESCENDING)

                postRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PostRepository", "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }


                    if (snapshot != null) {
                        val posts = snapshot.toObjects(Post::class.java)

                        val newPosts = posts.filter { !it.viewedBy.contains(currentUserId) }
                        onNewPostCount(newPosts.size)
                    }
                }
            } else {
                onNewPostCount(0)
                Log.d("PostRepository", "Không có bạn bè để truy vấn.")
            }
        }
    }

    private suspend fun getFriendIds(currentUserId: String): List<String> {
        return try {
            val query1 = fireStore.collection("friendships")
                .whereEqualTo("uid1", currentUserId)
                .whereEqualTo("state", "Accepted")

            val query2 = fireStore.collection("friendships")
                .whereEqualTo("uid2", currentUserId)
                .whereEqualTo("state", "Accepted")

            val combinedTask = Tasks.whenAllSuccess<QuerySnapshot>(query1.get(), query2.get())
                .await()

            val friendIds = mutableSetOf<String>()
            for (result in combinedTask) {
                result.documents.forEach { doc ->
                    val uid1 = doc.getString("uid1")
                    val uid2 = doc.getString("uid2")
                    if (uid1 != currentUserId && uid1 != null) {
                        friendIds.add(uid1)
                    }
                    if (uid2 != currentUserId && uid2 != null) {
                        friendIds.add(uid2)
                    }
                }
            }
            friendIds.toList()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching friend IDs", e)
            emptyList()
        }
    }

}