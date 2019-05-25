package se.familjensmas.gpio

import com.pi4j.io.gpio.PinState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class CommandTest {

    @Test
    fun parse() {
        val commands = Controller.Command.parseCommands("2H100,10L500")
        assertThat(commands).hasSize(2)

        assertThat(commands[0].pin).isEqualTo(2)
        assertThat(commands[0].state).isEqualTo(PinState.HIGH)
        assertThat(commands[0].duration).isEqualTo(Duration.ofMillis(100))

        assertThat(commands[1].pin).isEqualTo(10)
        assertThat(commands[1].state).isEqualTo(PinState.LOW)
        assertThat(commands[1].duration).isEqualTo(Duration.ofMillis(500))
    }
}