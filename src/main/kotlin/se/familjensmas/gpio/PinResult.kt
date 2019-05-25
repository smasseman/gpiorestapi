package se.familjensmas.gpio

import com.pi4j.io.gpio.PinState

data class PinResult(val pin: Int, val state: PinState)

