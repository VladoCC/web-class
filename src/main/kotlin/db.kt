import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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
    SchemaUtils.create(Users)
  }
}

object Todos: IntIdTable() {
  val text = varchar("text", 280)
  val owner = integer("owner")
}

class Todo(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Todo>(Todos)
  var text by Todos.text
  var owner by Todos.owner
}

object Users: IntIdTable() {
  val username = varchar("username", 50)
  val password = varchar("password", 30)
}

class User(id: EntityID<Int>): IntEntity(id) {
  companion object : IntEntityClass<User>(Users)
  var username by Users.username
  var password by Users.password
}