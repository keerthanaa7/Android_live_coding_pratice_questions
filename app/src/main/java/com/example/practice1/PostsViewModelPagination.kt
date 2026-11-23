package com.example.practice1

import PostItem
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PostUIStatePagination{
    data object Loading: PostUIStatePagination
    data class Success(val posts: List<PostItem>,
        val isPaging: Boolean = false, val hasmorepage: Boolean = true): PostUIStatePagination
    data class Error(val message: String): PostUIStatePagination
}

class PostsViewModelPagination(private val postRepository: PostRepository) : ViewModel(){

    private val uiStateMutable = MutableStateFlow<PostUIStatePagination>(PostUIStatePagination.Loading)
    val uistate = uiStateMutable.asStateFlow()
    var postslist = listOf<PostItem>()
    var displayedcount = 0
    val minpagesize = 25

    init {
        fetchPosts()
    }
     fun fetchPosts(){
        viewModelScope.launch {
            uiStateMutable.value = PostUIStatePagination.Loading
            try {
                 postslist = postRepository.getPosts()
                Log.d("PostsViewModel", "post list ${postslist.size}")
                if(postslist.isEmpty()){
                    uiStateMutable.value = PostUIStatePagination.Error("post list is empty")
                }else{
                     displayedcount = Math.min(minpagesize, postslist.size)
                    val initialchunk = postslist.subList(0, displayedcount)
                    uiStateMutable.value = PostUIStatePagination.Success(initialchunk, hasmorepage =
                    displayedcount < postslist.size)
                }
            }catch (e: Exception){
                uiStateMutable.value = PostUIStatePagination.Error("${e}")
            }
        }
    }

    fun loadNextPage(){
        val currenstate = (uiStateMutable.value as? PostUIStatePagination.Success)?:return
        if(currenstate.isPaging || !currenstate.hasmorepage){
            return
        }
        viewModelScope.launch {
            uiStateMutable.update {
                currentstate ->
                if(currentstate is PostUIStatePagination.Success){
                    currentstate.copy(isPaging = true)
                }else{
                    currentstate
                }
            }

            delay(500)
            val nextcount = Math.min(displayedcount + minpagesize, postslist.size)
            val nextchunk = postslist.subList(displayedcount, nextcount)
            displayedcount = nextcount
            val currentSuccess = uiStateMutable.value as? PostUIStatePagination.Success
            val existingPosts = currentSuccess?.posts ?: emptyList()

            uiStateMutable.value = PostUIStatePagination.Success(
                posts = existingPosts + nextchunk,
                isPaging = false,
                hasmorepage = displayedcount < postslist.size
            )
        }
    }
}