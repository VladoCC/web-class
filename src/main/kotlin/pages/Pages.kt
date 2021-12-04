package pages

import kotlinx.html.* // ktlint-disable no-wildcard-imports

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
