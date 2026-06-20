package com.noom.interview.fullstack.sleep.exception

import java.time.ZonedDateTime

data class ApiError(
    val status: Int,
    val error: String,
    val message: String?,
    val path: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
)