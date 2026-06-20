package com.noom.interview.fullstack.sleep.mapper

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepLogResponse
import com.noom.interview.fullstack.sleep.model.SleepLog
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SleepLogMapper {

    /**
     * Maps an incoming request payload to a fresh domain database entity.
     * It also isolates the core mathematical formula for calculating total duration.
     */
    fun toEntity(request: CreateSleepLogRequest): SleepLog {
        // Calculate the total duration in minutes between bedtime and wake time
        val rawMinutes = Duration.between(request.bedtime, request.wakeTime).toMinutes().toInt()

        // Handle roll-over if the user slept past midnight
        val adjustedMinutes = if (rawMinutes < 0) rawMinutes + 1440 else rawMinutes

        return SleepLog(
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = adjustedMinutes,
            morningFeeling = request.morningFeeling
        )
    }

    /**
     * Maps a database persistence entity cleanly over to a public response presentation DTO.
     */
    fun toResponseDto(entity: SleepLog): SleepLogResponse {
        return SleepLogResponse(
            id = entity.id
                ?: throw IllegalStateException("Target database entity was missing a generated identity key"),
            userId = entity.userId,
            sleepDate = entity.sleepDate,
            bedtime = entity.bedtime,
            wakeTime = entity.wakeTime,
            totalTimeInBedMinutes = entity.totalTimeInBedMinutes,
            morningFeeling = entity.morningFeeling
        )
    }
}