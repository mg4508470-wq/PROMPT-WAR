package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BloodRequest
import com.example.data.model.Donation
import com.example.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeDropRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_valid_succeeds() = runBlocking {
        val uid = signInTestUser("donor_alice@example.com")
        val repository = LifeDropRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            name = "Alice Donor",
            email = "donor_alice@example.com",
            phone = "+1987654321",
            bloodGroup = "O+",
            city = "Metropolis",
            district = "Downtown",
            radiusKm = 25,
            notificationsEnabled = true,
            isAvailable = true,
            isVerifiedDonor = false,
            role = "donor"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }
        assertTrue("Profile save should succeed", result.isSuccess)
    }

    @Test
    fun createBloodRequest_valid_createsDocument() = runBlocking {
        val uid = signInTestUser("requester_bob@example.com")
        val repository = LifeDropRepository(firestore)

        val request = BloodRequest(
            requesterId = uid,
            requesterName = "Bob Requester",
            requesterPhone = "+1555123456",
            patientRef = "REF-2026-99",
            bloodGroup = "A-",
            units = 2,
            hospitalName = "City Central Hospital",
            hospitalAddress = "456 Health Ave",
            city = "Metropolis",
            district = "North",
            urgency = "Critical",
            requiredDate = "2026-10-25",
            requiredTime = "14:00",
            notes = "Emergency surgery patient"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBloodRequest(request)
        }
        assertTrue("Blood request creation should succeed", result.isSuccess)
        assertTrue("Created document ID should not be blank", result.getOrNull()?.isNotBlank() == true)
    }

    @Test
    fun recordDonation_valid_recordsSuccessfully() = runBlocking {
        val uid = signInTestUser("donor_charlie@example.com")
        val repository = LifeDropRepository(firestore)

        // First initialize user profile so update with increment works
        repository.saveUserProfile(
            UserProfile(
                userId = uid,
                name = "Charlie",
                email = "donor_charlie@example.com",
                phone = "+12345678",
                bloodGroup = "B+",
                city = "Metropolis",
                district = "East"
            )
        )

        val donation = Donation(
            userId = uid,
            hospitalName = "Metro Blood Bank",
            bloodGroup = "B+",
            units = 1,
            donationDate = "2026-10-22",
            status = "completed"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordDonation(donation)
        }
        assertTrue("Donation record should succeed", result.isSuccess)
    }

    @Test
    fun submitReport_valid_submitsSuccessfully() = runBlocking {
        val uid = signInTestUser("reporter_dan@example.com")
        val repository = LifeDropRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitReport(
                requestId = "fake_request_123",
                reason = "Suspicious duplicate request, caller asks for money"
            )
        }
        assertTrue("Report submission should succeed", result.isSuccess)
    }
}
