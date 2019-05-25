package se.familjensmas.gpio

import com.pi4j.io.gpio.Pin
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [Initializer::class])
class GpioApplicationTests(@Autowired private val webTestClient: WebTestClient) {

    private val log = LoggerFactory.getLogger(GpioApplicationTests::class.java)

    @LocalServerPort
    var port: Int = 0

    @Test
    fun getLow() {
        simulateLowState(1)
        assertGetLowState(1)
    }

    @Test
    fun getHigh() {
        simulateHighState(1)
        assertGetHighState(1)
    }

    @Test
    fun setHigh() {
        simulateLowState(1)
        postHighState(1)
        assertHighStateInternal(RaspiPin.GPIO_01)
    }

    @Test
    fun setLow() {
        simulateHighState(1)
        postLowState(1)
        assertLowStateInternal(RaspiPin.GPIO_01)
    }

    @Test
    fun postHighAndThenGet() {
        postHighState(1)
        assertGetHighState(1)
    }

    @Test
    fun postLowAndThenGet() {
        postLowState(1)
        assertGetLowState(1)
    }

    @Test
    fun postSequenceH1000L500() {
        simulateLowState(1)
        webTestClient.post().uri("/sequence/1H1000,1L500")
                .exchange()
                .expectStatus().isNoContent

        Thread.sleep(500)
        assertGetHighState(1)
        Thread.sleep(1500)//Make sure sequence is finished before we let another test run.
    }

    @Test
    fun postSequenceH1000L1() {
        simulateLowState(1)
        webTestClient.post().uri("/sequence/1H1000,1L500")
                .exchange()
                .expectStatus().isNoContent

        Thread.sleep(1500)
        assertLowStateInternal(RaspiPin.GPIO_01)
        Thread.sleep(100)//Make sure sequence is finished before we let another test run.
    }

    @Test
    fun eventsForInputPin() {
        events(2)
    }

    @Test
    fun eventsForOutputPin() {
        events(1)
    }

    fun events(pin: Int) {
        simulateLowState(pin)
        val events = LinkedBlockingQueue<PinResult>()

        val disposable = WebClient
                .create("http://localhost:$port")
                .get().uri("/${pin}/events")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<PinResult>>() {})
                .doOnNext { pinResult ->
                    log.info("Got event::: ${pinResult.data()}")
                    pinResult.data()?.let { event ->
                        events.add(event)
                    }
                }
                .doOnError { log.error("Error", it) }
                .subscribe()

        Thread.sleep(300)
        simulateHighState(pin)
        simulateLowState(pin)
        simulateHighState(pin)

        assertEvent(events, PinState.HIGH)
        assertEvent(events, PinState.LOW)
        assertEvent(events, PinState.HIGH)
        assertNoMoreEvents(events)

        disposable.dispose()
    }

    private fun assertNoMoreEvents(events: LinkedBlockingQueue<PinResult>) {
        Assertions.assertThat(events.poll(500, TimeUnit.MILLISECONDS)).isNull()
    }

    private fun assertEvent(events: LinkedBlockingQueue<PinResult>, state: PinState) {
        events.poll(1, TimeUnit.SECONDS)?.let { event ->
            assertThat(event.state).isEqualTo(state)
        } ?: throw Exception("Got no event.")
    }


    private fun assertGetLowState(pin: Int) {
        webTestClient.get().uri("/$pin")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("LOW")
    }

    private fun assertGetHighState(pin: Int) {
        webTestClient.get().uri("/$pin")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("HIGH")
    }

    private fun postHighState(pin: Int) {
        webTestClient.post().uri("/$pin/high")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("HIGH")
    }

    private fun postLowState(pin: Int) {
        webTestClient.post().uri("/$pin/low")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("LOW")
    }

    private fun simulateHighState(pin: Int) {
        webTestClient.post().uri("/simulate/$pin/high")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("HIGH")
    }

    private fun simulateLowState(pin: Int) {
        webTestClient.post().uri("/simulate/$pin/low")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.state").isEqualTo("LOW")
    }

    private fun assertLowStateInternal(pin: Pin) {
        assertThat(RaspiProviderSimulator.instance().getState(pin).isLow).isTrue()
    }

    private fun assertHighStateInternal(pin: Pin) {
        assertThat(RaspiProviderSimulator.instance().getState(pin).isHigh).isTrue()
    }

}

class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        val values = TestPropertyValues.of(
                "gpio.config=OUTPUT.1.Tuta.LOW,INPUT.2.Door.PULL_DOWN")
        values.applyTo(configurableApplicationContext)
    }
}


