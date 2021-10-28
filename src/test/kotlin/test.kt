import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApiTest {

  data class LoginResponse(val action: String, val token: String)
  data class GetTodosResponse(val todo: Map<String, String>)

  private val env = createTestEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
  }
  private val gson = Gson()

  private fun <T> TestApplicationEngine.login(username: String, password: String, block: TestApplicationCall.() -> T): T {
    with(handleRequest(HttpMethod.Post, "/user") {
      addHeader("Accept", "application/json")
      addHeader("Content-Type", "application/json")
      setBody("""{ "username": "$username", "password": "$password"}""")
    }) {
      return block()
    }
  }

  private fun <T> TestApplicationEngine.todo(token: String,
                                             method: HttpMethod = HttpMethod.Get,
                                             body: String = "",
                                             path: String = "",
                                             block: TestApplicationCall.() -> T): T {
    with(handleRequest(method, "/todo$path") {
      addHeader("Accept", "application/json")
      addHeader("Content-Type", "application/json")
      addHeader("Authorization", "Bearer $token")
      setBody(body)
    }) {
      return block()
    }
  }

  private fun Gson.loginResponse(json: String) = fromJson(json, LoginResponse::class.java)
  private fun Gson.getTodosResponse(json: String) = fromJson(json, GetTodosResponse::class.java)

  @Test
  fun testLogin() {
    withApplication(env) {
      login("test", "correct") {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        assertEquals("Sign In", gson.loginResponse(response.content!!).action)
      }
      login("test", "correct") {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        assertEquals("Log In", gson.loginResponse(response.content!!).action)
      }
      login("test", "incorrect") {
        assertEquals(HttpStatusCode.Forbidden, response.status())
      }
    }
  }

  @Test
  fun testCRUD() {
    withApplication(env) {
      val token = login("test", "correct") {
        assertNotNull(response.content)
        gson.loginResponse(response.content!!).token
      }

      todo(token) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        val response = gson.getTodosResponse(response.content!!)
        assert(response.todo.isEmpty())
      }

      val message = "test message"

      // create
      val texts = (1..10).map {
        todo(token, HttpMethod.Post, """{ "text": "$message$it"}""") {
          assertEquals(HttpStatusCode.OK, response.status())
        }
        "$message$it"
      }

      // read
      todo(token) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        val response = gson.getTodosResponse(response.content!!)
        assert(response.todo.size == 10)
        assertContentEquals(texts, response.todo.values)
      }

      // update
      val updatedMessage = "todo with major changes"
      todo(token, HttpMethod.Put,"""{ "text": "$updatedMessage"}""","/7") {
        assertEquals(HttpStatusCode.OK, response.status())
      }

      todo(token) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        val response = gson.getTodosResponse(response.content!!)
        assert(response.todo.size == 10)
        assertEquals(response.todo["7"], updatedMessage)
      }

      // delete
      todo(token, HttpMethod.Delete, path = "/2") {
        assertEquals(HttpStatusCode.OK, response.status())
      }

      todo(token) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertNotNull(response.content)
        val response = gson.getTodosResponse(response.content!!)
        assert(response.todo.size == 9)
        assertFalse { response.todo.containsKey("2") }
      }
    }
  }

  private fun TestApplicationEngine.testSingleTodo(token: String, text: String) = todo(token) {
    assertEquals(HttpStatusCode.OK, response.status())
    assertNotNull(response.content)
    val response = gson.getTodosResponse(response.content!!)
    assert(response.todo.size == 1)
    val todo = response.todo.toList()[0]
    assertEquals(text, todo.second)
    todo.first
  }

  @Test
  fun testAccess() {
    withApplication(env) {
      val token1 = login("test1", "foo") {
        assertNotNull(response.content)
        gson.loginResponse(response.content!!).token
      }

      val token2 = login("test2", "bar") {
        assertNotNull(response.content)
        gson.loginResponse(response.content!!).token
      }

      val todo1 = "Todo of tester 1"
      todo(token1, HttpMethod.Post, """{ "text": "$todo1"}""") {
        assertEquals(HttpStatusCode.OK, response.status())
      }

      val todo2 = "Todo of tester 2"
      todo(token2, HttpMethod.Post, """{ "text": "$todo2"}""") {
        assertEquals(HttpStatusCode.OK, response.status())
      }

      val id1 = testSingleTodo(token1, todo1)
      val id2 = testSingleTodo(token2, todo2)

      val updatedTodo = "Todo that tester don't have access to"
      todo(token1, HttpMethod.Put, """{ "text": "$updatedTodo"}""", "/$id2") {
        assertEquals(HttpStatusCode.Forbidden, response.status())
      }
      todo(token2, HttpMethod.Put, """{ "text": "$updatedTodo"}""", "/$id1") {
        assertEquals(HttpStatusCode.Forbidden, response.status())
      }
      todo("", HttpMethod.Put, """{ "text": "$updatedTodo"}""", "/$id2") {
        assertEquals(HttpStatusCode.Unauthorized, response.status())
      }
      todo("", HttpMethod.Put, """{ "text": "$updatedTodo"}""", "/$id1") {
        assertEquals(HttpStatusCode.Unauthorized, response.status())
      }

      testSingleTodo(token1, todo1)
      testSingleTodo(token2, todo2)

      todo(token1, HttpMethod.Delete, path = "/$id2") {
        assertEquals(HttpStatusCode.Forbidden, response.status())
      }
      todo(token2, HttpMethod.Delete, path = "/$id1") {
        assertEquals(HttpStatusCode.Forbidden, response.status())
      }
      todo("", HttpMethod.Delete, path = "/$id2") {
        assertEquals(HttpStatusCode.Unauthorized, response.status())
      }
      todo("", HttpMethod.Delete, path = "/$id1") {
        assertEquals(HttpStatusCode.Unauthorized, response.status())
      }

      testSingleTodo(token1, todo1)
      testSingleTodo(token2, todo2)
    }
  }
}