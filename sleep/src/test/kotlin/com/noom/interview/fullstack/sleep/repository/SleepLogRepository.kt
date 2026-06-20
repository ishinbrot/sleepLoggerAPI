package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])class SleepLogRepositoryTest @Autowired constructor(
    val sleepLogRepository: SleepLogRepository
) {

    @Test
    fun `existsByUserIdAndSleepDate should return true if entry matches`() {
        val userId = "user_test"
        val todaysDate = LocalDate.of(2026, 6, 19)
        val sleepLog = SleepLog(
            userId = userId,
            sleepDate = todaysDate,
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.OK,
            createdAt = ZonedDateTime.now() // Satisfies the database constraint
        )
        val olderLog = sleepLog.copy(
            sleepDate = todaysDate.minusDays(5),
            createdAt = ZonedDateTime.now()
        )
        sleepLogRepository.save(sleepLog)

        val exists = sleepLogRepository.existsByUserIdAndSleepDate(userId, todaysDate)



        assertTrue(exists)
    }

    @Test
    fun `database should block identical duplicate dates for same user`() {
        val userId = "user_block_test"
        val date = LocalDate.of(2026, 6, 19)

        val log1 = SleepLog(
            userId = userId,
            sleepDate = date,
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.OK,
            createdAt = ZonedDateTime.now()
        )

        val log2 = SleepLog(
            userId = userId,
            sleepDate = date,
            bedtime = LocalTime.of(23, 0),
            wakeTime = LocalTime.of(7, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD,
            createdAt = ZonedDateTime.now()
        )
        sleepLogRepository.save(log1)

        assertThrows<DataIntegrityViolationException> {
            sleepLogRepository.saveAndFlush(log2)
        }
    }
}