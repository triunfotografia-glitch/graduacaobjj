package br.com.dojolb.graduacao.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.dojolb.graduacao.data.entity.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(student: Student): Long

    @Update
    suspend fun update(student: Student): Int

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM students ORDER BY createdAt DESC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): Student?

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudentById(id: Long): Flow<Student?>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentsCount(): Int
}