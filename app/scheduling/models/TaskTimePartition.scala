package scheduling.models

import play.api.libs.json.Format
import scheduling.Period
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}
import utils.EnumUtils

case class TaskTimePartition(taskPartitionId: Option[Int],
                             task: Int, staffsRequired: Int,
                             shiftDuration: Int, slot: Period,
                             alternateShifts: Boolean, // If enabled, staff changes will be divided in two -- other options ignored if disabled

                             alternateOffset: Option[Int],
                             firstAlternatedShift: PartitionRule.Value,
                             lastAlternatedShift: PartitionRule.Value,
                             lastNormalShift: PartitionRule.Value) {

  def produceSlots: Iterable[TaskSlot] = {
    val shiftedNum = if (alternateShifts) staffsRequired / 2 else 0
    val normalNum = staffsRequired - shiftedNum

    def computeShifts(numStaffs: Int, startTime: Int, endRule: PartitionRule.Value) = {
      startTime.until(slot.timeEnd, shiftDuration) flatMap (start => {
        val end = start + shiftDuration

        val actualEnd = if (end > slot.timeEnd) {
          if (endRule == PartitionRule.Shorter || start == startTime) Some(slot.timeEnd)
          else None // Either removed, or longer (== the previous shift ends the thing)
        } else {
          val remaining = slot.timeEnd - end

          if (remaining < shiftDuration && endRule == PartitionRule.Longer) Some(slot.timeEnd)
          else Some(end)
        }

        actualEnd.map(end => TaskSlot(None, task, numStaffs, Period(slot.day, start, end)))
      })
    }

    // Build main shifts
    val mainShifts = computeShifts(normalNum, slot.timeStart, lastNormalShift).toList

    val alternated = if (shiftedNum > 0) {
      val startOffset = alternateOffset.getOrElse(shiftDuration / 2)
      val offset = startOffset % shiftDuration

      val (start, firstAlternated) =
        if (firstAlternatedShift == PartitionRule.Removed) (slot.timeStart + startOffset, None)
        else if (firstAlternatedShift == PartitionRule.Shorter) (slot.timeStart + startOffset, Some(TaskSlot(None, task, shiftedNum, Period(slot.day, slot.timeStart, slot.timeStart + startOffset))))
        else (slot.timeStart + startOffset + shiftDuration, Some(TaskSlot(None, task, shiftedNum, Period(slot.day, slot.timeStart, slot.timeStart + startOffset + shiftDuration))))

      val alternatedShifts = computeShifts(shiftedNum, start, lastAlternatedShift).toList

      if (firstAlternated.isDefined) firstAlternated.get :: alternatedShifts
      else alternatedShifts
    } else Nil

    alternated ::: mainShifts
  }
}

object PartitionRule extends Enumeration {
  val Longer, Shorter, Removed = Value


  implicit val format: Format[PartitionRule.Value] = EnumUtils.format(PartitionRule)
  implicit val stateMap: JdbcType[PartitionRule.Value] with BaseTypedType[PartitionRule.Value] = EnumUtils.methodMap(PartitionRule, MySQLProfile)
}