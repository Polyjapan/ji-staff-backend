package scheduling.models

import play.api.libs.json.{Json, OFormat}
import scheduling.Period

case class TaskTimePartition(taskPartitionId: Option[Int],
                                             task: Int, staffsRequired: Int,
                                             shiftDuration: Int, slot: Period,
                                             alternateShifts: Boolean, // If enabled, staff changes will be divided in two -- other options ignored if disabled
                                            ) {

  def produceSlots: Iterable[TaskSlot] = {
    if (!alternateShifts) {
      slot.timeStart.until(slot.timeEnd, shiftDuration) map (start => {
        val end = Math.min(start + shiftDuration, slot.timeEnd) // Last shift may end earlier by default

        TaskSlot(None, task, staffsRequired, Period(slot.day, start, end))
      })
    } else {
      slot.timeStart.until(slot.timeEnd, shiftDuration) flatMap (start => {
        val shiftedNum = staffsRequired / 2
        val normalNum = staffsRequired - shiftedNum

        val shift = shiftDuration / 2
        val end = Math.min(slot.timeEnd, start + shiftDuration)

        val (shiftStart, shiftEnd) = {
          if (start == slot.timeStart) {
            (start, start + shift) // first shift is half the duration
          } else if (end == slot.timeEnd) {
            (start - shift, end) // last shift is 1.5 times the duration MAX
          } else {
            (start - shift, end - shift) // In the mean time, all shifts have the same length
          }
        }

        List(
          TaskSlot(None, task, normalNum, Period(slot.day, start, end)),
          TaskSlot(None, task, shiftedNum, Period(slot.day, shiftStart, shiftEnd))
        )

      })
    }
  }
}