package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.manager.TabLifecycleManager
import com.example.model.ThreadCategory
import com.example.model.ThreadItem

val sampleThreads = listOf(
    ThreadItem("1", "Claude — Work", ThreadCategory.CHAT, "https://claude.ai"),
    ThreadItem("2", "Claude — Personal", ThreadCategory.CHAT, "https://claude.ai"),
    ThreadItem("3", "Gemini", ThreadCategory.CHAT, "https://gemini.google.com"),
    ThreadItem("4", "Google AI Studio", ThreadCategory.PAGE, "https://aistudio.google.com"),
    ThreadItem("5", "GitHub", ThreadCategory.PAGE, "https://github.com"),
    ThreadItem("6", "Omega Local AI", ThreadCategory.LOCAL)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabContent(
    onThreadSelected: (ThreadItem) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(ThreadCategory.ALL) }

    val filteredThreads = remember(selectedCategory) {
        if (selectedCategory == ThreadCategory.ALL) {
            sampleThreads
        } else {
            sampleThreads.filter { it.category == selectedCategory }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pill filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThreadCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        // Thread List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredThreads, key = { it.id }) { thread ->
                ThreadListItem(
                    thread = thread,
                    onClick = {
                        TabLifecycleManager.openTab(thread)
                        onThreadSelected(thread)
                    },
                    onLongClick = {
                        val added = TabLifecycleManager.openTab(thread)
                        val msg = if (added) "Opened '${thread.title}' in new tab" else "'${thread.title}' is already in tabs"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadListItem(
    thread: ThreadItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val icon = when (thread.category) {
        ThreadCategory.CHAT -> Icons.Default.Chat
        ThreadCategory.PAGE -> Icons.Default.Info
        ThreadCategory.LOCAL -> Icons.Default.Face
        ThreadCategory.ALL -> Icons.Default.List
    }

    val isOpen = TabLifecycleManager.openTabs.any { it.id == thread.id }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isOpen) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "[${thread.category.name.lowercase()}] ${thread.url ?: "local on-device"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isOpen) {
                val isLive = TabLifecycleManager.openTabs.find { it.id == thread.id }?.isLive == true
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isLive) Color.Green else Color.Gray)
                )
            }
        }
    }
}
