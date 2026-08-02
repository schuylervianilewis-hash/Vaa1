package com.example.manager

import androidx.compose.runtime.mutableStateListOf
import com.example.model.ThreadCategory
import com.example.model.ThreadItem

object TabLifecycleManager {
    const val MAX_ALIVE_TABS = 3 // Cap for 3GB RAM low-end hardware

    val openTabs = mutableStateListOf<ThreadItem>()

    fun openTab(thread: ThreadItem): Boolean {
        if (openTabs.any { it.id == thread.id }) {
            return false // Already open
        }
        val liveThread = thread.copy(isLive = true)
        openTabs.add(liveThread)
        enforceAliveCap()
        return true
    }

    fun closeTab(threadId: String) {
        openTabs.removeAll { it.id == threadId }
    }

    private fun enforceAliveCap() {
        if (openTabs.size > MAX_ALIVE_TABS) {
            // Keep the last N tabs live, mark older tabs as sleeping
            val sleepIndex = openTabs.size - MAX_ALIVE_TABS
            for (i in 0 until sleepIndex) {
                val sleeping = openTabs[i].copy(isLive = false)
                openTabs[i] = sleeping
            }
        }
    }
}
