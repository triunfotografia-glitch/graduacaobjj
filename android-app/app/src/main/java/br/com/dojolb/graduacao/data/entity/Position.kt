package br.com.dojolb.graduacao.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "positions",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
@Serializable
data class Position(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val positionNumber: Int,
    val name: String = "",
    val grade: Int = -1, // -1 = not evaluated, 0, 25, 50, 75, 100
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val GRADE_NOT_EVALUATED = -1
        val VALID_GRADES = listOf(0, 25, 50, 75, 100)
    }

    val isEvaluated: Boolean
        get() = grade != GRADE_NOT_EVALUATED
}