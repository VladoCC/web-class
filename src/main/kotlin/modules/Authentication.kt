package modules

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureAuth() {
    install(Authentication) {
        jwt("auth-jwt") {
            val secret = environment.config.property("jwt.secret").getString()
            val issuer = environment.config.property("jwt.issuer").getString()
            val audience = environment.config.property("jwt.audience").getString()
            realm = environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                val id = credential.payload.getClaim("id").asInt()
                val user = transaction {
                    User.findById(id)
                }
                if (user != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
