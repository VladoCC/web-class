import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.util.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main(args: Array<String> = emptyArray()) {
  initDB()
  EngineMain.main(args)
}

fun Application.module(testing: Boolean = false) {
  val clientId = environment.config.property("oauth.google.clientId").getString()
  val clientSecret = environment.config.property("oauth.google.clientSecret").getString()
  install(Sessions) {
    cookie<Cookie>("cookie") {
      cookie.maxAgeInSeconds = cookieLifetimeSeconds
    }
  }
  install(ContentNegotiation) {
    gson {
      setPrettyPrinting()
      disableHtmlEscaping()
    }
  }
  routing {
    get("/") {
      call.respondHtml(HttpStatusCode.OK) {
        index()
      }
    }

    route("/user") {
      get() {
        call.respondHtml {
          user(call.sessions, clientId)
        }
      }
    }

    get("/signout") {
      call.sessions.clear<Cookie>()
      call.respondRedirect("/todo")
    }

    get("/token") {
      val code = call.request.queryParameters["code"]
      val session = getStoredCookie(call.sessions)

      if (session == null && code != null) {
        val cookie = associateGoogleAccountWithCookie(clientId, clientSecret, code)
        call.sessions.set(cookie)
        call.respondRedirect("/todo")
      } else {
        call.respondRedirect("/user")
      }
    }
    route("/todo") {
      get {
        val id = getStoredCookie(call.sessions)?.id()
        if (id != null) {
          call.respondHtml {
            todo(id)
          }
        } else {
          call.respondRedirect("/user")
        }
      }
      post {
        val id = getStoredCookie(call.sessions)?.id()
        val text = call.receiveParameters()["text"]
        if (!text.isNullOrBlank() && id != null) {
          transaction(database) {
            Todo.new {
              this.text = text
              this.owner = id
            }
          }
        }
        call.respondRedirect("/todo")
      }
      put("/{id}") {
        val userId = getStoredCookie(call.sessions)?.id()
        val formParams = call.receiveParameters()
        val text = formParams["text"]
        val id = try {
          call.parameters["id"]?.toInt()
        } catch (e: NumberFormatException) {
          null
        }
        if (!text.isNullOrBlank() && userId != null && id != null) {
          transaction(database) {
            val todo = Todo.findById(id)
            if (todo?.owner == userId) {
              todo.text = text
            }
          }
        }
      }
      delete("/{id}") {
        val userId = getStoredCookie(call.sessions)?.id()
        val id = try {
          call.parameters["id"]?.toInt()
        } catch (e: NumberFormatException) {
          null
        }
        if (userId != null && id != null) {
          transaction(database) {
            val todo = Todo.findById(id)
            if (todo?.owner == userId) {
              todo.delete()
            }
          }
        }
      }
    }
  }
}