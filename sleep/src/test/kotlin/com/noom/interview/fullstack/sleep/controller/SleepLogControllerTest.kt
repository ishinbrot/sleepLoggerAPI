package com.noom.interview.fullstack.sleep.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.noom.interview.fullstack.sleep.SleepLogController
import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.mapper.SleepLogMapper
import com.noom.interview.fullstack.sleep.dto.SleepLogResponse
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.service.SleepLogService
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

@WebMvcTest(SleepLogController::class)
class SleepLogControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    @MockBean
    lateinit var sleepLogService: SleepLogService
    @MockBean
    lateinit var sleepLogMapper: SleepLogMapper

    @Test
    fun `POST sleep entry should return 201 Created and map properties correctly`() {
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
        val expectedResponseDto = SleepLogResponse(
            id = 99L,
            userId = "user_456",
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 495,
            morningFeeling = request.morningFeeling
        )

        `when`(sleepLogService.createLog(request)).thenReturn(mockSavedEntity)
        `when`(sleepLogMapper.toResponseDto(mockSavedEntity)).thenReturn(expectedResponseDto)
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
        `when`(sleepLogService.createLog(request)).thenThrow(IllegalArgumentException(exceptionMessage))

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
            feelingFrequencies = mapOf("BAD" to 1, "OK" to 3, "GOOD" to 14),
            latestSleepLogId = 99L
        )

        `when`(sleepLogService.getThirtyDayAnalytics(userId)).thenReturn(mockAnalytics)

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

    @Test
    fun `GET single log by ID should return 200 OK and match individual parameters`() {
        val request = CreateSleepLogRequest(
            userId = "user_456",
            sleepDate = LocalDate.of(2026, 6, 19),
            bedtime = LocalTime.of(22, 30, 0),
            wakeTime = LocalTime.of(6, 45, 0),
            morningFeeling = MorningFeeling.GOOD
        )
        val targetId = 99L
        val mockSavedEntity = SleepLog(
            id = 99L,
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 495,
            morningFeeling = request.morningFeeling
        )
        val expectedResponseDto = SleepLogResponse(
            id = 99L,
            userId = "user_456",
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 495,
            morningFeeling = request.morningFeeling
        )

        `when`(sleepLogService.getLogById(targetId)).thenReturn(mockSavedEntity)
        `when`(sleepLogMapper.toResponseDto(mockSavedEntity)).thenReturn(expectedResponseDto)
        mockMvc.perform(
            get("/api/v1/sleep/$targetId")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(targetId))
            .andExpect(jsonPath("$.userId").value("user_456"))
            .andExpect(jsonPath("$.morningFeeling").value("GOOD"))
    }

    @Test
    fun `GET single log by ID should return 404 Not Found when entity does not exist`() {
        val nonExistentId = 999L
        val errorMessage = "Sleep log entry not found with ID: $nonExistentId"

        `when`(sleepLogService.getLogById(nonExistentId))
            .thenThrow(NoSuchElementException(errorMessage))

        mockMvc.perform(
            get("/api/v1/sleep/$nonExistentId")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }
    @Test
    fun `GET last night sleep profile should return 200 OK and match raw data payload`() {
        val userId = "user_456"
        val mockEntity = SleepLog(
            id = 99L,
            userId = userId,
            sleepDate = LocalDate.of(2026, 6, 20),
            bedtime = LocalTime.of(22, 0, 0),
            wakeTime = LocalTime.of(6, 0, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD
        )

        val expectedResponseDto = SleepLogResponse(
            id = 99L,
            userId = userId,
            sleepDate = mockEntity.sleepDate,
            bedtime = mockEntity.bedtime,
            wakeTime = mockEntity.wakeTime,
            totalTimeInBedMinutes = 480,
            morningFeeling = mockEntity.morningFeeling
        )

        `when`(sleepLogService.getLastNightsSleep(userId)).thenReturn(mockEntity)
        `when`(sleepLogMapper.toResponseDto(mockEntity)).thenReturn(expectedResponseDto)

        mockMvc.perform(
            get("/api/v1/sleep/user/$userId/last-night")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.totalTimeInBedMinutes").value(480))
            .andExpect(jsonPath("$.morningFeeling").value("GOOD"))
    }

    @Test
    fun `GET user history should return 200 OK and match serialized collection array`() {
        val userId = "user_456"
        val mockEntityList = listOf(
            SleepLog(
                id = 101L,
                userId = userId,
                sleepDate = LocalDate.of(2026, 6, 20),
                bedtime = LocalTime.of(22, 0),
                wakeTime = LocalTime.of(6, 0),
                totalTimeInBedMinutes = 480,
                morningFeeling = MorningFeeling.GOOD
            ),
            SleepLog(
                id = 100L,
                userId = userId,
                sleepDate = LocalDate.of(2026, 6, 19),
                bedtime = LocalTime.of(23, 0),
                wakeTime = LocalTime.of(7, 0),
                totalTimeInBedMinutes = 480,
                morningFeeling = MorningFeeling.OK
            )
        )

        val expectedDtoList = listOf(
            SleepLogResponse(
                id = 101L, userId = userId, sleepDate = mockEntityList[0].sleepDate,
                bedtime = mockEntityList[0].bedtime, wakeTime = mockEntityList[0].wakeTime,
                totalTimeInBedMinutes = 480, morningFeeling = MorningFeeling.GOOD
            ),
            SleepLogResponse(
                id = 100L, userId = userId, sleepDate = mockEntityList[1].sleepDate,
                bedtime = mockEntityList[1].bedtime, wakeTime = mockEntityList[1].wakeTime,
                totalTimeInBedMinutes = 480, morningFeeling = MorningFeeling.OK
            )
        )

        `when`(sleepLogService.getSleepLogsForUser(userId)).thenReturn(mockEntityList)
        `when`(sleepLogMapper.toResponseDto(mockEntityList[0])).thenReturn(expectedDtoList[0])
        `when`(sleepLogMapper.toResponseDto(mockEntityList[1])).thenReturn(expectedDtoList[1])

        mockMvc.perform(
            get("/api/v1/sleep/user/$userId/history")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(101))
            .andExpect(jsonPath("$[0].morningFeeling").value("GOOD"))
            .andExpect(jsonPath("$[1].id").value(100))
            .andExpect(jsonPath("$[1].morningFeeling").value("OK"))
    }

    @Test
    fun `GET user history should return 200 OK and an empty list when no history exists`() {
        val userId = "new_user_empty"
        `when`(sleepLogService.getSleepLogsForUser(userId)).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/sleep/user/$userId/history")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

}