package com.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SignatureViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // State to hold any caught exception so it can be rendered as a warning screen
    var crashError by mutableStateOf<Throwable?>(null)

    // Set a global thread handler to catch asynchronous background crashes
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e("MainActivityCrash", "Uncaught exception caught on thread ${thread.name}", throwable)
      Handler(Looper.getMainLooper()).post {
        crashError = throwable
      }
    }

    setContent {
      MyApplicationTheme {
        val error = crashError
        if (error != null) {
          CrashErrorScreen(throwable = error) {
            crashError = null
          }
        } else {
          val viewModel: SignatureViewModel = viewModel()
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            DashboardScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}

@Composable
fun CrashErrorScreen(throwable: Throwable, onReset: () -> Unit) {
  val sw = StringWriter()
  throwable.printStackTrace(PrintWriter(sw))
  val stackTraceString = sw.toString()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF151515))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      Text(
        text = "️⚠️ SYSTEM DIAGNOSTIC ERROR",
        color = Color(0xFFEF4444),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 0.5.sp
        )
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "A localized runtime exception was caught. The execution thread safely bypassed standard system termination to render diagnostics below.",
        color = Color.LightGray,
        style = MaterialTheme.typography.bodyMedium
      )
      Spacer(modifier = Modifier.height(16.dp))
      
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "EXCEPTION IDENTITY:",
            color = Color(0xFFFBBF24),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = throwable.javaClass.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "MESSAGE DETAILS:",
            color = Color(0xFFFBBF24),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = throwable.message ?: "[Null message details available]",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "CHRONOLOGICAL STACK TRACE:",
        color = Color.White,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(8.dp))
      
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Black, shape = RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Text(
          text = stackTraceString,
          color = Color(0xFF34D399), // Monospace Terminal Green
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
      Button(
        onClick = onReset,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Attempt System Reset", color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

