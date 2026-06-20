package com.noom.interview.fullstack.sleep.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@Service
class SleepService(private val sleepLogRepository: SleepLogRepository) {
    private val log = LoggerFactory.getLogger(SleepService::class.java)

    @Transactional(readOnly = true)
    fun getLogsForUser(userId: String): List<SleepLog> {
        return sleepLogRepository.findByUserIdOrderBySleepDateDesc(userId)
    }

    @Transactional
    fun createLog(request: CreateSleepLogRequest): SleepLog {
        log.info("Attempting to create sleep log for user: {} on date: {}", request.userId, request.sleepDate)

        if (sleepLogRepository.existsByUserIdAndSleepDate(request.userId, request.sleepDate)) {
            throw IllegalArgumentException("A sleep log entry already exists for user ${request.userId} on date ${request.sleepDate}")
        }

        // 2. Automatically calculate total time spent in bed
        val totalMinutes = calculateMinutesInBed(request.bedtime, request.wakeTime)

        // 3. Map DTO to Entity structure
        val sleepLog = SleepLog(
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = totalMinutes,
            morningFeeling = request.morningFeeling,
            createdAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"))
        )

        val savedLog =  sleepLogRepository.save(sleepLog)
        log.info("Successfully saved sleep log with ID: {} for user: {}", savedLog.id, savedLog.userId)
        return savedLog
    }

    private fun calculateMinutesInBed(bedtime: java.time.LocalTime, wakeTime: java.time.LocalTime): Int {
        val duration = Duration.between(bedtime, wakeTime)
        val minutes = duration.toMinutes()

        // Handle overnight sleep spanning past midnight (e.g., bedtime 10 PM, wake time 6 AM)
        return if (minutes < 0) {
            (minutes + Duration.ofDays(1).toMinutes()).toInt()
        } else {
            minutes.toInt()
        }
    }
    fun getThirtyDayAnalytics(userId: String): SleepAnalyticsResponse {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)

        log.info("Fetching 30-day sleep analytics for userId: {} between {} and {}", userId, startDate, endDate)
        val logs = sleepLogRepository.findByUserIdAndSleepDateGreaterThanEqual(userId, startDate)

        if (logs.isEmpty()) {
            log.info("No sleep logs found for userId: {} within the 30-day window. Returning empty analytics template.", userId)
            return SleepAnalyticsResponse(
                rangeStart = startDate,
                rangeEnd = endDate,
                averageTotalTimeInBedMinutes = 0.0,
                averageBedtime = null,
                averageWakeTime = null,
                feelingFrequencies = mapOf("BAD" to 0, "OK" to 0, "GOOD" to 0),
                null
            )
        }
        val latestLogId = logs.maxByOrNull { it.sleepDate }?.id
        log.debug("Successfully retrieved {} sleep log records for processing for userId: {}", logs.size, userId)
        val avgTimeInBed = logs.map { it.totalTimeInBedMinutes }.average()

        val avgBedtimeSeconds = logs.map { it.bedtime.toSecondOfDay() }.average().toLong()
        val avgWakeTimeSeconds = logs.map { it.wakeTime.toSecondOfDay() }.average().toLong()

        val initialFrequencies = mutableMapOf("BAD" to 0, "OK" to 0, "GOOD" to 0)
        logs.groupBy { it.morningFeeling }.forEach { (feeling, entries) ->
            initialFrequencies[feeling.name] = entries.size
        }
        log.debug("Analytics computed for user: {}. AvgMinutes: {}, FeelingDistribution: {}",
            userId, avgTimeInBed, initialFrequencies)
        return SleepAnalyticsResponse(
            rangeStart = startDate,
            rangeEnd = endDate,
            averageTotalTimeInBedMinutes = avgTimeInBed,
            averageBedtime = LocalTime.ofSecondOfDay(avgBedtimeSeconds),
            averageWakeTime = LocalTime.ofSecondOfDay(avgWakeTimeSeconds),
            feelingFrequencies = initialFrequencies,
            latestSleepLogId = latestLogId
        )
    }
    fun getLogById(id: Long): SleepLog {
        log.info("Fetching single sleep log record for id: {}", id)
        return sleepLogRepository.findById(id)
            .orElseThrow { NoSuchElementException("Sleep log entry not found with ID: $id") }
    }
}