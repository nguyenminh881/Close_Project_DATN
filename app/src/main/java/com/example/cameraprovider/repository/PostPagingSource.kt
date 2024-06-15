package com.example.cameraprovider.repository
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.cameraprovider.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
class PostPagingSource(
    private val fireStore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PagingSource<QuerySnapshot, Post>() {


    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {

        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return LoadResult.Error(Exception("Current user is null"))
            }

            val userId = currentUser.uid
            val currentPage = params.key ?: fireStore.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
                .get()
                .await()



            Log.d("PostPagingSource", "Loaded page: ${currentPage.documents.map { it.id }}")

            val lastVisiblePost = currentPage.documents.lastOrNull()
            val nextPage = lastVisiblePost?.let {
                fireStore.collection("posts")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .startAfter(it)
                    .limit(params.loadSize.toLong())
                    .get()
                    .await()
            }
            LoadResult.Page(
                data = currentPage.toObjects(Post::class.java),
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    override val jumpingSupported: Boolean
        get() = true
    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}