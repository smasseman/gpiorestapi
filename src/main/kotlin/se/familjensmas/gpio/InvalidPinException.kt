package se.familjensmas.gpio

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPinException(s: String) : BadRequestException(s)
