package de.tfelix.evmbitstream.exception

import de.tfelix.evmbitstream.payment.InvalidPaymentException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.*

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    protected fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any>? {
        return super.handleHttpMessageNotReadable(ex, headers, status, request)
    }

    protected fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors: MutableMap<String, TreeSet<String>> = HashMap()

        for (error in ex.fieldErrors) {
            val messages = errors.getOrDefault(error.field, TreeSet())
            messages.add(error.defaultMessage ?: "no error message given")
            errors[error.field] = messages
        }

        return ResponseEntity
            .badRequest()
            .body(InvalidRequestResponse(message = "invalid request", errors = errors))
    }

    protected fun handleBindException(
        ex: BindException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors: MutableMap<String, TreeSet<String>> = HashMap()

        for (error in ex.fieldErrors) {
            val messages = errors.getOrDefault(error.field, TreeSet())
            messages.add(error.defaultMessage ?: "no error message given")
            errors[error.field] = messages
        }

        return ResponseEntity
            .badRequest()
            .body(InvalidRequestResponse(message = "invalid request", errors = errors))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: Exception, request: WebRequest): ResponseEntity<*> {
        LOG.error(ex) { "Server error" }

        return ResponseEntity<Any>(
            ErrorResponse("unknown server error, please try again later"),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(InvalidPaymentException::class)
    fun handleInvalidPaymentException(ex: InvalidPaymentException, request: WebRequest): ResponseEntity<*> {
        return ResponseEntity<Any>(
            ErrorResponse(ex.message ?: "there was a problem with the payment"),
            HttpStatus.PAYMENT_REQUIRED
        )
    }

    companion object {
        private val LOG = KotlinLogging.logger { }
    }
}