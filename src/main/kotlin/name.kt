import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.sessions.*
import java.util.*

const val cookieLifetimeSeconds = 60 * 60 * 24L
const val tokenUrl = "https://oauth2.googleapis.com/token?" +
    "client_id=%s&" +
    "client_secret=%s&" +
    "code=%s&" +
    "redirect_uri=http://localhost:8080/token&" +
    "grant_type=authorization_code"

private val idMap = mutableMapOf<UUID, String>()
private val cookieLifetimeMap = mutableMapOf<UUID, Long>()

data class Token(val access_token: String, val expires_in: Int,
                 val token_type: String, val scope: String)
data class Cookie(val uuid: UUID) {
  companion object {
    fun create(): Cookie {
      var uuid = UUID.randomUUID()
      while (idMap.containsKey(uuid)) {
        uuid = UUID.randomUUID()
      }
      cookieLifetimeMap[uuid] = System.currentTimeMillis()
      return Cookie(uuid)
    }
  }

  fun validate(): Boolean {
    val lifetime = cookieLifetimeMap[uuid]
    val expired: Boolean
    if (lifetime == null) {
      expired = true
    } else {
      expired = System.currentTimeMillis() > lifetime + cookieLifetimeSeconds * 1000
      if (expired) {
        cookieLifetimeMap.remove(uuid)
      }
    }

    if (expired && idMap.containsKey(uuid)) {
      idMap.remove(uuid)
    }

    return !expired
  }

  fun id() = if (validate()) idMap[uuid] else null
}
data class UserId(val id: String)

/**
 * Sets up http client and returns name of google account corresponding to [code]
 * as a serializable object [NameData]
 */
suspend fun associateGoogleAccountWithCookie(clientId: String, clientSecret: String, code: String): Cookie {
  val client = getClient()
  val accessToken = getAccessToken(clientId, clientSecret, code, client)
  val user = client.get<UserId>("https://www.googleapis.com/oauth2/v1/userinfo") {
    headers {
      append("Authorization", "Bearer $accessToken")
    }
  }
  val cookie = Cookie.create()
  idMap[cookie.uuid] = user.id
  return cookie
}

/**
 * Returns access token, if [code] is right.
 * Stops page with code 500 otherwise.
 */
private suspend fun getAccessToken(clientId: String, clientSecret: String, code: String, client: HttpClient): String {
  val url = tokenUrl.format(clientId, clientSecret, code)
  return client.post<Token>(url).access_token
}

/**
 * Returns [HttpClient] that is able to parse JSON content.
 *
 * It's expected that server won't send responses too often,
 * so it's better to create new client every time rather than store it in memory.
 */
private fun getClient() = HttpClient() {
  install(JsonFeature)
}

fun getStoredCookie(sessions: CurrentSession): Cookie? {
  val cookie = sessions.get<Cookie>()
  return if (cookie?.validate() == true) cookie else null
}