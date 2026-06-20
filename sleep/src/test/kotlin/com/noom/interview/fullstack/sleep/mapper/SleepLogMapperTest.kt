package com.noom.interview.fullstack.sleep.mapper

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class SleepLogMapperTest {

    private val mapper = SleepLogMapper()

    @Test
    fun `toEntity should map request to domain model and accurately calculate duration within the same day`() {
        val request = CreateSleepLogRequest(
            userId = "user_789",
            sleepDate = LocalDate.of(2026, 6, 20),
            bedtime = LocalTime.of(1, 0, 0),  // 1:00 AM
            wakeTime = LocalTime.of(7, 30, 0), // 7:30 AM
            morningFeeling = MorningFeeling.GOOD
        )

        val entity = mapper.toEntity(request)

        assertEquals("user_789", entity.userId)
        assertEquals(LocalDate.of(2026, 6, 20), entity.sleepDate)
        assertEquals(LocalTime.of(1, 0, 0), entity.bedtime)
        assertEquals(LocalTime.of(7, 30, 0), entity.wakeTime)
        assertEquals(MorningFeeling.GOOD, entity.morningFeeling)

        assertEquals(390, entity.totalTimeInBedMinutes)
    }

    @Test
    fun `toEntity should correctly compute elapsed duration across a midnight rollover boundary`() {
        val request = CreateSleepLogRequest(
            userId = "user_overnight",
            sleepDate = LocalDate.of(2026, 6, 20),
            bedtime = LocalTime.of(22, 0, 0), // 10:00 PM (Previous night)
            wakeTime = LocalTime.of(6, 0, 0),  // 6:00 AM (Next morning)
            morningFeeling = MorningFeeling.OK
        )

        val entity = mapper.toEntity(request)

        assertEquals(480, entity.totalTimeInBedMinutes)
    }

    @Test
    fun `toResponseDto should map domain record details flawlessly to downstream presentation contract`() {
        val entity = SleepLog(
            id = 555L,
            userId = "user_presentation",
            sleepDate = LocalDate.of(2026, 6, 20),
            bedtime = LocalTime.of(23, 15, 0),
            wakeTime = LocalTime.of(7, 15, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.GOOD
        )

        val dto = mapper.toResponseDto(entity)

        assertEquals(555L, dto.id)
        assertEquals("user_presentation", dto.userId)
        assertEquals(LocalDate.of(2026, 6, 20), dto.sleepDate)
        assertEquals(LocalTime.of(23, 15, 0), dto.bedtime)
        assertEquals(LocalTime.of(7, 15, 0), dto.wakeTime)
        assertEquals(480, dto.totalTimeInBedMinutes)
        assertEquals(MorningFeeling.GOOD, dto.morningFeeling)
    }

    @Test
    fun `toResponseDto should throw IllegalStateException when processing transient entity with unassigned id`() {
        val transientEntity = SleepLog(
            id = null,
            userId = "transient_user",
            sleepDate = LocalDate.now(),
            bedtime = LocalTime.of(22, 0),
            wakeTime = LocalTime.of(6, 0),
            totalTimeInBedMinutes = 480,
            morningFeeling = MorningFeeling.OK
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            mapper.toResponseDto(transientEntity)
        }

        assertEquals("Target database entity was missing a generated identity key", exception.message)
    }
}