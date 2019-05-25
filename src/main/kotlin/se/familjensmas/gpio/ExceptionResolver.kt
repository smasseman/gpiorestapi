package se.familjensmas.gpio

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionResolver {

    @ExceptionHandler(value = [BadRequestException::class])
    fun resolveAndWriteException(e: BadRequestException): ResponseEntity<Failure> {
        return ResponseEntity(Failure(e.message), HttpStatus.BAD_REQUEST)
    }

    data class Failure(val message: String?)
}