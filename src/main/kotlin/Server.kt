import io.ktor.application.*
import io.ktor.server.netty.*
import modules.*

fun main(args: Array<String> = emptyArray()) {
  EngineMain.main(args)
}

fun Application.module() {
  configureDi()
  configureAuth()
  configureSerialization()
  configureRouting()
  configureDb()
}

