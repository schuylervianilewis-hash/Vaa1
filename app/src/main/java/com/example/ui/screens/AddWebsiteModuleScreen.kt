package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ElementSelectors
import com.example.model.ModuleConfig
import com.example.repository.ModuleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebsiteModuleScreen(
    onBack: () -> Unit,
    onModuleSaved: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: URL input, 2: Login & Inspect, 3: Selectors & Save

    var moduleName by remember { mutableStateOf("Claude") }
    var targetUrl by remember { mutableStateOf("https://claude.ai/new") }
    var inputBoxSelector by remember { mutableStateOf("[data-testid='composer-input']") }
    var containerSelector by remember { mutableStateOf("[data-testid='conversation']") }
    var isGoogleOnlyAuth by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add via Website (Step $step of 3)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (step) {
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Module Configuration", style = MaterialTheme.typography.titleLarge)
                        
                        OutlinedTextField(
                            value = moduleName,
                            onValueChange = { moduleName = it },
                            label = { Text("Module Name (e.g., Claude — Work)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = targetUrl,
                            onValueChange = { targetUrl = it },
                            label = { Text("Target Site URL") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(
                                    "Google OAuth Limit: Sites whose ONLY login option is 'Sign in with Google' cannot be added as scraped modules due to WebView OAuth restrictions. Use email/password or magic links.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (moduleName.isNotBlank() && targetUrl.isNotBlank()) {
                                    step = 2
                                } else {
                                    Toast.makeText(context, "Please fill in Name and URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next: Login on Site")
                        }
                    }
                }

                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Log in on the site below. Passwords/autofill prompts are explicitly disabled for privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            BoundedLoginWebView(url = targetUrl)
                        }

                        Button(
                            onClick = { step = 3 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Next: Configure Selectors")
                        }
                    }
                }

                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Configure Element Selectors", style = MaterialTheme.typography.titleLarge)

                        OutlinedTextField(
                            value = inputBoxSelector,
                            onValueChange = { inputBoxSelector = it },
                            label = { Text("Input Box CSS Selector") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = containerSelector,
                            onValueChange = { containerSelector = it },
                            label = { Text("Conversation Container CSS Selector") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isGoogleOnlyAuth,
                                onCheckedChange = { isGoogleOnlyAuth = it }
                            )
                            Text("Flag as Google-only Auth limited", style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = {
                                val config = ModuleConfig(
                                    id = moduleName.lowercase().replace(" ", "_"),
                                    name = moduleName,
                                    url = targetUrl,
                                    selectors = ElementSelectors(
                                        conversationContainer = containerSelector,
                                        inputBox = inputBoxSelector
                                    ),
                                    isGoogleOnlyAuth = isGoogleOnlyAuth
                                )
                                val saved = ModuleRepository.saveModule(context, config)
                                if (saved) {
                                    Toast.makeText(context, "Module saved successfully!", Toast.LENGTH_SHORT).show()
                                    onModuleSaved()
                                } else {
                                    Toast.makeText(context, "Error saving module", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Module")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BoundedLoginWebView(url: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Explicitly disable WebView autofill/save password prompts per Section 12.11
                settings.saveFormData = false
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
