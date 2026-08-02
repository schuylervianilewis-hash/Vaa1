package com.example.model

enum class ThreadCategory {
    ALL, CHAT, PAGE, LOCAL
}

data class ThreadItem(
    val id: String,
    val title: String,
    val category: ThreadCategory,
    val url: String? = null,
    val isLive: Boolean = false,
    val lastConversationSnippet: String? = null
)
