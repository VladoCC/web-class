import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private object AES256 {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()
  internal var secretKey = ""

  private fun cipher(opmode: Int):Cipher{
    val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val arr = secretKey.toByteArray(Charsets.UTF_8)
    val sk = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
    val iv = IvParameterSpec(secretKey.substring(0, 16).toByteArray(Charsets.UTF_8))
    try {
      c.init(opmode, sk, iv)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return c
  }

  fun encrypt(str: String): String{
    val encrypted = cipher(Cipher.ENCRYPT_MODE).doFinal(str.toByteArray(Charsets.UTF_8))
    return String(encoder.encode(encrypted))
  }

  fun decrypt(str: String): String{
    val byteStr = decoder.decode(str.toByteArray(Charsets.UTF_8))
    return String(cipher(Cipher.DECRYPT_MODE).doFinal(byteStr))
  }
}

val database by lazy {
  val workingDir = Paths.get("").toAbsolutePath().toString()
  val db = Database.connect("jdbc:sqlite:$workingDir/db/todos.db", "org.sqlite.JDBC")
  TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
  db
}

fun initDB(encryptionKey: String) {
  AES256.secretKey = encryptionKey
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
  private var _password by Users.password
  var password: String
    get() = AES256.decrypt(_password)
    set(value) {
      _password = AES256.encrypt(value)
    }
}