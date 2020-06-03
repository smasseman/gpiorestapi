package se.familjensmas.gpio

import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.Executors
import java.util.regex.Pattern

@RequestMapping("/gpio")
@RestController
class Controller(private val config: GpioConfiguration) {

    private val sequenceExecutor = Executors.newSingleThreadExecutor()
    private val gpio: GpioController = GpioFactory.getInstance()
    private val log = LoggerFactory.getLogger(Controller::class.java)

    @GetMapping("/{pin}")
    fun get(@PathVariable pin: Int): Mono<PinResult> {
        val state = gpio.getState(config.getDigitalPin(pin))
        return Mono.just(PinResult(pin, state))
    }

    @GetMapping("/{pin}/events", produces = ["text/event-stream"])
    fun events(@PathVariable pin: Int): Flux<ServerSentEvent<PinResult>> {
        val gpioPin: GpioPinDigital = config.getDigitalPin(pin)

        val flux: Flux<ServerSentEvent<PinResult>> = Flux.create { sink ->
            val pinListener = GpioPinListenerDigital { event ->
                log.debug("Got event ${event.pin}=${event.state}")
                sink.next(ServerSentEvent.builder<PinResult>()
                        .event("pin_state")
                        .data(PinResult(pin, event!!.state))
                        .build())
            }

            log.debug("Added listener on $gpioPin")
            gpioPin.addListener(pinListener)
            //gpio.addListener(pinListener, gpioPin)

            sink.onDispose {
                log.debug("Remove listener from $gpioPin")
                //gpio.removeListener(pinListener, gpioPin)
                gpioPin.removeListener(pinListener)
            }
        }
        return flux
                .doOnNext{log.debug("Pushing event $it")}
                .doOnSubscribe { log.info("Subscribed on pin $pin") }
    }

    @PostMapping("/{pin}/high")
    fun high(@PathVariable pin: Int) = setState(pin, true)

    @PostMapping("/simulate/{pin}/high")
    fun simulateHigh(@PathVariable pin: Int) = setSimulatedState(pin, true)

    @PostMapping("/{pin}/low")
    fun low(@PathVariable pin: Int) = setState(pin, false)

    @PostMapping("/simulate/{pin}/low")
    fun simulateLow(@PathVariable pin: Int) = setSimulatedState(pin, false)

    @PostMapping("/sequence/{sequence}") //1/H300,L500
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sequence(@PathVariable sequence: String) {
        val commands = Command.parseCommands(sequence)
        //Verify that all pins are output pins
        commands.forEach { config.getDigitalOutputPin(it.pin) }

        log.debug("Submit commands: {}", commands)
        sequenceExecutor.submit {
            try {
                commands.forEach { command ->
                    val digitalPin = config.getDigitalOutputPin(command.pin)
                    log.debug("Execute command {}", command)
                    gpio.setState(command.state, digitalPin)
                    log.debug("Sleep {}", command.duration.toMillis())
                    Thread.sleep(command.duration.toMillis())
                }
                log.debug("All commands executed.")
            } catch (e: Throwable) {
                log.error("Failed.", e)
            }
        }
    }

    private fun setState(pin: Int, state: Boolean): Mono<PinResult> {
        val digitalPin = config.getDigitalOutputPin(pin)
        gpio.setState(state, digitalPin)
        return get(pin)
    }

    private fun setSimulatedState(pin: Int, state: Boolean): Mono<PinResult> {
        with(GpioFactory.getDefaultProvider()) {
            if (this is RaspiProviderSimulator) {
                this.simulateStateChange(config.getDigitalPin(pin).pin, if (state) PinState.HIGH else PinState.LOW)
            } else {
                throw IllegalStateException("Not running simulator mode.")
            }
        }
        return get(pin)
    }

    data class Command(val pin: Int, val state: PinState, val duration: Duration) {
        companion object {

            fun parseCommands(sequence: String) = sequence.split(Pattern.compile(",")).map { parse(it) }

            private fun parse(commandString: String): Command {
                val numbers = commandString.split(Pattern.compile("[^0-9]+"))
                val pinNumberStr = numbers[0]
                val state = commandString[pinNumberStr.length].let { stateChar ->
                    when (stateChar) {
                        'H' -> PinState.HIGH
                        'L' -> PinState.LOW
                        else -> throw IllegalArgumentException("Not a valid state: $stateChar")
                    }
                }
                val duration = Duration.ofMillis(numbers[1].toLong())
                return Command(pinNumberStr.toInt(), state, duration)
            }
        }
    }

}
