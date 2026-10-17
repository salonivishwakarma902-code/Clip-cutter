package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CutJobDao {

    @Query("SELECT * FROM cut_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<CutJobEntity>>

    @Query("SELECT * FROM cut_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Long): CutJobEntity?

    @Query("SELECT * FROM cut_jobs WHERE status IN ('SCANNING', 'ANALYZING', 'CUTTING', 'VERIFYING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveJob(): CutJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: CutJobEntity): Long

    @Update
    suspend fun updateJob(job: CutJobEntity)

    @Query("DELETE FROM cut_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: Long)

    @Query("DELETE FROM cut_jobs")
    suspend fun clearAllJobs()
}
