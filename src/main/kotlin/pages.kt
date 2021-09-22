import io.ktor.html.*
import io.ktor.sessions.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.transactions.transaction

const val signInUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
    "scope=https://www.googleapis.com/auth/userinfo.profile&" +
    "include_granted_scopes=true&" +
    "response_type=code&" +
    "redirect_uri=http://localhost:8080/token&" +
    "client_id=%s"

fun HTML.index() {
  head {
    title("Hello from Ktor!")
  }
  body {
    div {
      +"Hello from Ktor"
    }
  }
}

fun HTML.user(session: CurrentSession, clientId: String) {
  head {
    title {
      +"User"
    }
  }
  body {
    div {
      val id = getStoredCookie(session)?.id()
      if (id != null) {
        a("/todo") {
          +"To Todo List"
        }
        a("/signout") {
          +"Sign Out"
        }
      } else {
        signin(clientId)
      }
    }
  }
}

fun FlowOrInteractiveOrPhrasingContent.signin(clientId: String) {
  val signIn = signInUrl.format(clientId)
  a(href = signIn) {
    +"Sign In using Google Account"
  }
}

fun FlowContent.todoElement(todo: Todo) {
  val todoId = "todo${todo.id}"
  DIV(attributesMapOf("id", todoId), consumer).visit {
    contentEditable = true
    +todo.text
    id = todoId
    input(type = InputType.button) {
      value = "Update"
      onClick = "updateTodo(document.getElementById(\"$todoId\").firstChild.textContent, ${todo.id});" +
          "\n" +
          "function updateTodo(text, id) {\n" +
          "    const formData = new FormData();\n" +
          "    formData.append('text', text);\n" +
          "\n" +
          "    return fetch('/todo/' + id, {\n" +
          "        method: 'PUT',\n" +
          "        body: formData\n" +
          "    }).then(response => location.reload())\n" +
          "}"
    }
    input(type = InputType.button) {
      value = "Delete"
      onClick = "deleteTodo(${todo.id});" +
          "\n" +
          "function deleteTodo(id) {\n" +
          "    return fetch('/todo/' + id, {\n" +
          "        method: 'DELETE',\n" +
          "    }).then(response => location.reload())\n" +
          "}"
    }
  }
}

fun HTML.todo(id: String) {
  head {
    title {
      +"Todos"
    }
  }
  body {
    div {
      val todos = transaction(database) {
        Todo.find { Todos.owner eq id }.toList()
      }
      if (todos.isEmpty()) {
        div {
          +"You don't have todos yet"
        }
      } else {
        todos.forEach {
          todoElement(it)
        }
      }
      form(method = FormMethod.post) {
        input(type = InputType.text, name = "text") {
          this.id = "test"
          required = true
        }
        input(type = InputType.submit) {
          value = "Add Todo"
        }
      }
    }
  }
}