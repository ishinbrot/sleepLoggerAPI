package com.noom.interview.fullstack.sleep.dto


import com.noom.interview.fullstack.sleep.model.MorningFeeling
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "The outgoing serialized API response schema for a confirmed sleep log")
data class SleepLogResponse(
    val id: Long,
    val userId: String,
    val sleepDate: LocalDate,
    val bedtime: LocalTime,
    val wakeTime: LocalTime,
    val totalTimeInBedMinutes: Int,
    val morningFeeling: MorningFeeling
)