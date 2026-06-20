package com.noom.interview.fullstack.sleep.service

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

/**
 * Service layer orchestrator responsible for executing business domain logic, validation checks,
 * and data aggregation computations surrounding user sleep telemetry logs.
 */
@Service
class SleepLogService(private val sleepLogRepository: SleepLogRepository) {
    private val log = LoggerFactory.getLogger(SleepLogService::class.java)

    /**
     * Retrieves all sleep log entries recorded for a target user, sorted chronologically descending.
     *
     * @param userId The unique identifier string of the target user profile.
     * @return A list of [SleepLog] entities associated with the user profile.
     */
    @Transactional(readOnly = true)
    fun getLogsForUser(userId: String): List<SleepLog> {
        return sleepLogRepository.findByUserIdOrderBySleepDateDesc(userId)
    }
    /**
     * Validates and provisions a new historical sleep log entry for a user profile.
     * Calculates the absolute elapsed minutes spent in bed, accounting for day boundaries.
     *
     * @param request The data transfer object containing entry parameters.
     * @return The persisted, database-managed [SleepLog] instance complete with identity key.
     * @throws IllegalArgumentException If a tracking record already exists for the user on the requested date.
     */
    @Transactional
    fun createLog(request: CreateSleepLogRequest): SleepLog {
        log.info("Attempting to create sleep log for user: {} on date: {}", request.userId, request.sleepDate)

        if (sleepLogRepository.existsByUserIdAndSleepDate(request.userId, request.sleepDate)) {
            throw IllegalArgumentException("A sleep log entry already exists for user ${request.userId} on date ${request.sleepDate}")
        }

        val totalMinutes = calculateMinutesInBed(request.bedtime, request.wakeTime)

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
    /**
     * Computes the total time spent in bed in minutes.
     *
     * This function handles cross-midnight sleep intervals (e.g., bedtime at 23:00
     * and wake time at 07:00) by calculating the wrap-around duration relative
     * to the daily chronological boundary.
     *
     * @param bedtime The localized time the user went to bed.
     * @param wakeTime The localized time the user woke up.
     * @return The absolute elapsed time spent in bed, represented in minutes.
     */
    private fun calculateMinutesInBed(bedtime: LocalTime, wakeTime: LocalTime): Int {
        val duration = Duration.between(bedtime, wakeTime)
        val minutes = duration.toMinutes()

        return if (minutes < 0) {
            (minutes + Duration.ofDays(1).toMinutes()).toInt()
        } else {
            minutes.toInt()
        }
    }
    /**
     * Aggregates statistical tracking metadata across a sliding historical 30-day window
     * relative to the execution day.
     *
     * Computes moving averages of total rest periods, localized time-of-day clock centers,
     * and maps categorical frequencies for subjective morning moods. Returns an empty baseline
     * matrix if no metrics are present within the range.
     *
     * @param userId The unique identifier string of the target user profile.
     * @return A structured [SleepAnalyticsResponse] compiling the aggregated data findings.
     */
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
    /**
     * Looks up an individual tracking entry matching an explicit unique database primary key.
     *
     * @param id The auto-incremented surrogate record key identifier.
     * @return The matching [SleepLog] record entity.
     * @throws NoSuchElementException If no table rows are indexed under the targeted tracking ID.
     */
    fun getLogById(id: Long): SleepLog {
        log.info("Fetching single sleep log record for id: {}", id)
        return sleepLogRepository.findById(id)
            .orElseThrow { NoSuchElementException("Sleep log entry not found with ID: $id") }
    }
    /**
     * Resolves and extracts the single chronologically newest log recorded by a user profile.
     * Maps precisely to core application Requirement #2 requirements.
     *
     * @param userId The unique identifier string of the target user profile.
     * @return The single latest [SleepLog] entry.
     * @throws NoSuchElementException If the user profile has not initialized any logs in the database.
     */
    fun getLastNightsSleep(userId: String): SleepLog {
        log.debug("Querying last nights sleep for user: {}", userId)
        return sleepLogRepository.findFirstByUserIdOrderBySleepDateDesc(userId)
            .orElseThrow { NoSuchElementException("No sleep history logs found with ID: $userId") }

    }
}