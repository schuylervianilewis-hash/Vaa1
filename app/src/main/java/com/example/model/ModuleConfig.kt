package com.example.model

data class ElementSelectors(
    val conversationContainer: String = "",
    val inputBox: String = ""
)

data class ModuleConfig(
    val id: String,
    val name: String,
    val url: String,
    val threadType: String = "chat",
    val selectors: ElementSelectors = ElementSelectors(),
    val sendMethod: String = "enter_key",
    val loginCheck: String = "",
    val isGoogleOnlyAuth: Boolean = false
)
