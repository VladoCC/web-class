import io.ktor.application.Application
import io.ktor.server.netty.EngineMain
import modules.configureAuth
import modules.configureDb
import modules.configureDi
import modules.configureRouting
import modules.configureSerialization

fun main(args: Array<String> = emptyArray()) {
    // garbage
    EngineMain.main(args)
}

fun Application.module() {
    configureDi()
    configureAuth()
    configureSerialization()
    configureRouting()
    configureDb()
}
