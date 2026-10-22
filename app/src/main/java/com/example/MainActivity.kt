package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.util.NotificationHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.LifeDropTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Android notification channel for urgent blood matches
        NotificationHelper.initChannel(this)

        val initialScreen = intent?.getStringExtra("OPEN_SCREEN") ?: "home"

        setContent {
            LifeDropTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LifeDropApp(initialScreen = initialScreen)
                }
            }
        }
    }
}

@Composable
fun LifeDropApp(initialScreen: String = "home") {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AppNavigation(
            currentUser = currentUser,
            initialScreen = initialScreen
        )
    }
}
