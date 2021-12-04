package modules

/* ktlint-disable no-wildcard-imports */
import io.ktor.application.*
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger
/* ktlint-enable no-wildcard-imports */

fun Application.configureDi() {
    install(Koin) {
        slf4jLogger()

        modules(
            module {
                val type = environment.config.property("ktor.deployment.type").getString()
                single { Env(type) }
            }
        )
    }
}

class Env(var type: String)
