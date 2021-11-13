import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import org.jetbrains.exposed.sql.transactions.transaction
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
import pages.index
import java.util.*

fun main(args: Array<String> = emptyArray()) {
  EngineMain.main(args)
}

fun Application.module() {
  configureDI()
  configureAuth()
  configureSerialization()
  configureRouting()
  configureDB()
}

