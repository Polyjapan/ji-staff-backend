package scheduling.models

import scheduling.Period

private[models] case class TaskTimePartition(taskPartitionId: Option[Int], task: Int, staffsRequired: Int, splitIn: Int, slot: Period,
                             allowAlternateShifts: Boolean, // If enabled, staff changes will be divided in two -- other options ignored if disabled
                             firstShiftStartsLater: Boolean, // The first shift will start earlier for half of the staffs
                             firstShiftEndsEarlier: Boolean, // The first shift will end earlier for half the staff
                             lastShiftEndsEarlier: Boolean, // The last shift will end earlier for half the staff
                             lastShiftEndsLater: Boolean    // The last shift will end later for half the staff
                            ) {

  def produceSlots: Iterable[TaskSlot] = {
    val totalDuration = slot.timeEnd - slot.timeStart
    val shiftDuration = totalDuration / splitIn

    if (!allowAlternateShifts) {
      0 until splitIn map (i => {

        val start = slot.timeStart + i * shiftDuration
        val end = start + shiftDuration

        TaskSlot(None, task, staffsRequired, Period(slot.day, start, end))
      })
    } else {
      0 until splitIn flatMap (i => {
        val shiftedNum = staffsRequired / 2
        val normalNum = staffsRequired - shiftedNum
        val shift = shiftDuration / 2

        val start = slot.timeStart + i * shiftDuration
        val end = start + shiftDuration

        val (shiftStart, shiftEnd) = {
          if (i == 0) {
            val s = if (firstShiftStartsLater) start + shift else start
            val e = if (firstShiftEndsEarlier) end - shift else end + shift
            (s, e)
          } else if (i == splitIn - 1) {
            val s = if (firstShiftEndsEarlier) start - shift else start + shift
            val e =
              if (lastShiftEndsEarlier)
                start + shift
              else if (lastShiftEndsLater)
                end + shift
              else
                end

            (s, e)
          } else {
            val s = if (firstShiftEndsEarlier) start - shift else start + shift
            (s, s + shiftDuration)
          }
        }

        if (shiftStart < shiftEnd) {
          List(
            TaskSlot(None, task, normalNum, Period(slot.day, start, end)),
            TaskSlot(None, task, shiftedNum, Period(slot.day, shiftStart, shiftEnd))
          )
        } else {
          // Case where shifted shift is 0 in length
          List(TaskSlot(None, task, normalNum, Period(slot.day, start, end)))
        }
      })
    }
  }
}
