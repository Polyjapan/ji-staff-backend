package tools

import java.util.Date

import models.EditionsModel.{EditionWrapper, EditionsModel}
import models.FormModel.{BooleanDecorator, ChoiceListDecorator, DateDecorator, Form, FormEntry, FormModel, IntegerDecorator, StringDecorator}

import scala.concurrent.ExecutionContext

/**
  * An object that allows you to create the two basic forms
  *
  * @author Louis Vialar
  */
object TemporaryEdition {
  def createEditions(model: EditionsModel)(implicit ec: ExecutionContext): Unit = {
    /*
Disponibilités dates
   */
    val testStart = 1511283180000L
    val testEnd = 1511373599000L
    val officialStart = 1511373600000L
    val officialEnd = 1514761200000L
    var conventionStart = 1518822000000L

    model.setEdition(EditionWrapper(new Date(testStart), new Date(testEnd), new Date(conventionStart), "2017-test"))
    model.setEdition(EditionWrapper(new Date(officialStart), new Date(officialEnd), new Date(conventionStart),"2018"))
  }
}
