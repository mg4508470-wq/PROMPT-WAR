package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.model.BloodGroup
import com.example.data.model.UserProfile
import com.example.data.repository.LifeDropRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BloodRequestFormScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DonationHistoryScreen
import com.example.ui.screens.LifeCoinsWalletScreen
import com.example.ui.screens.LiveMapScreen
import com.example.ui.screens.MatchingRequestsScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SettingsScreen
import com.google.firebase.auth.FirebaseUser

enum class AppNavTab(val title: String) {
    HOME("Home"),
    MAP("Live Map"),
    MATCHING("Matches"),
    WALLET("Wallet"),
    NOTIFICATIONS("Alerts")
}

@Composable
fun AppNavigation(
    currentUser: FirebaseUser?,
    initialScreen: String = "home"
) {
    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { /* Auth state listener triggers navigation */ })
        return
    }

    val context = LocalContext.current
    val repository = remember { LifeDropRepository(context) }

    // Real-time observations
    val userProfile by repository.observeUserProfile(currentUser.uid).collectAsState(initial = null)
    val verifiedRequests by repository.observeVerifiedRequests().collectAsState(initial = emptyList())
    val userDonations by repository.observeUserDonations(currentUser.uid).collectAsState(initial = emptyList())
    val allRequests by repository.observeAllRequestsForAdmin().collectAsState(initial = emptyList())
    val reports by repository.observeReports().collectAsState(initial = emptyList())
    val hospitals by repository.observeHospitals().collectAsState(initial = emptyList())

    var currentTab by remember {
        mutableStateOf(
            when (initialScreen) {
                "map" -> AppNavTab.MAP
                "matching" -> AppNavTab.MATCHING
                "wallet" -> AppNavTab.WALLET
                else -> AppNavTab.HOME
            }
        )
    }
    var activeSubScreen by remember { mutableStateOf<String?>(null) } // "request", "history", "admin", "settings", "edit_profile"

    // Initial check for profile presence
    var isCheckingProfile by remember { mutableStateOf(true) }
    LaunchedEffect(userProfile) {
        isCheckingProfile = false
    }

    if (isCheckingProfile && userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (userProfile == null) {
        // First-time donor profile setup
        ProfileSetupScreen(
            currentUserId = currentUser.uid,
            currentUserEmail = currentUser.email ?: "",
            currentDisplayName = currentUser.displayName ?: "",
            existingProfile = null,
            repository = repository,
            onProfileSaved = { /* State updates automatically */ }
        )
        return
    }

    val profile = userProfile!!

    // Back navigation handling
    BackHandler(enabled = activeSubScreen != null || currentTab != AppNavTab.HOME) {
        if (activeSubScreen != null) {
            activeSubScreen = null
        } else {
            currentTab = AppNavTab.HOME
        }
    }

    // Sub-screens routing
    if (activeSubScreen == "request") {
        BloodRequestFormScreen(
            currentUserId = currentUser.uid,
            currentUserPhone = profile.phone,
            currentUserName = profile.name,
            userCity = profile.city,
            repository = repository,
            onNavigateBack = { activeSubScreen = null },
            onRequestSubmitted = { activeSubScreen = null }
        )
        return
    }

    if (activeSubScreen == "history") {
        DonationHistoryScreen(
            userProfile = profile,
            donations = userDonations,
            repository = repository,
            onNavigateBack = { activeSubScreen = null }
        )
        return
    }

    if (activeSubScreen == "admin") {
        AdminDashboardScreen(
            currentAdminUid = currentUser.uid,
            allRequests = allRequests,
            reports = reports,
            repository = repository,
            onNavigateBack = { activeSubScreen = null }
        )
        return
    }

    if (activeSubScreen == "settings") {
        SettingsScreen(
            userProfile = profile,
            repository = repository,
            onNavigateBack = { activeSubScreen = null },
            onNavigateToEditProfile = { activeSubScreen = "edit_profile" },
            onSignOut = { /* Handled by AuthStateListener */ }
        )
        return
    }

    if (activeSubScreen == "edit_profile") {
        ProfileSetupScreen(
            currentUserId = currentUser.uid,
            currentUserEmail = currentUser.email ?: "",
            currentDisplayName = profile.name,
            existingProfile = profile,
            repository = repository,
            onProfileSaved = { activeSubScreen = null },
            onNavigateBack = { activeSubScreen = null }
        )
        return
    }

    val compatibleCount = remember(verifiedRequests, profile.bloodGroup) {
        verifiedRequests.count { BloodGroup.isCompatible(profile.bloodGroup, it.bloodGroup) }
    }

    // Main Scaffold with Bottom Navigation
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // 1. Home
                NavigationBarItem(
                    selected = currentTab == AppNavTab.HOME,
                    onClick = { currentTab = AppNavTab.HOME },
                    icon = {
                        Icon(
                            if (currentTab == AppNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )

                // 2. Live Map
                NavigationBarItem(
                    selected = currentTab == AppNavTab.MAP,
                    onClick = { currentTab = AppNavTab.MAP },
                    icon = {
                        Icon(
                            if (currentTab == AppNavTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Live Map"
                        )
                    },
                    label = { Text("Live Map") },
                    modifier = Modifier.testTag("nav_map")
                )

                // 3. Matches
                NavigationBarItem(
                    selected = currentTab == AppNavTab.MATCHING,
                    onClick = { currentTab = AppNavTab.MATCHING },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (compatibleCount > 0) {
                                    Badge { Text("$compatibleCount") }
                                }
                            }
                        ) {
                            Icon(
                                if (currentTab == AppNavTab.MATCHING) Icons.Filled.Bloodtype else Icons.Outlined.Bloodtype,
                                contentDescription = "Matches"
                            )
                        }
                    },
                    label = { Text("Matches") },
                    modifier = Modifier.testTag("nav_matches")
                )

                // 4. LifeCoins Wallet
                NavigationBarItem(
                    selected = currentTab == AppNavTab.WALLET,
                    onClick = { currentTab = AppNavTab.WALLET },
                    icon = {
                        Icon(
                            if (currentTab == AppNavTab.WALLET) Icons.Filled.MonetizationOn else Icons.Outlined.MonetizationOn,
                            contentDescription = "Wallet"
                        )
                    },
                    label = { Text("Wallet") },
                    modifier = Modifier.testTag("nav_wallet")
                )

                // 5. Alerts / Notifications
                NavigationBarItem(
                    selected = currentTab == AppNavTab.NOTIFICATIONS,
                    onClick = { currentTab = AppNavTab.NOTIFICATIONS },
                    icon = {
                        Icon(
                            if (currentTab == AppNavTab.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Alerts"
                        )
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("nav_alerts")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.HOME -> {
                    DashboardScreen(
                        userProfile = profile,
                        verifiedRequests = verifiedRequests,
                        repository = repository,
                        onNavigateToMatching = { currentTab = AppNavTab.MATCHING },
                        onNavigateToLiveMap = { currentTab = AppNavTab.MAP },
                        onNavigateToWallet = { currentTab = AppNavTab.WALLET },
                        onNavigateToRequestForm = { activeSubScreen = "request" },
                        onNavigateToHistory = { activeSubScreen = "history" },
                        onNavigateToNotifications = { currentTab = AppNavTab.NOTIFICATIONS },
                        onNavigateToAdmin = { activeSubScreen = "admin" },
                        onNavigateToEditProfile = { activeSubScreen = "edit_profile" }
                    )
                }

                AppNavTab.MAP -> {
                    LiveMapScreen(
                        userProfile = profile,
                        verifiedRequests = verifiedRequests,
                        hospitals = hospitals,
                        repository = repository,
                        onNavigateBack = { currentTab = AppNavTab.HOME }
                    )
                }

                AppNavTab.MATCHING -> {
                    MatchingRequestsScreen(
                        userProfile = profile,
                        verifiedRequests = verifiedRequests,
                        repository = repository,
                        onNavigateBack = { currentTab = AppNavTab.HOME }
                    )
                }

                AppNavTab.WALLET -> {
                    LifeCoinsWalletScreen(
                        userProfile = profile,
                        repository = repository,
                        onNavigateBack = { currentTab = AppNavTab.HOME }
                    )
                }

                AppNavTab.NOTIFICATIONS -> {
                    NotificationCenterScreen(
                        userProfile = profile,
                        verifiedRequests = verifiedRequests,
                        repository = repository,
                        onNavigateBack = { currentTab = AppNavTab.HOME }
                    )
                }
            }
        }
    }
}
