package com.example.practice1

import PostItem
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PostUIState{
    data object Loading: PostUIState
    data class Success(val posts: List<PostItem>): PostUIState
    data class Error(val message: String): PostUIState
}

class PostsViewModel(private val postRepository: PostRepository) : ViewModel(){

    private val uiStateMutable = MutableStateFlow<PostUIState>(PostUIState.Loading)
    val uistate = uiStateMutable.asStateFlow()

    init {
        fetchPosts()
    }
     fun fetchPosts(){
        viewModelScope.launch {
            uiStateMutable.value = PostUIState.Loading
            try {
                val postslist = postRepository.getPosts()
                Log.d("PostsViewModel", "post list ${postslist.size}")
                if(postslist.isEmpty()){
                    uiStateMutable.value = PostUIState.Error("post list is empty")
                }else{
                    uiStateMutable.value = PostUIState.Success(postslist)
                }
            }catch (e: Exception){
                uiStateMutable.value = PostUIState.Error("${e}")
            }
        }
    }
}