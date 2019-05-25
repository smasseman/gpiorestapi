package se.familjensmas.gpio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GpioApplication

fun main(args: Array<String>) {
	runApplication<GpioApplication>(*args)
}
