package br.com.dojolb.graduacao.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "students")
@Serializable
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val currentBelt: String,
    val desiredBelt: String,
    val examDate: String, // DD/MM/YYYY format
    val positionsCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val finalAverage: Double = 0.0,
    val result: String = "IN_PROGRESS" // IN_PROGRESS, APPROVED, FAILED
) {
    companion object {
        const val RESULT_IN_PROGRESS = "IN_PROGRESS"
        const val RESULT_APPROVED = "APPROVED"
        const val RESULT_FAILED = "FAILED"
    }
}