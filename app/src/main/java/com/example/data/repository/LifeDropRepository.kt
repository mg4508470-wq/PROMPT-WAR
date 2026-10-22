package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BloodRequest
import com.example.data.model.CoinTransaction
import com.example.data.model.CoinWallet
import com.example.data.model.Donation
import com.example.data.model.Hospital
import com.example.data.model.Redemption
import com.example.data.model.Report
import com.example.data.model.Reward
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LifeDropRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing LifeDrop.")
    }

    fun isCurrentUserAdmin(email: String?): Boolean {
        return email != null && email.equals("mg4508470@gmail.com", ignoreCase = true)
    }

    // -------------------------------------------------------------
    // User Profile
    // -------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        val isExisting = docRef.get().await().exists()

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "bloodGroup" to profile.bloodGroup,
            "city" to profile.city,
            "district" to profile.district,
            "radiusKm" to profile.radiusKm,
            "notificationsEnabled" to profile.notificationsEnabled,
            "isAvailable" to profile.isAvailable,
            "isVerifiedDonor" to profile.isVerifiedDonor,
            "role" to profile.role,
            "lastDonationDate" to profile.lastDonationDate,
            "donationCount" to profile.donationCount,
            "equippedBadge" to profile.equippedBadge,
            "equippedTheme" to profile.equippedTheme,
            "equippedFrame" to profile.equippedFrame,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!isExisting) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, SetOptions.merge()).await()
            // Auto initialize wallet with 20 welcome coins on profile save
            initWalletIfMissing(uid)
        } catch (e: Exception) {
            handleFirestoreError(e, if (isExisting) OperationType.UPDATE else OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun equipReward(userId: String, rewardType: String, rewardTitle: String): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
        val field = when (rewardType) {
            "THEME" -> "equippedTheme"
            "FRAME" -> "equippedFrame"
            else -> "equippedBadge"
        }
        docRef.update(mapOf(field to rewardTitle, "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    // -------------------------------------------------------------
    // Blood Requests
    // -------------------------------------------------------------
    fun observeVerifiedRequests(): Flow<List<BloodRequest>> = flow {
        val path = "bloodRequests"
        db.collection(path)
            .whereEqualTo("status", "verified")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(BloodRequest::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeUserRequests(userId: String): Flow<List<BloodRequest>> = flow {
        val path = "bloodRequests"
        db.collection(path)
            .whereEqualTo("requesterId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(BloodRequest::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeAllRequestsForAdmin(): Flow<List<BloodRequest>> = flow {
        val path = "bloodRequests"
        db.collection(path)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(BloodRequest::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun createBloodRequest(request: BloodRequest): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("bloodRequests")
        val newDocRef = colRef.document()

        // Assign realistic coordinates if not set (default around 13.0827, 80.2707 / Springfield base)
        val lat = if (request.latitude != 0.0) request.latitude else 13.0827 + (Math.random() - 0.5) * 0.08
        val lng = if (request.longitude != 0.0) request.longitude else 80.2707 + (Math.random() - 0.5) * 0.08

        val payload = mapOf(
            "id" to newDocRef.id,
            "requesterId" to uid,
            "requesterName" to request.requesterName,
            "requesterPhone" to request.requesterPhone,
            "patientRef" to request.patientRef,
            "bloodGroup" to request.bloodGroup,
            "units" to request.units,
            "hospitalName" to request.hospitalName,
            "hospitalAddress" to request.hospitalAddress,
            "city" to request.city,
            "district" to request.district,
            "latitude" to lat,
            "longitude" to lng,
            "urgency" to request.urgency,
            "requiredDate" to request.requiredDate,
            "requiredTime" to request.requiredTime,
            "status" to "pending",
            "notes" to request.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            newDocRef.set(payload).await()
            newDocRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateRequestStatus(
        requestId: String,
        newStatus: String,
        verifiedBy: String = ""
    ): Result<Unit> = runCatching {
        val docRef = db.collection("bloodRequests").document(requestId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (verifiedBy.isNotBlank()) {
            updates["verifiedBy"] = verifiedBy
        }

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBloodRequest(requestId: String): Result<Unit> = runCatching {
        val docRef = db.collection("bloodRequests").document(requestId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // Hospitals & Blood Banks (Live Map)
    // -------------------------------------------------------------
    fun observeHospitals(): Flow<List<Hospital>> = flow {
        val path = "hospitals"
        db.collection(path)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Hospital::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun seedSampleHospitalsIfEmpty() {
        try {
            val col = db.collection("hospitals")
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val samples = listOf(
                    Hospital("hosp_1", "City Central General Hospital", "456 Health Ave", "Springfield", 13.0827, 80.2707, "+1 555-0101", "hospital", 18),
                    Hospital("hosp_2", "Red Cross Regional Blood Bank", "12 Bloodline Way", "Springfield", 13.0910, 80.2810, "+1 555-0102", "blood_bank", 50),
                    Hospital("hosp_3", "Memorial Super Specialty Hospital", "88 Recovery Blvd", "Springfield", 13.0720, 80.2590, "+1 555-0103", "hospital", 12),
                    Hospital("hosp_4", "Apex Trauma & Blood Center", "102 Mercy Road", "Springfield", 13.0880, 80.2620, "+1 555-0104", "blood_bank", 35)
                )
                for (h in samples) {
                    col.document(h.id).set(h).await()
                }
            }
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------
    // LifeCoins Wallet & Transactions (WEBTOON Style)
    // -------------------------------------------------------------
    fun observeCoinWallet(userId: String): Flow<CoinWallet?> = flow {
        val path = "coinWallets/$userId"
        db.collection("coinWallets").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(CoinWallet::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun initWalletIfMissing(userId: String): Result<Unit> = runCatching {
        val docRef = db.collection("coinWallets").document(userId)
        val doc = docRef.get().await()
        if (!doc.exists()) {
            val wallet = mapOf(
                "userId" to userId,
                "balance" to 20,
                "totalEarned" to 20,
                "totalRedeemed" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(wallet).await()

            // Record welcome bonus transaction
            val txRef = db.collection("coinTransactions").document()
            txRef.set(
                mapOf(
                    "id" to txRef.id,
                    "userId" to userId,
                    "transactionType" to "EARN",
                    "amount" to 20,
                    "reason" to "Profile verification bonus",
                    "referenceId" to "welcome_bonus",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun earnCoins(amount: Int, reason: String, referenceId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val walletRef = db.collection("coinWallets").document(uid)

        // Increment wallet balance
        walletRef.update(
            mapOf(
                "balance" to FieldValue.increment(amount.toLong()),
                "totalEarned" to FieldValue.increment(amount.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Create transaction record
        val txRef = db.collection("coinTransactions").document()
        txRef.set(
            mapOf(
                "id" to txRef.id,
                "userId" to uid,
                "transactionType" to "EARN",
                "amount" to amount,
                "reason" to reason,
                "referenceId" to referenceId,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun redeemReward(reward: Reward): Result<Redemption> = runCatching {
        val uid = requireUserId()
        val walletRef = db.collection("coinWallets").document(uid)
        val walletDoc = walletRef.get().await()
        val currentBalance = walletDoc.getLong("balance")?.toInt() ?: 0

        if (currentBalance < reward.coinPrice) {
            throw IllegalStateException("Insufficient LifeCoins! Required: ${reward.coinPrice}, Available: $currentBalance")
        }

        // Deduct coins
        walletRef.update(
            mapOf(
                "balance" to FieldValue.increment(-reward.coinPrice.toLong()),
                "totalRedeemed" to FieldValue.increment(reward.coinPrice.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Create transaction record
        val txRef = db.collection("coinTransactions").document()
        txRef.set(
            mapOf(
                "id" to txRef.id,
                "userId" to uid,
                "transactionType" to "REDEEM",
                "amount" to reward.coinPrice,
                "reason" to "Redeemed: ${reward.title}",
                "referenceId" to reward.id,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Create Redemption record
        val voucherCode = "DROP-" + UUID.randomUUID().toString().take(6).uppercase()
        val redRef = db.collection("redemptions").document()
        val redemption = Redemption(
            id = redRef.id,
            userId = uid,
            rewardId = reward.id,
            rewardTitle = reward.title,
            rewardType = reward.rewardType,
            coinsSpent = reward.coinPrice,
            redemptionStatus = "completed",
            voucherCode = voucherCode
        )

        redRef.set(
            mapOf(
                "id" to redemption.id,
                "userId" to redemption.userId,
                "rewardId" to redemption.rewardId,
                "rewardTitle" to redemption.rewardTitle,
                "rewardType" to redemption.rewardType,
                "coinsSpent" to redemption.coinsSpent,
                "redemptionStatus" to redemption.redemptionStatus,
                "voucherCode" to redemption.voucherCode,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // If digital badge or theme, auto-equip
        equipReward(uid, reward.rewardType, reward.title)

        redemption
    }

    fun observeTransactions(userId: String): Flow<List<CoinTransaction>> = flow {
        val path = "coinTransactions"
        db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(CoinTransaction::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeRedemptions(userId: String): Flow<List<Redemption>> = flow {
        val path = "redemptions"
        db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Redemption::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeRewards(): Flow<List<Reward>> = flow {
        val path = "rewards"
        db.collection(path)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Reward::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun seedInitialRewardsIfEmpty() {
        try {
            val col = db.collection("rewards")
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val catalog = listOf(
                    Reward("rew_theme_gold", "Golden Lifesaver Theme", "Exclusive glowing gold profile aura & accent", 100, "THEME", true, "gold"),
                    Reward("rew_badge_angel", "Guardian Angel Badge", "Honors donors ready for immediate emergency calls", 50, "BADGE", true, "angel"),
                    Reward("rew_frame_ruby", "Ruby Pulse Avatar Frame", "Animated crimson lifeline frame for your donor profile", 75, "FRAME", true, "ruby"),
                    Reward("rew_voucher_health", "Free Health Screening Voucher", "50% off full body blood lipid profile at partner labs", 150, "VOUCHER", true, "voucher"),
                    Reward("rew_voucher_pharmacy", "Partner Pharmacy 20% Coupon", "Save on emergency first aid & medical supplies", 60, "VOUCHER", true, "pharmacy"),
                    Reward("rew_badge_shield", "Blood Shield Champion", "Legendary donor badge for 3+ successful donations", 120, "BADGE", true, "shield")
                )
                for (r in catalog) {
                    col.document(r.id).set(r).await()
                }
            }
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------
    // Donations
    // -------------------------------------------------------------
    fun observeUserDonations(userId: String): Flow<List<Donation>> = flow {
        val path = "donations"
        db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Donation::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun recordDonation(donation: Donation): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("donations")
        val docRef = colRef.document()

        val payload = mapOf(
            "id" to docRef.id,
            "userId" to uid,
            "requestId" to donation.requestId,
            "hospitalName" to donation.hospitalName,
            "bloodGroup" to donation.bloodGroup,
            "units" to donation.units,
            "donationDate" to donation.donationDate,
            "status" to donation.status,
            "certificateUrl" to donation.certificateUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            // Increment user donationCount and update lastDonationDate
            val userRef = db.collection("users").document(uid)
            userRef.update(
                mapOf(
                    "donationCount" to FieldValue.increment(donation.units.toLong()),
                    "lastDonationDate" to donation.donationDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Award 50 LifeCoins for completing a verified blood donation
            earnCoins(50, "Verified blood donation reward at ${donation.hospitalName}", docRef.id)

            docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // Reports
    // -------------------------------------------------------------
    fun observeReports(): Flow<List<Report>> = flow {
        val path = "reports"
        db.collection(path)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Report::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun submitReport(requestId: String, reason: String): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("reports")
        val docRef = colRef.document()

        val payload = mapOf(
            "id" to docRef.id,
            "reporterId" to uid,
            "requestId" to requestId,
            "reason" to reason,
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }
}
