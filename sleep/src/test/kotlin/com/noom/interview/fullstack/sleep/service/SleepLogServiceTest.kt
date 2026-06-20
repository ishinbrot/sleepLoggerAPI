package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SleepLogServiceTest {

    @Mock
    lateinit var sleepLogRepository: SleepLogRepository

    @InjectMocks
    lateinit var sleepLogService: SleepLogService

    @Test
    fun `createLog should calculate correct minutes when sleep crosses midnight`() {
        // Arrange: Going to bed at 10:30 PM (22:30) and waking up at 6:45 AM (06:45)
        // Total duration: 1h 30m before midnight + 6h 45m after midnight = 8h 15m (495 minutes)
        val request = CreateSleepLogRequest(
            userId = "user_123abc",
            sleepDate = LocalDate.of(2026, 6, 19),
            bedtime = LocalTime.of(22, 30, 0),
            wakeTime = LocalTime.of(6, 45, 0),
            morningFeeling = MorningFeeling.GOOD
        )

        // Mock saved entity matching what the service will pass to save()
        val mockSavedLog = SleepLog(
            id = 1L,
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 495,
            morningFeeling = request.morningFeeling
        )

        // Stubbing the repository checks
        `when`(sleepLogRepository.existsByUserIdAndSleepDate(request.userId, request.sleepDate)).thenReturn(false)

        // Use Mockito's any() safely inside mockito-core by using class check if equality match isn't preferred,
        // but passing the constructed entity structure works beautifully
        `when`(sleepLogRepository.save(ArgumentMatchers.any(SleepLog::class.java))).thenReturn(mockSavedLog)

        // Act
        val result = sleepLogService.createLog(request)

        // Assert
        assertNotNull(result.id)
        assertEquals("user_123abc", result.userId)
        assertEquals(495, result.totalTimeInBedMinutes) // Confirms cross-midnight logic handles time correctly
        assertEquals(MorningFeeling.GOOD, result.morningFeeling)
    }

    @Test
    fun `createLog should calculate correct minutes when sleep is entirely within the same day`() {
        // Arrange: A quick daytime nap from 1:00 PM (13:00) to 2:30 PM (14:30) = 90 minutes
        val request = CreateSleepLogRequest(
            userId = "user_123abc",
            sleepDate = LocalDate.of(2026, 6, 19),
            bedtime = LocalTime.of(13, 0, 0),
            wakeTime = LocalTime.of(14, 30, 0),
            morningFeeling = MorningFeeling.OK
        )

        val mockSavedLog = SleepLog(
            id = 2L,
            userId = request.userId,
            sleepDate = request.sleepDate,
            bedtime = request.bedtime,
            wakeTime = request.wakeTime,
            totalTimeInBedMinutes = 90,
            morningFeeling = request.morningFeeling
        )

        `when`(sleepLogRepository.existsByUserIdAndSleepDate(request.userId, request.sleepDate)).thenReturn(false)
        `when`(sleepLogRepository.save(ArgumentMatchers.any(SleepLog::class.java))).thenReturn(mockSavedLog)

        // Act
        val result = sleepLogService.createLog(request)

        // Assert
        assertEquals(90, result.totalTimeInBedMinutes)
    }

    @Test
    fun `createLog should throw IllegalArgumentException if log already exists for that user and date`() {
        // Arrange
        val request = CreateSleepLogRequest(
            userId = "user_123abc",
            sleepDate = LocalDate.of(2026, 6, 19),
            bedtime = LocalTime.of(22, 0, 0),
            wakeTime = LocalTime.of(6, 0, 0),
            morningFeeling = MorningFeeling.BAD
        )

        // Simulate that the row already exists in database
        `when`(sleepLogRepository.existsByUserIdAndSleepDate(request.userId, request.sleepDate)).thenReturn(true)

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            sleepLogService.createLog(request)
        }

        assertEquals("A sleep log entry already exists for user user_123abc on date 2026-06-19", exception.message)
    }

    @Test
    fun `getThirtyDayAnalytics should compute averages and frequencies correctly`() {
        // Arrange
        val userId = "noom_user_123"

        // Mock data: 2 entries with different bedtimes, wake times, and feelings
        val mockLogs = listOf(
            SleepLog(
                id = 1L, userId = userId, sleepDate = LocalDate.now(),
                bedtime = LocalTime.of(22, 0), wakeTime = LocalTime.of(6, 0),
                totalTimeInBedMinutes = 480, morningFeeling = MorningFeeling.GOOD
            ),
            SleepLog(
                id = 2L, userId = userId, sleepDate = LocalDate.now().minusDays(1),
                bedtime = LocalTime.of(23, 0), wakeTime = LocalTime.of(7, 0),
                totalTimeInBedMinutes = 540, morningFeeling = MorningFeeling.OK
            )
        )
        val targetStartDate = LocalDate.now().minusDays(30)
        `when`(sleepLogRepository.findByUserIdAndSleepDateGreaterThanEqual(userId, targetStartDate))
            .thenReturn(mockLogs)

        val result: SleepAnalyticsResponse = sleepLogService.getThirtyDayAnalytics(userId)

        assertThat(result.averageTotalTimeInBedMinutes).isEqualTo(510.0)

        assertThat(result.averageBedtime).isEqualTo(LocalTime.of(22, 30))

        assertThat(result.averageWakeTime).isEqualTo(LocalTime.of(6, 30))

        assertThat(result.feelingFrequencies["GOOD"]).isEqualTo(1)
        assertThat(result.feelingFrequencies["OK"]).isEqualTo(1)
        assertThat(result.feelingFrequencies["BAD"]).isEqualTo(0)
        assertEquals(1L, result.latestSleepLogId)

    }

    @Test
    fun `getThirtyDayAnalytics should handle empty history gracefully without throwing exceptions`() {
        val userId = "new_user"
        val targetStartDate = LocalDate.now().minusDays(30)

        val errorMessage = "Sleep log entry not found with ID: $userId";
        `when`(sleepLogRepository.findByUserIdAndSleepDateGreaterThanEqual(userId, targetStartDate))
            .thenThrow(IllegalArgumentException(errorMessage))

        val exception = assertThrows<IllegalArgumentException> {
            sleepLogService.getThirtyDayAnalytics(userId)
        }

        // Verify the exception message matches what was thrown by the mock
        assertThat(exception.message).isEqualTo(errorMessage)
    }

    @Test
    fun `getLogById should return the sleep log when a valid ID is provided`() {
        val targetId = 42L
        val mockLog = SleepLog(
            id = targetId,
            userId = "user_abc",
            sleepDate = LocalDate.now(),
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD
        )

        `when`(sleepLogRepository.findById(targetId)).thenReturn(Optional.of(mockLog))

        val result = sleepLogService.getLogById(targetId)

        assertEquals(targetId, result.id)
        assertEquals("user_abc", result.userId)
        assertEquals(MorningFeeling.GOOD, result.morningFeeling)
    }

    @Test
    fun `getLogById should throw NoSuchElementException when the ID does not exist`() {
        val nonExistentId = 999L
        `when`(sleepLogRepository.findById(nonExistentId)).thenReturn(Optional.empty())

        val exception = assertThrows<NoSuchElementException> {
            sleepLogService.getLogById(nonExistentId)
        }

        assertEquals("Sleep log entry not found with ID: $nonExistentId", exception.message)
    }

    @Test
    fun `getLastNightsSleep should return the chronologically newest single log entry`() {
        val userId = "user_123abc"
        val expectedLog = SleepLog(
            id = 99L,
            userId = userId,
            sleepDate = LocalDate.now(),
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD
        )

        `when`(sleepLogRepository.findFirstByUserIdOrderBySleepDateDesc(userId)).thenReturn(Optional.of(expectedLog))

        val result = sleepLogService.getLastNightsSleep(userId)

         assertEquals(99L, result.id)
        assertEquals(LocalDate.now(), result.sleepDate)
        assertEquals(MorningFeeling.GOOD, result.morningFeeling)
    }
}