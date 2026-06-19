package com.noom.interview.fullstack.sleep

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class SleepController {

    @PostMapping("/api/sleep-logs")
    @ResponseStatus(HttpStatus.CREATED) // Returns a 201 Created status code upon success
    fun createSleepLog(@RequestBody request: CreateSleepLogRequest): SleepLogResponse {
        // TODO: Call your service layer here to save to Postgres, e.g.:
        // val savedLog = sleepService.saveLog(request)

        // Temporarily returning a dummy response matching your request format so it compiles
        return SleepLogResponse(
            id = 1L,
            startTime = request.startTime,
            endTime = request.endTime,
            notes = request.notes
        )
    }
}

// Data Transfer Object (DTO) for incoming JSON requests
data class CreateSleepLogRequest(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val notes: String?
)

// Data Transfer Object (DTO) for the outgoing API response
data class SleepLogResponse(
    val id: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val notes: String?
)