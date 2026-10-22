package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BloodGroup(val display: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    companion object {
        fun fromString(value: String): BloodGroup {
            return entries.find { it.display.equals(value.trim(), ignoreCase = true) } ?: O_POS
        }

        // Returns true if a donor with `donorGroup` can donate to patient with `patientGroup`
        fun isCompatible(donorGroup: String, patientGroup: String): Boolean {
            val donor = fromString(donorGroup)
            val recipient = fromString(patientGroup)

            return when (recipient) {
                AB_POS -> true // Universal recipient
                AB_NEG -> donor in listOf(AB_NEG, A_NEG, B_NEG, O_NEG)
                A_POS -> donor in listOf(A_POS, A_NEG, O_POS, O_NEG)
                A_NEG -> donor in listOf(A_NEG, O_NEG)
                B_POS -> donor in listOf(B_POS, B_NEG, O_POS, O_NEG)
                B_NEG -> donor in listOf(B_NEG, O_NEG)
                O_POS -> donor in listOf(O_POS, O_NEG)
                O_NEG -> donor == O_NEG // O- can only receive from O-
            }
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val bloodGroup: String = "O+",
    val city: String = "",
    val district: String = "",
    val radiusKm: Int = 25,
    val notificationsEnabled: Boolean = true,
    val isAvailable: Boolean = true,
    val isVerifiedDonor: Boolean = false,
    val role: String = "donor", // "donor", "admin", "hospital"
    val lastDonationDate: String = "",
    val donationCount: Int = 0,
    val equippedBadge: String = "",
    val equippedTheme: String = "",
    val equippedFrame: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterPhone: String = "",
    val patientRef: String = "",
    val bloodGroup: String = "O+",
    val units: Int = 1,
    val hospitalName: String = "",
    val hospitalAddress: String = "",
    val city: String = "",
    val district: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val urgency: String = "Urgent", // "Normal", "Urgent", "Critical"
    val requiredDate: String = "",
    val requiredTime: String = "",
    val status: String = "pending", // "pending", "verified", "fulfilled", "cancelled", "rejected"
    val verifiedBy: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Hospital(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val type: String = "hospital", // "hospital", "blood_bank"
    val availableBeds: Int = 10
)

@IgnoreExtraProperties
data class CoinWallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalRedeemed: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CoinTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val transactionType: String = "EARN", // "EARN", "REDEEM"
    val amount: Int = 0,
    val reason: String = "",
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reward(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val coinPrice: Int = 50,
    val rewardType: String = "BADGE", // "THEME", "BADGE", "FRAME", "VOUCHER"
    val active: Boolean = true,
    val iconName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Redemption(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rewardId: String = "",
    val rewardTitle: String = "",
    val rewardType: String = "BADGE",
    val coinsSpent: Int = 0,
    val redemptionStatus: String = "completed",
    val voucherCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Donation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val requestId: String = "",
    val hospitalName: String = "",
    val bloodGroup: String = "O+",
    val units: Int = 1,
    val donationDate: String = "",
    val status: String = "completed", // "completed", "scheduled"
    val certificateUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Report(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val requestId: String = "",
    val reason: String = "",
    val status: String = "open", // "open", "reviewed", "dismissed"
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class NotificationItem(
    val id: String = "",
    val requestId: String = "",
    val title: String = "",
    val message: String = "",
    val bloodGroup: String = "",
    val hospitalName: String = "",
    val urgency: String = "Urgent",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val responseStatus: String = "NONE"
)
