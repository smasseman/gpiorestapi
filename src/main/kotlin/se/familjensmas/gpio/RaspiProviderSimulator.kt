package se.familjensmas.gpio

import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.PinListener
import org.slf4j.LoggerFactory

class RaspiProviderSimulator : GpioProviderBase(), GpioProvider {

    override fun getName(): String {
        return NAME
    }

    override fun hasPin(pin: Pin): Boolean {
        return true
    }

    override fun export(pin: Pin, mode: PinMode) {
        super.export(pin, mode)
        setMode(pin, mode)
        simulateStateChange(pin, PinState.LOW)
    }

    override fun isExported(pin: Pin): Boolean {
        return super.isExported(pin)
    }

    override fun unexport(pin: Pin) {
        super.unexport(pin)
    }

    override fun setMode(pin: Pin, mode: PinMode) {
        super.setMode(pin, mode)
    }

    override fun getMode(pin: Pin): PinMode {
        return super.getMode(pin)
    }

    override fun setPullResistance(pin: Pin, resistance: PinPullResistance) {
        super.setPullResistance(pin, resistance)
    }

    override fun getPullResistance(pin: Pin): PinPullResistance {
        return super.getPullResistance(pin)
    }

    override fun setState(pin: Pin, state: PinState) {
        pinStateLogger.debug("Set {} to {}", pin.name, state)
        super.setState(pin, state)
        getPinCache(pin).state = state
        dispatchPinDigitalStateChangeEvent(pin, state)
    }

    override fun getState(pin: Pin): PinState {
        return super.getState(pin)
    }

    override fun setValue(pin: Pin, value: Double) {
        super.setValue(pin, value)
        throw RuntimeException("This GPIO provider does not support analog pins.")
    }

    override fun getValue(pin: Pin): Double {
        super.getValue(pin)
        throw RuntimeException("This GPIO provider does not support analog pins.")
    }

    override fun setPwm(pin: Pin, value: Int) {
        super.setPwm(pin, value)
        if (getMode(pin) == PinMode.PWM_OUTPUT) {
            throw UnsupportedOperationException()
        }
    }

    override fun getPwm(pin: Pin): Int {
        return super.getPwm(pin)
    }

    fun simulateStateChange(pin: Pin, state: PinState) {
        getPinCache(pin).state = state
        dispatchPinDigitalStateChangeEvent(pin, state)
    }

    override fun addListener(pin: Pin, listener: PinListener) {
        super.addListener(pin, listener)
    }

    override fun removeListener(pin: Pin, listener: PinListener) {
        super.removeListener(pin, listener)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(RaspiProviderSimulator::class.java)
        private val pinStateLogger = LoggerFactory.getLogger("GPIO")
        const val NAME = "RaspberryPi GPIO Provider"

        fun setupGpioSimulator(): Boolean {
            val os = System.getProperty("os.name")
            return if (os == "Mac OS X") {
                logger.info("Setup simulator since we are running on $os")
                val provider = RaspiProviderSimulator()
                GpioFactory.setDefaultProvider(provider)
                true
            } else {
                logger.debug("Simulator not setup since we are running on $os")
                false
            }
        }

        fun instance(): RaspiProviderSimulator =
                with(GpioFactory.getDefaultProvider()) {
                    return if (this is RaspiProviderSimulator) {
                        this
                    } else {
                        throw IllegalStateException("Simulator is not activated")
                    }
                }

    }
}
