package modules

/* ktlint-disable no-wildcard-imports */
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
/* ktlint-enable no-wildcard-imports */

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}
