package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
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
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class SleepServiceTest {

    @Mock
    lateinit var sleepLogRepository: SleepLogRepository

    @InjectMocks
    lateinit var sleepService: SleepService

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
        `when`(sleepLogRepository.save(org.mockito.ArgumentMatchers.any(SleepLog::class.java))).thenReturn(mockSavedLog)

        // Act
        val result = sleepService.createLog(request)

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
        `when`(sleepLogRepository.save(org.mockito.ArgumentMatchers.any(SleepLog::class.java))).thenReturn(mockSavedLog)

        // Act
        val result = sleepService.createLog(request)

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
            sleepService.createLog(request)
        }

        assertEquals("A sleep log entry already exists for user user_123abc on date 2026-06-19", exception.message)
    }
}