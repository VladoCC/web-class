
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import modules.*
import java.util.*

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

