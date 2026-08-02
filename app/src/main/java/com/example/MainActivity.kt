package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.logging.LogKeeper
import com.example.manager.CacheManager
import com.example.manager.TabLifecycleManager
import com.example.model.ThreadCategory
import com.example.model.ThreadItem
import com.example.ui.screens.ChatsTabContent
import com.example.ui.screens.LogViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Global default uncaught exception logger
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      LogKeeper.log(applicationContext, "CRASH", "Uncaught exception in thread ${thread.name}", throwable)
      defaultHandler?.uncaughtException(thread, throwable)
    }

    LogKeeper.log(applicationContext, "APP_START", "Vaa Application onCreate initialized")

    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }

  override fun onDestroy() {
    CacheManager.performCacheCleanupIfEnabled(applicationContext)
    super.onDestroy()
  }
}

@Composable
fun AppNavigation() {
  val context = LocalContext.current
  val prefs = context.getSharedPreferences("vaa_prefs", Context.MODE_PRIVATE)
  val isFirstLaunch = prefs.getBoolean("first_launch_complete", false).not()
  
  val navController = rememberNavController()
  
  NavHost(
    navController = navController, 
    startDestination = if (isFirstLaunch) "welcome" else "main"
  ) {
    composable("welcome") {
      WelcomeScreen(onGetStarted = {
        prefs.edit().putBoolean("first_launch_complete", true).apply()
        LogKeeper.log(context, "WELCOME", "First launch completed")
        navController.navigate("main") {
          popUpTo("welcome") { inclusive = true }
        }
      })
    }
    composable("main") {
      MainShell(
        onOpenSettings = { navController.navigate("settings") },
        onOpenLogs = { navController.navigate("logs") }
      )
    }
    composable("settings") {
      SettingsScreen(
        onBack = { navController.popBackStack() },
        onOpenLogs = { navController.navigate("logs") }
      )
    }
    composable("logs") {
      LogViewerScreen(
        onBack = { navController.popBackStack() }
      )
    }
  }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Vaa",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp)
      )
      Text(
        text = "One place for your AI chats, Google AI Studio, and GitHub.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 32.dp)
      )
      Button(onClick = onGetStarted) {
        Text("Get Started")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
  onOpenSettings: () -> Unit,
  onOpenLogs: () -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("vaa_prefs", Context.MODE_PRIVATE) }
  var showLogFab by remember { mutableStateOf(prefs.getBoolean("log_keeper_fab_enabled", true)) }

  DisposableEffect(Unit) {
    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
      if (key == "log_keeper_fab_enabled") {
        showLogFab = p.getBoolean("log_keeper_fab_enabled", true)
      }
    }
    prefs.registerOnSharedPreferenceChangeListener(listener)
    onDispose {
      prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }

  val pagerState = rememberPagerState(pageCount = { 4 })
  val coroutineScope = rememberCoroutineScope()
  var showMenu by remember { mutableStateOf(false) }

  val tabs = listOf("Chats", "Updates", "Loader", "Tools")
  val icons = listOf(Icons.Default.Chat, Icons.Default.Notifications, Icons.Default.Refresh, Icons.Default.Build)
  
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Vaa") }
      )
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
      ) {
        NavigationBar(
          windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
          tabs.forEachIndexed { index, title ->
            NavigationBarItem(
              selected = pagerState.currentPage == index,
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(index)
                }
              },
              icon = { Icon(icons[index], contentDescription = title) },
              label = { Text(title) }
            )
          }
        }
        
        // True bottom-most tab strip for open threads
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Scrollable list of open thread icons
          Row(
            modifier = Modifier
              .weight(1f)
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (TabLifecycleManager.openTabs.isEmpty()) {
              Text(
                text = "No open tabs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              TabLifecycleManager.openTabs.forEach { tab ->
                val icon = when (tab.category) {
                  ThreadCategory.CHAT -> Icons.Default.Chat
                  ThreadCategory.PAGE -> Icons.Default.Info
                  ThreadCategory.LOCAL -> Icons.Default.Face
                  ThreadCategory.ALL -> Icons.Default.List
                }
                
                Surface(
                  shape = CircleShape,
                  color = if (tab.isLive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                  modifier = Modifier
                    .size(36.dp)
                    .clickable {
                      Toast.makeText(context, "Switching to tab: ${tab.title} (${if (tab.isLive) "live" else "sleeping"})", Toast.LENGTH_SHORT).show()
                    }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = icon,
                      contentDescription = tab.title,
                      modifier = Modifier.size(20.dp),
                      tint = if (tab.isLive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          IconButton(onClick = {
            Toast.makeText(context, "Add new thread tab", Toast.LENGTH_SHORT).show()
          }) {
            Icon(Icons.Default.Add, contentDescription = "Add Tab")
          }

          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "More Options")
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                  showMenu = false
                  onOpenSettings()
                }
              )
              DropdownMenuItem(
                text = { Text("Add New") },
                onClick = {
                  showMenu = false
                  LogKeeper.log(context, "UI_ACTION", "Add New tapped")
                }
              )
              DropdownMenuItem(
                text = { Text("All Threads") },
                onClick = {
                  showMenu = false
                  LogKeeper.log(context, "UI_ACTION", "All Threads tapped")
                }
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { page ->
        when (page) {
          0 -> ChatsTabContent(onThreadSelected = { thread ->
            LogKeeper.log(context, "THREAD_SELECT", "Selected thread: ${thread.title}")
          })
          1 -> EmptyTabContent("Updates")
          2 -> EmptyTabContent("Loader")
          3 -> EmptyTabContent("Tools & Skills")
        }
      }
      
      // Floating Action Buttons overlaid on top of content
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Log Keeper shortcut - left, alone (conditionally visible)
        if (showLogFab) {
          FloatingActionButton(
            onClick = { onOpenLogs() },
            modifier = Modifier.align(Alignment.BottomStart)
          ) {
            Icon(Icons.Default.List, contentDescription = "Log Keeper")
          }
        }
        
        // Stacked right FABs
        Column(
          modifier = Modifier.align(Alignment.BottomEnd),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.End
        ) {
          // Omega shortcut - top
          FloatingActionButton(onClick = { LogKeeper.log(context, "UI_ACTION", "Omega FAB tapped") }) {
            Icon(Icons.Default.Face, contentDescription = "Omega")
          }
          // Contextual action - below Omega
          FloatingActionButton(onClick = { LogKeeper.log(context, "UI_ACTION", "Contextual FAB tapped") }) {
            Icon(Icons.Default.Add, contentDescription = "Action")
          }
        }
      }
    }
  }
}

@Composable
fun EmptyTabContent(title: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(text = "$title (Static Placeholder)", style = MaterialTheme.typography.headlineMedium)
  }
}
