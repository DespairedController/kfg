package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.UndefinedName
import org.jetbrains.research.kfg.TF
import org.jetbrains.research.kfg.ir.value.Value

class ExitMonitorInst(owner: Value)
    : Instruction(UndefinedName.instance, TF.getVoidType(), arrayOf(owner)) {
    fun getOwner() = operands[0]

    override fun print() = "exit monitor ${getOwner()}"
}