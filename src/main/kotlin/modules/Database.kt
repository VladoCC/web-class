package modules

/* ktlint-disable no-wildcard-imports */
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.modules
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
/* ktlint-enable no-wildcard-imports */

private const val POOL_SIZE = 6

fun Application.configureDb() {
    val dbDir = FileSystems.getDefault().getPath("db")
    val env by inject<Env>()
    try {
        Files.createDirectories(dbDir)
    } catch (e: UnsupportedOperationException) {
        e.printStackTrace()
    }

    val database = if (env.type == "test") {
        val cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite::memory:"
            maximumPoolSize = POOL_SIZE
        }
        Database.connect(HikariDataSource(cfg))
    } else {
        val workingDir = Paths.get("").toAbsolutePath().toString()
        val path = "$workingDir/db/todos_${env.type}.db"
        Database.connect("jdbc:sqlite:$path", "org.sqlite.JDBC")
    }
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction(database) {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Todos)
        SchemaUtils.create(Users)
    }
}

object Todos : IntIdTable() {
    val text = varchar("text", 280)
    val owner = integer("owner")
}

class Todo(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Todo>(Todos)

    var text by Todos.text
    var owner by Todos.owner
}

object Users : IntIdTable() {
    val username = varchar("username", 50)
    val password = varchar("password", 32)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var password by Users.password
}
