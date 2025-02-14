package org.jetbrains.research.kfg.builder.cfg

import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.UsageContext
import org.jetbrains.research.kfg.visitor.MethodVisitor

class ThrowCatchNormalizer(override val cm: ClassManager, val ctx: UsageContext) : MethodVisitor {
    override fun cleanup() {}

    override fun visit(method: Method) = with(ctx) {
        super.visit(method)

        for (block in method.basicBlocks.toList()) {
            if (block.size > 1) continue
            if (block.successors.size != 1) continue
            if (block.predecessors.size != 1) continue

            val successor = block.successors.first()
            val predecessor = block.predecessors.first()
            if (predecessor.handlers != successor.handlers) continue
            for (handler in successor.handlers intersect predecessor.handlers) {
                block.linkThrowing(handler)
            }
        }
    }
}