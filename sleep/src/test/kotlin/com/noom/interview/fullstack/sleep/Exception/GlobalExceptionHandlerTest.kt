package com.noom.interview.fullstack.sleep.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.noom.interview.fullstack.sleep.SleepController
import com.noom.interview.fullstack.sleep.mapper.SleepLogMapper
import com.noom.interview.fullstack.sleep.service.SleepService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SleepController::class)
class GlobalExceptionHandlerTest @Autowired constructor(
    val mockMvc: MockMvc
) {

    @MockBean
    lateinit var sleepService: SleepService
    @MockBean
    lateinit var sleepLogMapper: SleepLogMapper
    @Test
    fun `should handle IllegalArgumentException and format structured ApiError`() {
        val userId = "user_123"
        val exceptionMessage = "Duplicate entry for this date"

        `when`(sleepService.getThirtyDayAnalytics(userId))
            .thenThrow(IllegalArgumentException(exceptionMessage))

        mockMvc.perform(get("/api/v1/sleep/user/$userId/analytics"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(exceptionMessage))
            .andExpect(jsonPath("$.path").value("/api/v1/sleep/user/$userId/analytics"))
    }

    // =========================================================================
    // BRANCH 2a: HttpMessageNotReadableException with MismatchedInputException
    // =========================================================================
    @Test
    fun `should handle HttpMessageNotReadableException with MismatchedInputException for bad format`() {
        // Build mock Jackson path references to match your joinToString string building logic
        val mockReference = JsonMappingException.Reference(null, "bedtime")
        val mockCause = MismatchedInputException.from(null, String::class.java, "Bad time string format")
        mockCause.prependPath(mockReference)

        val targetException = HttpMessageNotReadableException(
            "Mismatched input error",
            mockCause,
            MockHttpInputMessage(ByteArray(0))
        )

        // Force controller entry point to throw this framework level exception
        `when`(sleepService.getThirtyDayAnalytics("user_123")).thenThrow(targetException)

        mockMvc.perform(get("/api/v1/sleep/user/user_123/analytics"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Malformed Payload"))
            .andExpect(jsonPath("$.message").value("Invalid format for field 'bedtime'. Please ensure dates match 'yyyy-MM-dd' and times match 'HH:mm:ss' strings."))
    }

    @Test
    fun `should handle HttpMessageNotReadableException with generic cause for generic bad payload`() {
        val targetException = HttpMessageNotReadableException(
            "Generic bad payload JSON framework error",
            RuntimeException("Generic raw parser crash"),
            MockHttpInputMessage(ByteArray(0))
        )

        `when`(sleepService.getThirtyDayAnalytics("user_123")).thenThrow(targetException)

        mockMvc.perform(get("/api/v1/sleep/user/user_123/analytics"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Malformed JSON request payload body structure."))
    }
    @Test
    fun `should handle unexpected Exception and return 500 Internal Server Error`() {
        val userId = "user_123"

        `when`(sleepService.getThirtyDayAnalytics(userId))
            .thenThrow(RuntimeException("Database connection dropped down unexpectedly!"))

        mockMvc.perform(get("/api/v1/sleep/user/$userId/analytics"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred while processing your request."))
    }
}