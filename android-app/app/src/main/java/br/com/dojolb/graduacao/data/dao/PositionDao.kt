package br.com.dojolb.graduacao.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.dojolb.graduacao.data.entity.Position
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(position: Position): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(positions: List<Position>): List<Long>

    @Update
    suspend fun update(position: Position): Int

    @Update
    suspend fun updateAll(positions: List<Position>): Int

    @Query("DELETE FROM positions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM positions WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: Long): Int

    @Query("SELECT * FROM positions WHERE studentId = :studentId ORDER BY positionNumber ASC")
    fun getPositionsByStudentId(studentId: Long): Flow<List<Position>>

    @Query("SELECT * FROM positions WHERE studentId = :studentId ORDER BY positionNumber ASC")
    suspend fun getPositionsByStudentIdSync(studentId: Long): List<Position>

    @Query("SELECT COUNT(*) FROM positions WHERE studentId = :studentId AND grade != -1")
    suspend fun getEvaluatedCount(studentId: Long): Int

    @Query("SELECT COUNT(*) FROM positions WHERE studentId = :studentId")
    suspend fun getTotalCount(studentId: Long): Int

    @Query("SELECT AVG(grade) FROM positions WHERE studentId = :studentId AND grade != -1")
    suspend fun getProvisionalAverage(studentId: Long): Double?

    @Query("SELECT * FROM positions WHERE studentId = :studentId AND positionNumber = :positionNumber")
    suspend fun getPosition(studentId: Long, positionNumber: Int): Position?
}