package com.noom.interview.fullstack.sleep.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "sleep_log",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_user_sleep_date", columnNames = ["user_id", "sleep_date"])
    ],
    indexes = [
        Index(name = "idx_sleep_log_user_date", columnList = "user_id, sleep_date DESC")
    ]
)
class SleepLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "sleep_date", nullable = false)
    val sleepDate: LocalDate,

    @Column(name = "bedtime", nullable = false)
    val bedtime: LocalTime,

    @Column(name = "wake_time", nullable = false)
    val wakeTime: LocalTime,

    @Column(name = "total_time_in_bed_minutes", nullable = false)
    val totalTimeInBedMinutes: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "morning_feeling", nullable = false, length = 10)
    val morningFeeling: MorningFeeling,

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    val createdAt: ZonedDateTime = ZonedDateTime.now(java.time.ZoneId.of("UTC")))