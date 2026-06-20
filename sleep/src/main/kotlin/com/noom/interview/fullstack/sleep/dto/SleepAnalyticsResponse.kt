package com.noom.interview.fullstack.sleep.dto

import java.time.LocalDate
import java.time.LocalTime

/**
 * Data Transfer Object representing aggregated sleep metrics over a 30-day window.
 * Aligns precisely with the Noom functional specifications for tracking moving averages.
 */
data class SleepAnalyticsResponse(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val averageTotalTimeInBedMinutes: Double,
    val averageBedtime: LocalTime?,
    val averageWakeTime: LocalTime?,
    val feelingFrequencies: Map<String, Int>,
    val latestSleepLogId: Long?
)