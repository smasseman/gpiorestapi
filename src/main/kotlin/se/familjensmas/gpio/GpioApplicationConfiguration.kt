package se.familjensmas.gpio

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GpioApplicationConfiguration {

    init {
        RaspiProviderSimulator.setupGpioSimulator()
    }

    @Bean
    fun config(@Value("\${gpio.config}") config: String) = GpioConfiguration.parse(config)

}