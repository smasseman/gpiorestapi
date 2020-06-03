package se.familjensmas.gpio

import com.pi4j.io.gpio.*
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class GpioConfiguration(private val outputPins: Map<Int, GpioPinDigitalOutput>,
                        private val inputPins: Map<Int, GpioPinDigitalInput>,
                        private val pins: Map<Int, GpioPinDigital>) {

    companion object {

        private val log = LoggerFactory.getLogger(GpioConfiguration::class.java)

        fun parse(string: String): GpioConfiguration {
            val gpio: GpioController = GpioFactory.getInstance()
            val outputPins: MutableMap<Int, GpioPinDigitalOutput> = HashMap()
            val inputPins: MutableMap<Int, GpioPinDigitalInput> = HashMap()
            val pins: MutableMap<Int, GpioPinDigital> = HashMap()

            //INPUT.1.Door.PULL_DOWN,OUTPUT.2.Tuta.LOW
            string.split(Regex(","))
                    .forEach {
                        it.split(Pattern.compile(Pattern.quote("."))).let { pinSplit ->
                            val pinNumber = pinSplit[1].toInt()
                            val pinName = pinSplit[2]
                            val pin: Pin = getPinByNumber(pinNumber)

                            when (pinSplit[0]) {
                                "INPUT" -> {
                                    val resistance = PinPullResistance.valueOf(pinSplit[3])
                                    val inputPin = gpio.provisionDigitalInputPin(pin, pinName, resistance)
                                    inputPins[pinNumber] = inputPin
                                    pins[pinNumber] = inputPin
                                    log.info("Configured input pin: ${pin}:${pinName}:${resistance}")
                                }
                                "OUTPUT" -> {
                                    val state = PinState.valueOf(pinSplit[3])
                                    val outputPin = gpio.provisionDigitalOutputPin(pin, pinName, state)
                                    outputPin.state = state
                                    outputPins[pinNumber] = outputPin
                                    pins[pinNumber] = outputPin
                                    log.info("Configured output pin: ${pin}:${pinName}:${state}")
                                }
                                else -> throw InvalidPinException("Expected INPUT or OUTPUT but got ${pinSplit[0]}")
                            }
                        }
                    }

            return GpioConfiguration(outputPins, inputPins, pins)
        }

        private fun getPinByNumber(pin: Int): Pin {
            return RaspiPin.allPins().first {
                it.name.endsWith(" $pin")
            } ?: throw InvalidPinException("$pin is not a valid pin number")
        }
    }

    fun getDigitalOutputPin(pin: Int) =
            outputPins[pin]
                    ?: if (inputPins.containsKey(pin)) {
                        throw InvalidPinException("Pin $pin is configured as input pin")
                    } else {
                        throw InvalidPinException("Pin $pin is not configured")
                    }

    fun getDigitalInputPin(pin: Int) =
            inputPins[pin]
                    ?: if (outputPins.containsKey(pin)) {
                        throw InvalidPinException("Pin $pin is configured as output pin")
                    } else {
                        throw InvalidPinException("Pin $pin is not configured")
                    }

    fun getDigitalPin(pin: Int) = pins[pin]
            ?: if (pins.containsKey(pin)) {
                throw InvalidPinException("Pin $pin is configured as output pin")
            } else {
                throw InvalidPinException("Pin $pin is not configured")
            }


}
