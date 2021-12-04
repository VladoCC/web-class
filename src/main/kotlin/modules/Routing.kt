package modules

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
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
import pages.index
import java.util.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK) {
                index()
            }
        }

        post("/user") {
            val secret = environment.config.property("jwt.secret").getString()
            val issuer = environment.config.property("jwt.issuer").getString()
            val audience = environment.config.property("jwt.audience").getString()
            val hashSalt = environment.config.property("db.hash.salt").getString()

            val user = call.receive<Auth>()
            val storedUser = transaction {
                User.find { Users.username eq user.username }.firstOrNull()
            }
            val hashedPassword = "${user.password}$hashSalt".digestKeccak(KeccakParameter.KECCAK_256).decodeToString()
            val success = storedUser == null || storedUser.password == hashedPassword
            if (success) {
                val id = storedUser?.id ?: transaction {
                    User.new {
                        username = user.username
                        password = hashedPassword
                    }
                }.id
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("id", id.value)
                    .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                    .sign(Algorithm.HMAC256(secret))
                call.respond(hashMapOf("token" to token, "action" to if (storedUser == null) "Sign In" else "Log In"))
            } else {
                call.respond(HttpStatusCode.Forbidden, Unit)
            }
        }

        val env by inject<Env>()
        get("/health") {
            call.respond(Health(env.type))
        }

        authenticate("auth-jwt") {
            route("/todo") {
                get {
                    val id = call.id()
                    val todos = transaction {
                        Todo.find { Todos.owner eq id }.map { "${it.id}" to it.text }.toMap()
                    }
                    call.respond(mapOf("todo" to todos))
                }
                post {
                    val post = call.receive<PostContent>()
                    val id = call.id()
                    val text = post.text
                    var success = false
                    if (text.isNotBlank()) {
                        transaction {
                            Todo.new {
                                this.text = text
                                this.owner = id
                            }
                            success = true
                        }
                    }
                    call.respond(success)
                }
                put("/{id}") {
                    val post = call.receive<PostContent>()
                    val userId = call.id()
                    val text = post.text
                    val id = try {
                        call.parameters["id"]?.toInt()
                    } catch (e: NumberFormatException) {
                        null
                    }
                    var success = false
                    if (text.isNotBlank() && id != null) {
                        transaction {
                            val todo = Todo.findById(id)
                            if (todo?.owner == userId) {
                                todo.text = text
                                success = true
                            }
                        }
                    }
                    call.respond(success)
                }
                delete("/{id}") {
                    val userId = call.id()
                    val id = try {
                        call.parameters["id"]?.toInt()
                    } catch (e: NumberFormatException) {
                        null
                    }
                    var success = false
                    if (id != null) {
                        transaction {
                            val todo = Todo.findById(id)
                            if (todo?.owner == userId) {
                                todo.delete()
                                success = true
                            }
                        }
                    }
                    call.respond(success)
                }
            }
        }
    }
}

suspend fun ApplicationCall.respond(success: Boolean) {
    if (success) {
        respond(status = HttpStatusCode.OK, Unit)
    } else {
        respond(status = HttpStatusCode.Forbidden, Unit)
    }
}

fun ApplicationCall.id(): Int {
    val principal = principal<JWTPrincipal>()
    return principal!!.payload.getClaim("id").asInt()
}

data class Auth(val username: String, val password: String)
data class PostContent(val text: String)
data class Health(val appEnv: String)
