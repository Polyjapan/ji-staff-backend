package models

import java.sql.Connection
import anorm.Macro.ColumnNaming
import anorm.SqlParser.scalar
import anorm.{Column, NamedParameter, SQL, ToParameterList, ToStatement}

object SqlUtils {

  implicit def enumToColumn(enum: Enumeration): Column[enum.Value] = Column.columnToString.map(s => enum.withName(s))
  implicit def enumToStatement(enum: Enumeration): ToStatement[enum.Value] = (s, index, v) => s.setString(index, v.toString)

  /**
   * Inserts one item in the given table and returns its id
   *
   * @param table the table in which the item shall be inserted
   * @param item  the item that shall be inserted
   * @return the id of the inserted item
   */
  def insertOne[T](table: String, item: T, columnNaming: ColumnNaming = ColumnNaming.SnakeCase)(implicit parameterList: ToParameterList[T], conn: Connection): Int = {
    val params: Seq[NamedParameter] = parameterList(item);
    val names: List[String] = params.map(_.name).toList
    val colNames = names.map(columnNaming) mkString ", "
    val placeholders = names.map { n => s"{$n}" } mkString ", "

    SQL("INSERT INTO " + table + "(" + colNames + ") VALUES (" + placeholders + ")")
      .bind(item)
      .executeInsert(scalar[Int].single)
  }

  def replaceOne[T](table: String, key: String, item: T, columnNaming: ColumnNaming = ColumnNaming.SnakeCase)(implicit parameterList: ToParameterList[T], conn: Connection): Int = {
    val params: Seq[NamedParameter] = parameterList(item);
    val colNames = params.map(_.name).toList.map { n => s"${columnNaming(n)} = {$n}" }.mkString(", ")

    SQL("UPDATE " + table + " SET " + colNames + s" WHERE ${columnNaming(key)} = {$key}")
      .bind(item)
      .executeUpdate()
  }
}
