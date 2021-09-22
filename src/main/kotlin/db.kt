import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.util.*

val database by lazy {
  val workingDir = Paths.get("").toAbsolutePath().toString()
  val db = Database.connect("jdbc:sqlite:$workingDir/db/todos.db", "org.sqlite.JDBC")
  TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
  db
}

fun initDB() {
  val dbDir = FileSystems.getDefault().getPath("db");
  try {
    Files.createDirectories(dbDir)
  } catch (e: UnsupportedOperationException) {}
  transaction(database) {
    addLogger(StdOutSqlLogger)
    SchemaUtils.create(Todos)
  }
}

object Todos: IntIdTable() {
  val text = varchar("text", 280)
  val owner = varchar("owner", 30)
}

class Todo(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Todo>(Todos)
  var text by Todos.text
  var owner by Todos.owner
}