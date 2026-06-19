package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.LocalTime

@DataJpaTest
class SleepLogRepositoryTest @Autowired constructor(
    val sleepLogRepository: SleepLogRepository
) {

    @Test
    fun `existsByUserIdAndSleepDate should return true if entry matches`() {
        // Arrange
        val userId = "user_test"
        val date = LocalDate.of(2026, 6, 19)
        val log = SleepLog(
            userId = userId,
            sleepDate = date,
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.OK,
            createdAt = java.time.ZonedDateTime.now() // Satisfies the database constraint
        )
        sleepLogRepository.save(log)

        // Act
        val exists = sleepLogRepository.existsByUserIdAndSleepDate(userId, date)

        // Assert
        assertTrue(exists)
    }

    @Test
    fun `database should block identical duplicate dates for same user`() {
        // Arrange
        val userId = "user_block_test"
        val date = LocalDate.of(2026, 6, 19)

        val log1 = SleepLog(
            userId = userId,
            sleepDate = date,
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.OK,
            createdAt = java.time.ZonedDateTime.now() // Satisfies the database constraint
        )

        val log2 = SleepLog(
            userId = userId,
            sleepDate = date,
            bedtime = LocalTime.of(23, 0),
            wakeTime = LocalTime.of(7, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD,
            createdAt = java.time.ZonedDateTime.now() // Satisfies the database constraint
        )
        // Act & Assert
        sleepLogRepository.save(log1)

        // This forces Hibernate to immediately flush the SQL transaction to H2, triggering the unique check
        assertThrows<DataIntegrityViolationException> {
            sleepLogRepository.saveAndFlush(log2)
        }
    }
}