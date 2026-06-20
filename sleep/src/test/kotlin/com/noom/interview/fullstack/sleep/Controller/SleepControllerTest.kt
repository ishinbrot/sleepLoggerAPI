package com.noom.interview.fullstack.sleep.Controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.noom.interview.fullstack.sleep.SleepController
import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.service.SleepService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(SleepController::class)
class SleepControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    @MockBean
    lateinit var sleepService: SleepService

    @Test
    fun `POST sleep entry should return 201 Created and map properties correctly`() {
        // Arrange
        val request = CreateSleepLogRequest(
            userId = "user_456",
            sleepDate = LocalDate.of(2026, 6, 19),
            bedtime = LocalTime.of(22, 30, 0),
            wakeTime = LocalTime.of(6, 45, 0),
            morningFeeling = MorningFeeling.GOOD
        )

        val mockSavedEntity = SleepLog(
            id = 99L,
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 495, // 8h 15m
            morningFeeling = request.morningFeeling
        )

        `when`(sleepService.createLog(request)).thenReturn(mockSavedEntity)

        // Act & Assert
        mockMvc.perform(
            post("/api/v1/sleep")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.userId").value("user_456"))
            .andExpect(jsonPath("$.totalTimeInBedMinutes").value(495))
            .andExpect(jsonPath("$.morningFeeling").value("GOOD"))
    }

    @Test
    fun `POST sleep entry should bubble exceptions up to global advice handler`() {
        val request = CreateSleepLogRequest(
            userId = "user_duplicate",
            sleepDate = LocalDate.now(),
            bedtime = LocalTime.of(23, 0),
            wakeTime = LocalTime.of(7, 0),
            morningFeeling = MorningFeeling.OK
        )

        val exceptionMessage = "A sleep log entry already exists for user user_duplicate"
        `when`(sleepService.createLog(request)).thenThrow(IllegalArgumentException(exceptionMessage))

        mockMvc.perform(
            post("/api/v1/sleep")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
    @Test
    fun `GET sleep analytics should return 200 OK and calculated moving metrics`() {
        val userId = "user_456"
        val mockAnalytics = SleepAnalyticsResponse(
            rangeStart = LocalDate.now().minusDays(30),
            rangeEnd = LocalDate.now(),
            averageTotalTimeInBedMinutes = 480.5,
            averageBedtime = LocalTime.of(22, 30, 0),
            averageWakeTime = LocalTime.of(6, 30, 0),
            feelingFrequencies = mapOf("BAD" to 1, "OK" to 3, "GOOD" to 14)
        )

        `when`(sleepService.getThirtyDayAnalytics(userId)).thenReturn(mockAnalytics)

        // Act & Assert
        mockMvc.perform(
            get("/api/v1/sleep/user/$userId/analytics")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.averageTotalTimeInBedMinutes").value(480.5))
            .andExpect(jsonPath("$.averageBedtime").value("22:30:00"))
            .andExpect(jsonPath("$.averageWakeTime").value("06:30:00"))
            .andExpect(jsonPath("$.feelingFrequencies.GOOD").value(14))
            .andExpect(jsonPath("$.feelingFrequencies.BAD").value(1))
    }
}