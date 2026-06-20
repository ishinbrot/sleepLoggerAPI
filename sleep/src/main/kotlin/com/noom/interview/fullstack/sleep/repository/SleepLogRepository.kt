package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.model.SleepLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SleepLogRepository : JpaRepository<SleepLog, Long> {
    fun findByUserIdOrderBySleepDateDesc(userId: String): List<SleepLog>
    fun existsByUserIdAndSleepDate(userId: String, sleepDate: LocalDate): Boolean
    fun findByUserIdAndSleepDateGreaterThanEqual(userId: String, startDate: LocalDate): List<SleepLog>
}