package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WaterMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WaterPrimary
import com.example.ui.viewmodel.WaterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val errorState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup global Exception Handler to catch any background or main thread crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()
            Log.e(TAG, "Uncaught Exception: $exceptionAsString", throwable)
            
            if (thread.name == "main") {
                // Main thread crashes cannot be recovered by updating Compose state
                // because the main Looper exits. Hand over to the system handler to avoid UI lockups.
                defaultHandler?.uncaughtException(thread, throwable)
            } else {
                runOnUiThread {
                    errorState.value = exceptionAsString
                }
            }
        }

        enableEdgeToEdge()
        
        // Request POST_NOTIFICATIONS on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1010)
            }
        }

        setContent {
            MyApplicationTheme {
                val currentError by errorState

                if (currentError != null) {
                    // Elegant Crash Fallback Screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "عذراً، حدث خطأ غير متوقع! ⚠️",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WaterPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "مستوى الخطأ التقني للتطبيق:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stacktrace container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = currentError ?: "",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    // Try resetting database as fallback
                                    try {
                                        val db = com.example.data.database.DatabaseProvider.getDatabase(applicationContext)
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                db.clearAllTables()
                                            }
                                            errorState.value = null
                                            recreate()
                                        }
                                    } catch (e: Exception) {
                                        errorState.value = null
                                        recreate()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WaterPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("إعادة تهيئة التطبيق ومحاولة التشغيل 🔄", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val viewModel: WaterViewModel = viewModel()
                        WaterMainScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
