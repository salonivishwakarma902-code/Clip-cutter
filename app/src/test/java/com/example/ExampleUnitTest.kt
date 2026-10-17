package com.example

import com.example.data.ai.ClipProposal
import com.example.data.ai.EditPlan
import com.example.data.media.VideoMetadata
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVideoMetadataFormattedDuration() {
        val metadata = VideoMetadata(
            uriString = "content://media/external/video/media/1",
            fileName = "sample_clip.mp4",
            durationMs = 125450L, // 2 minutes, 5 seconds, 450 ms
            width = 1920,
            height = 1080,
            rotation = 0,
            bitrate = 5000000L,
            mimeType = "video/mp4",
            sizeBytes = 25000000L,
            hasAudioTrack = true,
            hasVideoTrack = true
        )

        assertEquals("02:05.450", metadata.formattedDuration())
    }

    @Test
    fun testClipProposalTimeRangeFormatting() {
        val proposal = ClipProposal(
            id = "clip_1",
            title = "Intro Scene",
            startMs = 15000L, // 00:15
            endMs = 45000L,  // 00:45
            rationale = "Opening highlight",
            confidence = 0.95f
        )

        assertEquals("00:15 - 00:45", proposal.formattedTimeRange())
    }

    @Test
    fun testEditPlanJsonParsing() {
        val json = """
            {
              "planId": "plan_99",
              "taskDescription": "Cut opening 20 seconds",
              "suggestedClips": [
                {
                  "id": "c1",
                  "title": "Opening Clip",
                  "startMs": 0,
                  "endMs": 20000,
                  "rationale": "High energy intro",
                  "confidence": 0.92
                }
              ],
              "requiresClarification": false,
              "clarificationPrompt": null
            }
        """.trimIndent()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EditPlan::class.java)

        val plan = adapter.fromJson(json)
        assertNotNull(plan)
        assertEquals("plan_99", plan?.planId)
        assertEquals(1, plan?.suggestedClips?.size)
        assertEquals(0L, plan?.suggestedClips?.get(0)?.startMs)
        assertEquals(20000L, plan?.suggestedClips?.get(0)?.endMs)
    }
}
