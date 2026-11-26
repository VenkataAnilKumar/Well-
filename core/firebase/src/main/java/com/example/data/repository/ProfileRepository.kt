package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.HealthProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val session: FirestoreSessionHelper
) {
    private fun profileDoc(uid: String) =
        session.userDoc(uid).collection("profile").document("main")

    fun observeProfile(userId: String): Flow<HealthProfile?> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/profile/main"
        emitAll(
            profileDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            HealthProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveProfile(
        userId: String,
        displayName: String,
        age: Long,
        biologicalSex: BiologicalSex,
        bloodType: BloodType,
        allergies: List<String>,
        medications: List<String>,
        conditions: List<String>,
        wellnessGoals: List<String>,
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        onboardingCompleted: Boolean = true,
        disclaimerAccepted: Boolean = true,
        ageConfirmed18Plus: Boolean = true,
        preferredHealthRegion: String = "",
        preferredEmergencyCountry: String = "",
        coverageProvider: String = "",
        coverageMemberId: String = "",
        primaryDoctorName: String = "",
        preferredPharmacy: String = ""
    ): Result<Unit> {
        val path = "users/$userId/profile/main"
        return try {
            val uid = session.requireUserId(userId)
            val safeName = displayName.trim().take(80)
            val safeAge = age.coerceIn(0L, 130L)
            val cleanAllergies = session.sanitizeList(allergies, 25)
            val cleanMeds = session.sanitizeList(medications, 25)
            val cleanConditions = session.sanitizeList(conditions, 25)
            val cleanGoals = session.sanitizeList(wellnessGoals, 15)
            val safeContactName = emergencyContactName.trim().take(80)
            val safeContactPhone = emergencyContactPhone.trim().take(30)
            val safeHealthRegion = preferredHealthRegion.trim().uppercase().take(8)
            val safeCountry = preferredEmergencyCountry.trim().uppercase().take(8)
            val safeCoverageProvider = coverageProvider.trim().take(80)
            val safeCoverageMemberId = coverageMemberId.trim().take(40)
            val safePrimaryDoctor = primaryDoctorName.trim().take(80)
            val safePharmacy = preferredPharmacy.trim().take(100)

            val docRef = profileDoc(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "age" to safeAge,
                        "biologicalSex" to biologicalSex.name,
                        "bloodType" to bloodType.code,
                        "allergies" to cleanAllergies,
                        "medications" to cleanMeds,
                        "conditions" to cleanConditions,
                        "wellnessGoals" to cleanGoals,
                        "onboardingCompleted" to onboardingCompleted,
                        "disclaimerAccepted" to disclaimerAccepted,
                        "ageConfirmed18Plus" to ageConfirmed18Plus,
                        "preferredHealthRegion" to safeHealthRegion,
                        "preferredEmergencyCountry" to safeCountry,
                        "emergencyContactName" to safeContactName,
                        "emergencyContactPhone" to safeContactPhone,
                        "coverageProvider" to safeCoverageProvider,
                        "coverageMemberId" to safeCoverageMemberId,
                        "primaryDoctorName" to safePrimaryDoctor,
                        "preferredPharmacy" to safePharmacy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "age" to safeAge,
                        "biologicalSex" to biologicalSex.name,
                        "bloodType" to bloodType.code,
                        "allergies" to cleanAllergies,
                        "medications" to cleanMeds,
                        "conditions" to cleanConditions,
                        "wellnessGoals" to cleanGoals,
                        "onboardingCompleted" to onboardingCompleted,
                        "disclaimerAccepted" to disclaimerAccepted,
                        "ageConfirmed18Plus" to ageConfirmed18Plus,
                        "preferredHealthRegion" to safeHealthRegion,
                        "preferredEmergencyCountry" to safeCountry,
                        "emergencyContactName" to safeContactName,
                        "emergencyContactPhone" to safeContactPhone,
                        "coverageProvider" to safeCoverageProvider,
                        "coverageMemberId" to safeCoverageMemberId,
                        "primaryDoctorName" to safePrimaryDoctor,
                        "preferredPharmacy" to safePharmacy,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
