import kotlinx.html.*

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