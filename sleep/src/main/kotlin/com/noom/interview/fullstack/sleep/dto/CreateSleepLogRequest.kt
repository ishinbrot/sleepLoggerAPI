package com.noom.interview.fullstack.sleep.dto

import com.noom.interview.fullstack.sleep.model.MorningFeeling
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Payload for creating a new daily sleep log")
data class CreateSleepLogRequest(
    @get:Schema(description = "Unique identifier for the user", example = "user_123abc", required = true)
    val userId: String,

    @get:Schema(description = "The calendar date the user went to sleep", example = "2026-06-19", required = true)
    val sleepDate: LocalDate,

    @get:Schema(description = "Time the user got into bed", example = "22:30:00", required = true)
    val bedtime: LocalTime,

    @get:Schema(description = "Time the user woke up", example = "06:45:00", required = true)
    val wakeTime: LocalTime,

    @get:Schema(description = "User's qualitative reflection of morning energy status", example = "GOOD", required = true)
    val morningFeeling: MorningFeeling
)