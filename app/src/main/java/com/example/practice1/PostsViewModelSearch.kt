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

sealed interface PostUIStateSearch{
    data object Loading: PostUIStateSearch
    data class Success(val posts: List<PostItem>): PostUIStateSearch
    data class Error(val message: String): PostUIStateSearch
}

class PostsViewModelSearch(private val postRepository: PostRepository) : ViewModel(){

    private val uiStateMutable = MutableStateFlow<PostUIStateSearch>(PostUIStateSearch.Loading)

    private val searchQuerymutable = MutableStateFlow("")
    val searchQuery = searchQuerymutable.asStateFlow()

    val uistate = combine(uiStateMutable, searchQuerymutable){ state, query ->
        if(state is PostUIStateSearch.Success){
            if(query.isBlank()){
                state
            }else{
                val filteredposts = state.posts.filter {post -> post.title.contains(query) || post.body.contains(query) }
                PostUIStateSearch.Success(filteredposts)
            }
        }else{
            state
        }
    }.debounce(300).distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        PostUIStateSearch.Loading)

    init {
        fetchPosts()
    }
     fun fetchPosts(){
        viewModelScope.launch {
            uiStateMutable.value = PostUIStateSearch.Loading
            try {
                val postslist = postRepository.getPosts()
                Log.d("PostsViewModel", "post list ${postslist.size}")
                if(postslist.isEmpty()){
                    uiStateMutable.value = PostUIStateSearch.Error("post list is empty")
                }else{
                    uiStateMutable.value = PostUIStateSearch.Success(postslist)
                }
            }catch (e: Exception){
                uiStateMutable.value = PostUIStateSearch.Error("${e}")
            }
        }
    }

    fun onQueryChanged(newQuery: String){
        searchQuerymutable.value = newQuery
    }
}