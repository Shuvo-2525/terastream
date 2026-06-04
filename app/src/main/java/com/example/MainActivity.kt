package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.TeraRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SelectionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.immersiveGlowBackground
import com.example.ui.viewmodel.TeraViewModel
import com.example.ui.viewmodel.TeraViewModelFactory
import com.example.ui.viewmodel.TeraUiState

class MainActivity : ComponentActivity() {

    private lateinit var model: TeraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { TeraRepository(database.recentLinkDao()) }
            val viewModel: TeraViewModel = viewModel(
                factory = TeraViewModelFactory(repository)
            )
            
            // Assign instance reference to handle lifecycle intent changes
            model = viewModel

            // Handle any pre-existing inbound share intents
            LaunchedEffect(intent) {
                handleInboundIntent(intent)
            }

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    val uiState by viewModel.uiState.collectAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (val state = uiState) {
                            is TeraUiState.Idle -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onPasteAction = { viewModel.onUrlInputChange(it) }
                                )
                            }
                            is TeraUiState.Loading -> {
                                LoadingScreen()
                            }
                            is TeraUiState.ListReady -> {
                                SelectionScreen(
                                    viewModel = viewModel,
                                    folderTitle = state.title,
                                    items = state.items,
                                    originalShareUrl = viewModel.urlInput.value,
                                    onBack = { viewModel.resetToIdle() }
                                )
                            }
                            is TeraUiState.StreamReady -> {
                                PlayerScreen(
                                    viewModel = viewModel,
                                    name = state.name,
                                    url = state.url,
                                    headers = state.headers,
                                    originalShareUrl = state.originalShareUrl,
                                    fileId = state.fileId,
                                    onBack = { viewModel.goBackFromPlayer() }
                                )
                            }
                            is TeraUiState.Error -> {
                                ErrorScreen(
                                    viewModel = viewModel,
                                    code = state.code,
                                    message = state.message,
                                    urlAttempted = state.urlAttempted,
                                    onRetry = { password ->
                                        viewModel.resolveLink(state.urlAttempted, password)
                                    },
                                    onBack = { viewModel.resetToIdle() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::model.isInitialized) {
            handleInboundIntent(intent)
        }
    }

    private fun handleInboundIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && "text/plain" == type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                model.onUrlInputChange(sharedText)
                model.resolveLink(sharedText)
            }
        } else if (Intent.ACTION_VIEW == action) {
            val dataString = intent.dataString
            if (!dataString.isNullOrBlank()) {
                model.onUrlInputChange(dataString)
                model.resolveLink(dataString)
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .immersiveGlowBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("resolving_progress_indicator"),
                strokeWidth = 4.dp
            )
            Text(
                text = "Resolving link…",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scraping and analyzing stream servers. Please wait.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

@Composable
fun ErrorScreen(
    viewModel: TeraViewModel,
    code: String,
    message: String,
    urlAttempted: String,
    onRetry: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPasswordRequired = code == "PASSWORD_REQUIRED"
    var passwordValue by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .immersiveGlowBackground()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (isPasswordRequired) Icons.Default.Lock else Icons.Default.ErrorOutline,
                    contentDescription = "Status Icon",
                    tint = if (isPasswordRequired) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = if (isPasswordRequired) "Password Required" else "Resolution Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                if (isPasswordRequired) {
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { passwordValue = it },
                        placeholder = { Text("Enter folder password", fontSize = 13.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Button(
                        onClick = { onRetry(passwordValue) },
                        enabled = passwordValue.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("password_submit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                            Text("Unlock & Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { onRetry(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("retry_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                            Text("Try Again", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray),
                    modifier = Modifier.testTag("error_back_button")
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                        Text("Back to Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
