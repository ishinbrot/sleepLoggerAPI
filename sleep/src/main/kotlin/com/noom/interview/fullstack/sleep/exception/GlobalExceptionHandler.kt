package com.noom.interview.fullstack.sleep.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import net.bytebuddy.agent.builder.AgentBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(AgentBuilder.CircularityLock.Global::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val errorResponse = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<String> {
        return ResponseEntity.status(404).body(ex.message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val rootCause = ex.cause
        val readableMessage = if (rootCause is MismatchedInputException) {
            "Invalid format for field '${rootCause.path.joinToString(".") { it.fieldName ?: "" }}'. " +
                    "Please ensure dates match 'yyyy-MM-dd' and times match 'HH:mm:ss' strings."
        } else {
            "Malformed JSON request payload body structure."
        }

        val errorResponse = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Malformed Payload",
            message = readableMessage,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    // 3. Catch-All for unexpected runtime infrastructure or database failures (500 Internal Error)
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val errorResponse = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred while processing your request.",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    @ExceptionHandler(
        MissingPathVariableException::class,
    )
    fun handleBadRequestExceptions(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("Bad Request received: ${ex.message}")

        val errorBody = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Required request parameters, path variables, or body fields are missing or malformed.",
            path = request.requestURI

        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody)
    }
}