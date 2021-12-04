package modules

import io.ktor.application.*
import module
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDi() {
  install(Koin) {
    slf4jLogger()

    modules(module {
      val type = environment.config.property("ktor.deployment.type").getString()
      single { Env(type) }
    })
  }
}

class Env(var type: String)