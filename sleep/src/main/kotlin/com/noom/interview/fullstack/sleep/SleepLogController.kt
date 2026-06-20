package com.noom.interview.fullstack.sleep

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.mapper.SleepLogMapper
import com.noom.interview.fullstack.sleep.dto.SleepLogResponse
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.service.SleepLogService
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
class SleepLogController(
    private val sleepLogService: SleepLogService,
    private val sleepLogMapper: SleepLogMapper)  {
    private val log = LoggerFactory.getLogger(SleepLogController::class.java)
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
            val savedLog = sleepLogService.createLog(request)

            return ResponseEntity.status(HttpStatus.CREATED).body(sleepLogMapper.toResponseDto(savedLog))
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
        val analytics = sleepLogService.getThirtyDayAnalytics(userId)
        return ResponseEntity.ok(analytics)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Switch back to single sleep log view",
        description = "Retrieves granular historical data for an individual night using the navigation hook ID returned by the analytics payload.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Single sleep log instance located successfully",
                content = [Content(schema = Schema(implementation = SleepLog::class))]
            ),
            ApiResponse(responseCode = "404", description = "Target sleep log reference not found"),
            ApiResponse(responseCode = "500", description = "Internal server error state")
        ]
    )
    fun getSleepLogById(
        @Parameter(description = "The target log identifier (e.g., extracted from latestSleepLogId)", example = "42")
        @PathVariable id: Long
    ): ResponseEntity<SleepLogResponse> {
        log.info("Received request for single sleep log view, ID: {}", id)
        val sleepLog = sleepLogService.getLogById(id)
        return ResponseEntity.ok(sleepLogMapper.toResponseDto(sleepLog))
    }
    @GetMapping("/user/{userId}/last-night")
    @Operation(
        summary = "Fetch information about last night's sleep",
        description = "Requirement #2: Retrieves the single most recent sleep log entry recorded by the user.",
        responses = [
            ApiResponse(
                responseCode = "200", description = "Last night's log located successfully",
                content = [Content(schema = Schema(implementation = SleepLogResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "No history profile found for target user")
        ]
    )
    fun getLastNightsSleep(
        @PathVariable userId: String
    ): ResponseEntity<SleepLogResponse> {
        log.debug("Fetching last night's profile for userId: {}", userId)
        val savedLog = sleepLogService.getLastNightsSleep(userId)
        return ResponseEntity.ok(sleepLogMapper.toResponseDto(savedLog))
    }
}