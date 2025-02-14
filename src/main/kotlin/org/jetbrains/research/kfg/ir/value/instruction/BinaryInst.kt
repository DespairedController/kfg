package org.jetbrains.research.kfg.ir.value.instruction

import org.jetbrains.research.kfg.ir.value.Name
import org.jetbrains.research.kfg.ir.value.UsageContext
import org.jetbrains.research.kfg.ir.value.Value

class BinaryInst internal constructor(
    name: Name,
    val opcode: BinaryOpcode,
    lhv: Value,
    rhv: Value,
    ctx: UsageContext
) : Instruction(name, lhv.type, arrayOf(lhv, rhv), ctx) {

    val lhv: Value
        get() = ops[0]

    val rhv: Value
        get() = ops[1]

    override fun print() = "$name = $lhv $opcode $rhv"
    override fun clone(ctx: UsageContext): Instruction = BinaryInst(name.clone(), opcode, lhv, rhv, ctx)
}