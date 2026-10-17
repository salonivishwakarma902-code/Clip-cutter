package com.example.data.repository

import com.example.data.db.CutJobDao
import com.example.data.db.CutJobEntity
import com.example.data.db.JobStatus
import kotlinx.coroutines.flow.Flow

class JobRepository(private val dao: CutJobDao) {

    val allJobs: Flow<List<CutJobEntity>> = dao.getAllJobs()

    suspend fun getJobById(jobId: Long): CutJobEntity? = dao.getJobById(jobId)

    suspend fun getActiveJob(): CutJobEntity? = dao.getActiveJob()

    suspend fun insertJob(
        sourceUri: String,
        sourceFileName: String,
        sourceDurationMs: Long,
        clipStartMs: Long,
        clipEndMs: Long,
        outputFileName: String,
        isReencodeMode: Boolean
    ): Long {
        val job = CutJobEntity(
            sourceUri = sourceUri,
            sourceFileName = sourceFileName,
            sourceDurationMs = sourceDurationMs,
            clipStartMs = clipStartMs,
            clipEndMs = clipEndMs,
            outputFileName = outputFileName,
            isReencodeMode = isReencodeMode,
            status = JobStatus.QUEUED,
            progressPercent = 0,
            currentStageMessage = "Queued for export"
        )
        return dao.insertJob(job)
    }

    suspend fun updateProgress(jobId: Long, progressPercent: Int, stageMessage: String, status: JobStatus) {
        val existing = dao.getJobById(jobId) ?: return
        val updated = existing.copy(
            progressPercent = progressPercent,
            currentStageMessage = stageMessage,
            status = status
        )
        dao.updateJob(updated)
    }

    suspend fun markCompleted(jobId: Long, outputUri: String, outputSizeBytes: Long) {
        val existing = dao.getJobById(jobId) ?: return
        val updated = existing.copy(
            status = JobStatus.COMPLETED,
            progressPercent = 100,
            currentStageMessage = "Verified and exported successfully / सफलतापूर्वक निर्यातित",
            outputUri = outputUri,
            outputSizeBytes = outputSizeBytes,
            completedAt = System.currentTimeMillis()
        )
        dao.updateJob(updated)
    }

    suspend fun markFailed(jobId: Long, errorMessage: String) {
        val existing = dao.getJobById(jobId) ?: return
        val updated = existing.copy(
            status = JobStatus.FAILED,
            errorMessage = errorMessage,
            currentStageMessage = "Export failed / निर्यात विफल: $errorMessage"
        )
        dao.updateJob(updated)
    }

    suspend fun deleteJob(jobId: Long) = dao.deleteJob(jobId)

    suspend fun clearAll() = dao.clearAllJobs()
}
