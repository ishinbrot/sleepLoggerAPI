package com.noom.interview.fullstack.sleep

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.dto.SleepLogResponse
import com.noom.interview.fullstack.sleep.service.SleepService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/sleep")
@Tag(name = "Sleep Tracking", description = "Endpoints for logging and analyzing user sleep data")
class SleepController(private val sleepService: SleepService) {
    private val log = LoggerFactory.getLogger(SleepController::class.java)

    @PostMapping
    @Operation(
        summary = "Log a new sleep entry",
        description = "Creates a daily sleep log record for a user. Ensures only one log exists per user per date.",
        responses = [
            ApiResponse(
                responseCode = "201", description = "Sleep log successfully created",
                content = [Content(schema = Schema(implementation = SleepLogResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid request payload or duplicate entry date"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun createSleepLog(
        @RequestBody request: CreateSleepLogRequest
    ): ResponseEntity<SleepLogResponse> {
            val savedLog = sleepService.createLog(request)

            // Map the saved database entity back to your clean response DTO
            val responseBody = SleepLogResponse(
                id = savedLog.id ?: throw IllegalStateException("Entity ID was not generated"),
                userId = savedLog.userId,
                sleepDate = savedLog.sleepDate,
                bedtime = savedLog.bedtime,
                wakeTime = savedLog.wakeTime,
                totalTimeInBedMinutes = savedLog.totalTimeInBedMinutes,
                morningFeeling = savedLog.morningFeeling
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
    }
    @GetMapping("/user/{userId}/analytics")
    @Operation(
        summary = "Get 30-day sleep averages",
        description = "Retrieves the historical moving average metrics of time spent in bed across the trailing 30 days.",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Analytics calculated successfully",
                content = [Content(schema = Schema(implementation = SleepAnalyticsResponse::class))]
            ),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getSleepAnalytics(
        @PathVariable userId: String
    ): ResponseEntity<SleepAnalyticsResponse> {
        log.debug("Received analytics request endpoint for userId: {}", userId)
        val analytics = sleepService.getThirtyDayAnalytics(userId)
        return ResponseEntity.ok(analytics)
    }
}