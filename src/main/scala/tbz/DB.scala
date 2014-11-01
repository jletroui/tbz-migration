package tbz

import java.io.Closeable
import java.sql.{PreparedStatement, ResultSet}

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import annotation.tailrec
import language.implicitConversions
import scala.io.Source

object DB {
  val MySQLDatasource = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
  val MySQLDriver = "com.mysql.jdbc.Driver"

  // See http://spray.io/blog/2012-12-13-the-magnet-pattern/ for how this works
  // Setters allow strongly typed parameters to be passed to the query
  type ParamSetter = (PreparedStatement, Int) => Unit

  implicit def intSetter(v: Int): ParamSetter = (stmt, idx) => stmt.setInt(idx, v)

  implicit def longSetter(v: Long): ParamSetter = (stmt, idx) => stmt.setLong(idx, v)

  implicit def stringSetter(v: String): ParamSetter = (stmt, idx) => stmt.setString(idx, v)

  implicit def byteArraySetter(v: Array[Byte]): ParamSetter = (stmt, idx) => stmt.setBytes(idx, v)
}

/**
 * Minimal JDBC wrapper for clear DB calls in the code.
 */
class DB(url: String, user: String, password: String, driverClassName: String) extends Logging with Closeable {
  import DB._

  log.info(s"  Connecting to database $url")

  private val poolConfig = new HikariConfig()
  poolConfig.setDriverClassName(driverClassName)
  poolConfig.setMaximumPoolSize(3)
  poolConfig.setJdbcUrl(url)
  poolConfig.setUsername(user)
  poolConfig.setPassword(password)

  private val pool = new HikariDataSource(poolConfig)

  def close = pool.close()

  @tailrec
  private def mapWhile[T](test: => Boolean, block: => T, acc: Seq[T] = Seq.empty[T]): Seq[T] =
    if (test) mapWhile(test, block, acc :+ block)
    else acc

  /** Executes the SQL and processes the result set using the specified function. */
  def query[B](sql: String, params: ParamSetter*)(transformResultSet: ResultSet => B): B =
    using(pool.getConnection) { connection =>
      using(connection.prepareStatement(sql)) { statement =>

        params.zipWithIndex.foreach { case (setParam, index) => setParam(statement, index + 1)}

        using(statement.executeQuery()) { results =>
          transformResultSet(results)
        }
      }
    }

  /** Executes the SQL and uses the process function to convert each row into a T. Returns a sequence of the results. */
  def queryEach[T](sql: String, params: ParamSetter*)(transformRow: ResultSet => T): Seq[T] =
    query(sql, params: _*) { results =>
      mapWhile(results.next, transformRow(results))
    }

  def update(sql: String, params: ParamSetter*) =
    using(pool.getConnection) { connection =>
      using(connection.prepareStatement(sql)) { statement =>
        params.zipWithIndex.foreach { case (setParam, index) => setParam(statement, index + 1)}

        statement.executeUpdate()
      }
    }

  def execute(sqlFile: String) = {
    val statements = Source.fromFile(sqlFile).getLines.mkString(" ").split(";")
    statements.foreach(update(_))
  }
}