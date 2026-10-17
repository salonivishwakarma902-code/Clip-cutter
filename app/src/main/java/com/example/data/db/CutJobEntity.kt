package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class JobStatus {
    IDLE,
    QUEUED,
    VALIDATING,
    SCANNING,
    ANALYZING,
    AWAITING_CONFIRMATION,
    CUTTING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "cut_jobs")
data class CutJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUri: String,
    val sourceFileName: String,
    val sourceDurationMs: Long,
    val clipStartMs: Long,
    val clipEndMs: Long,
    val outputFileName: String,
    val outputUri: String? = null,
    val outputSizeBytes: Long = 0L,
    val status: JobStatus = JobStatus.IDLE,
    val progressPercent: Int = 0,
    val currentStageMessage: String = "",
    val errorMessage: String? = null,
    val aiReasoning: String? = null,
    val isReencodeMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
