package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransaction
import com.example.data.model.CoinWallet
import com.example.data.model.Redemption
import com.example.data.model.Reward
import com.example.data.model.UserProfile
import com.example.data.repository.LifeDropRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCoinsWalletScreen(
    userProfile: UserProfile,
    repository: LifeDropRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time flows
    val wallet by repository.observeCoinWallet(userProfile.userId).collectAsState(initial = null)
    val transactions by repository.observeTransactions(userProfile.userId).collectAsState(initial = emptyList())
    val redemptions by repository.observeRedemptions(userProfile.userId).collectAsState(initial = emptyList())
    val rewardsCatalog by repository.observeRewards().collectAsState(initial = emptyList())

    // Seed sample rewards on startup
    LaunchedEffect(Unit) {
        repository.seedInitialRewardsIfEmpty()
        repository.initWalletIfMissing(userProfile.userId)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Store, 1: Earn, 2: History, 3: My Rewards
    var selectedRewardForRedemption by remember { mutableStateOf<Reward?>(null) }
    var isRedeeming by remember { mutableStateOf(false) }
    var lastClaimedRedemption by remember { mutableStateOf<Redemption?>(null) }
    var showQuizDialog by remember { mutableStateOf(false) }

    val currentBalance = wallet?.balance ?: 20
    val totalEarned = wallet?.totalEarned ?: 20
    val totalRedeemed = wallet?.totalRedeemed ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LifeCoins Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1010),
                    titleContentColor = Color(0xFFFFD54F),
                    navigationIconContentColor = Color(0xFFFFD54F)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("lifecoins_wallet_screen")
        ) {
            // WEBTOON-Style Golden Header Wallet Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1010),
                                Color(0xFF3E1F1F)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Stylized 3D Gold LifeCoin graphic
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFFFEE58), Color(0xFFFFB300), Color(0xFFF57F17))
                                    )
                                )
                                .border(2.dp, Color(0xFFFFF9C4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF5D4037)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "LIFECOINS BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$currentBalance",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Ledger Metrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Earned", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(text = "+$totalEarned", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontSize = 14.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Redeemed", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(text = "-$totalRedeemed", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 14.sp)
                        }
                    }
                }
            }

            // WEBTOON-Style Segmented Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Redeem Store", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Earn Coins", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("My Rewards", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Redeem Store
                    RedeemStoreTab(
                        rewards = rewardsCatalog,
                        currentBalance = currentBalance,
                        onSelectReward = { reward -> selectedRewardForRedemption = reward }
                    )
                }

                1 -> {
                    // Earn Coins Tasks
                    EarnCoinsTab(
                        userProfile = userProfile,
                        onOpenQuiz = { showQuizDialog = true },
                        onReferFriend = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Join me on LifeDrop! Donate blood, save lives, and earn virtual LifeCoins rewards. Use referral code: ${userProfile.name.take(4).uppercase()}-HERO")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Refer a Hero Donor")
                            context.startActivity(shareIntent)
                            scope.launch {
                                repository.earnCoins(15, "Referral bonus for sharing LifeDrop", "referral")
                                Toast.makeText(context, "+15 LifeCoins awarded for referral!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                2 -> {
                    // Transaction History Ledger
                    TransactionHistoryTab(transactions = transactions)
                }

                3 -> {
                    // My Claimed Rewards & Customizations
                    MyRewardsTab(
                        redemptions = redemptions,
                        userProfile = userProfile,
                        onEquip = { type, title ->
                            scope.launch {
                                repository.equipReward(userProfile.userId, type, title)
                                Toast.makeText(context, "Equipped $title to your profile!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Confirmation & Processing Dialog for Redemption
    if (selectedRewardForRedemption != null) {
        val reward = selectedRewardForRedemption!!
        val canAfford = currentBalance >= reward.coinPrice

        AlertDialog(
            onDismissRequest = { if (!isRedeeming) selectedRewardForRedemption = null },
            title = {
                Text(
                    text = "Redeem ${reward.title}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = reward.description, style = MaterialTheme.typography.bodyMedium)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Cost:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${reward.coinPrice} LifeCoins",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "Current Balance: $currentBalance LifeCoins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!canAfford) {
                        Text(
                            text = "You need ${reward.coinPrice - currentBalance} more LifeCoins! Complete an awareness quiz or log a donation in the 'Earn Coins' tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRedeeming = true
                        scope.launch {
                            val result = repository.redeemReward(reward)
                            isRedeeming = false
                            selectedRewardForRedemption = null
                            if (result.isSuccess) {
                                lastClaimedRedemption = result.getOrNull()
                                Toast.makeText(context, "Success! ${reward.title} redeemed.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Redemption failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = canAfford && !isRedeeming,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isRedeeming) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Confirm Redemption")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedRewardForRedemption = null },
                    enabled = !isRedeeming
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Claim Dialog
    if (lastClaimedRedemption != null) {
        val claim = lastClaimedRedemption!!
        AlertDialog(
            onDismissRequest = { lastClaimedRedemption = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reward Claimed!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You successfully redeemed '${claim.rewardTitle}' for ${claim.coinsSpent} LifeCoins.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (claim.voucherCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Your digital coupon voucher code:", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = claim.voucherCode,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B5E20),
                                letterSpacing = 2.sp
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("LifeDrop Voucher", claim.voucherCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Voucher code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }

                    Text(
                        text = "This reward is now permanently saved in your 'My Rewards' tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { lastClaimedRedemption = null }) {
                    Text("Awesome!")
                }
            }
        )
    }

    // Educational Awareness Quiz Dialog
    if (showQuizDialog) {
        BloodAwarenessQuizDialog(
            onDismiss = { showQuizDialog = false },
            onQuizPassed = {
                scope.launch {
                    repository.earnCoins(10, "Blood Donation Awareness Quiz Passed", "quiz_edu")
                    showQuizDialog = false
                    Toast.makeText(context, "Quiz complete! +10 LifeCoins added to your wallet!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
fun RedeemStoreTab(
    rewards: List<Reward>,
    currentBalance: Int,
    onSelectReward: (Reward) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Featured Digital Rewards & Vouchers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Redeem your virtual LifeCoins for custom donor themes, badges, and partner coupons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(rewards, key = { it.id }) { reward ->
            val canAfford = currentBalance >= reward.coinPrice

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reward Category Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (reward.rewardType) {
                                    "THEME" -> Color(0xFFFFD54F).copy(alpha = 0.25f)
                                    "FRAME" -> Color(0xFFFF8A80).copy(alpha = 0.25f)
                                    "VOUCHER" -> Color(0xFF80CBC4).copy(alpha = 0.25f)
                                    else -> Color(0xFFB388FF).copy(alpha = 0.25f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (reward.rewardType) {
                                "THEME" -> Icons.Default.Palette
                                "FRAME" -> Icons.Default.Star
                                "VOUCHER" -> Icons.Default.LocalAtm
                                else -> Icons.Default.WorkspacePremium
                            },
                            contentDescription = null,
                            tint = when (reward.rewardType) {
                                "THEME" -> Color(0xFFF57F17)
                                "FRAME" -> Color(0xFFD32F2F)
                                "VOUCHER" -> Color(0xFF00796B)
                                else -> Color(0xFF6200EA)
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reward.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reward.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🪙 ${reward.coinPrice} Coins",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canAfford) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSelectReward(reward) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canAfford) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(if (canAfford) "Redeem" else "Unlock", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EarnCoinsTab(
    userProfile: UserProfile,
    onOpenQuiz: () -> Unit,
    onReferFriend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Ways to Earn LifeCoins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Participate in legitimate community actions and verified donations. LifeCoins are non-cash virtual appreciation points.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Complete Donation
        item {
            EarnCard(
                icon = Icons.Filled.Bloodtype,
                title = "Complete Verified Blood Donation",
                coins = "+50 Coins",
                desc = "Awarded automatically when an authorized hospital/blood bank logs your completed donation.",
                actionLabel = "Log in History",
                onAction = {}
            )
        }

        // 2. Educational Quiz
        item {
            EarnCard(
                icon = Icons.Default.Quiz,
                title = "Blood Donor Awareness Quiz",
                coins = "+10 Coins",
                desc = "Learn about blood compatibility, donor health, and cross-matching to earn coins.",
                actionLabel = "Start Quiz",
                onAction = onOpenQuiz
            )
        }

        // 3. Refer a Hero Donor
        item {
            EarnCard(
                icon = Icons.Default.Share,
                title = "Refer a Friend to LifeDrop",
                coins = "+15 Coins",
                desc = "Invite family and friends to register as voluntary donors in your district.",
                actionLabel = "Share Invite",
                onAction = onReferFriend
            )
        }

        // 4. Profile Verification
        item {
            EarnCard(
                icon = Icons.Default.Verified,
                title = "Complete Profile Verification",
                coins = "+20 Coins",
                desc = "Verify mobile OTP and set accurate blood group and radius preferences.",
                actionLabel = if (userProfile.isVerifiedDonor) "Verified ✓" else "Verify Now",
                onAction = {},
                isCompleted = userProfile.isVerifiedDonor
            )
        }
    }
}

@Composable
fun EarnCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    coins: String,
    desc: String,
    actionLabel: String,
    onAction: () -> Unit,
    isCompleted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = coins, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "Claimed",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryTab(transactions: List<CoinTransaction>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No coin transactions yet. Earn coins by completing tasks or quizzes!", textAlign = TextAlign.Center, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                val isEarn = tx.transactionType == "EARN"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isEarn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEarn) "+" else "-",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = if (isEarn) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = tx.reason, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "ID: ${tx.id.take(8).uppercase()}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Text(
                            text = if (isEarn) "+${tx.amount}" else "-${tx.amount}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isEarn) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyRewardsTab(
    redemptions: List<Redemption>,
    userProfile: UserProfile,
    onEquip: (String, String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Currently Equipped Customizations Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Active Profile Customizations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Active Badge: ${userProfile.equippedBadge.ifBlank { "Standard Hero" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Active Theme: ${userProfile.equippedTheme.ifBlank { "Crimson Medical (Default)" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                text = "Unlocked Rewards Inventory (${redemptions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (redemptions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No rewards redeemed yet. Visit the Redeem Store to get badges, themes & vouchers!", textAlign = TextAlign.Center, color = Color.Gray)
                }
            }
        } else {
            items(redemptions, key = { it.id }) { red ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF57F17))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = red.rewardTitle, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "${red.coinsSpent} Coins", fontSize = 12.sp, color = Color.Gray)
                        }

                        if (red.voucherCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Code: ${red.voucherCode}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Voucher", red.voucherCode))
                                    Toast.makeText(context, "Copied code!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Copy Code")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onEquip(red.rewardType, red.rewardTitle) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Equip to Donor Profile")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BloodAwarenessQuizDialog(
    onDismiss: () -> Unit,
    onQuizPassed: () -> Unit
) {
    var selectedOptionQ1 by remember { mutableIntStateOf(-1) }
    var selectedOptionQ2 by remember { mutableIntStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blood Donor Awareness Quiz", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Q1: Which blood group is known as the Universal Donor for Red Blood Cells?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                listOf("A) AB+", "B) O- (O Negative)", "C) B+", "D) A-").forEachIndexed { idx, opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionQ1 = idx }
                    ) {
                        RadioButton(selected = selectedOptionQ1 == idx, onClick = { selectedOptionQ1 = idx })
                        Text(text = opt, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text(
                    text = "Q2: What is the minimum recommended body weight to donate blood safely?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                listOf("A) 35 kg", "B) 40 kg", "C) 50 kg (110 lbs)", "D) 75 kg").forEachIndexed { idx, opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionQ2 = idx }
                    ) {
                        RadioButton(selected = selectedOptionQ2 == idx, onClick = { selectedOptionQ2 = idx })
                        Text(text = opt, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Correct answers: Q1 is idx 1 (O-), Q2 is idx 2 (50kg)
                    if (selectedOptionQ1 == 1 && selectedOptionQ2 == 2) {
                        onQuizPassed()
                    } else {
                        errorMessage = "Some answers were incorrect. Hint: O- is universal donor, and 50 kg is standard minimum weight."
                    }
                }
            ) {
                Text("Submit Answers")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
