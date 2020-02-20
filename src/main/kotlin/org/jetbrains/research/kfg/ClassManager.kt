package org.jetbrains.research.kfg

import com.abdullin.kthelper.KtException
import com.abdullin.kthelper.defaultHashCode
import org.jetbrains.research.kfg.builder.cfg.CfgBuilder
import org.jetbrains.research.kfg.ir.Class
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.ir.OuterClass
import org.jetbrains.research.kfg.ir.value.ValueFactory
import org.jetbrains.research.kfg.ir.value.instruction.InstructionFactory
import org.jetbrains.research.kfg.type.TypeFactory
import org.jetbrains.research.kfg.util.parse
import org.objectweb.asm.tree.ClassNode
import java.util.jar.JarFile

class Package(name: String) {
    val name: String
    val isConcrete: Boolean = name.lastOrNull() != '*'

    companion object {
        val defaultPackage = Package("*")
        val emptyPackage = Package("")
        fun parse(string: String) = Package(string.replace('.', '/'))
    }

    init {
        this.name = when {
            isConcrete -> name
            else -> name.removeSuffix("*").removeSuffix("/")
        }
    }

    fun isParent(other: Package) = when {
        isConcrete -> this.name == other.name
        else -> other.name.startsWith(this.name)
    }

    fun isChild(other: Package) = other.isParent(this)
    fun isParent(name: String) = isParent(Package(name))
    fun isChild(name: String) = isChild(Package(name))

    override fun toString() = "$name${if (isConcrete) "" else "/*"}"
    override fun hashCode() = defaultHashCode(name, isConcrete)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as Package
        return this.name == other.name && this.isConcrete == other.isConcrete
    }
}

class ClassManager(jar: JarFile, val config: KfgConfig = KfgConfigBuilder().build()) {
    val value = ValueFactory(this)
    val instruction = InstructionFactory(this)
    val type = TypeFactory(this)
    val concreteClasses: Set<ConcreteClass>

    val `package` get() = config.`package`
    val flags get() = config.flags

    private val classNodes = hashMapOf<String, ClassNode>()
    private val classes = hashMapOf<String, Class>()

    init {
        val jarClasses = jar.parse(`package`, flags)
        classNodes.putAll(jarClasses)
        jarClasses.forEach { (name, cn) ->
            classes.getOrPut(name) { ConcreteClass(this, cn) }.init()
        }

        val failedClasses = hashSetOf<String>()
        classes.forEach { (name, klass) ->
            try {
                klass.allMethods.forEach { method ->
                    if (!method.isAbstract) CfgBuilder(this, method).build()
                }
            } catch (e: KfgException) {
                if (config.failOnError) throw e
                failedClasses += name
            } catch (e: KtException) {
                if (config.failOnError) throw e
                failedClasses += name
            }
        }
        // this is fucked up, but i don't know any other way to do this
        // if `failOnError` option is enabled and we have some failed classes, we need to
        // rebuild all the methods so they will not use invalid instance of ConcreteClass for failing class
        if (!config.failOnError && failedClasses.isNotEmpty()) {
            val oldClasses = classes.toMap()
            classes.clear()
            for ((name, klass) in oldClasses) {
                when (name) {
                    !in failedClasses -> classes.getOrPut(name) { ConcreteClass(this, klass.cn) }.init()
                    else -> classes.getOrPut(name) { OuterClass(this, ClassNode().also { it.name = name }) }.init()
                }

            }

            classes.forEach { (name, klass) ->
                if (name !in failedClasses) {
                    klass.allMethods.forEach { method ->
                        if (!method.isAbstract) CfgBuilder(this, method).build()
                    }
                }
            }
        }
        concreteClasses = classes.values.mapNotNull { it as? ConcreteClass }.toSet()
    }

    fun get(cn: ClassNode) = classes.getOrPut(cn.name) { ConcreteClass(this, cn) }

    fun getByName(name: String): Class {
        var cn = classNodes[name]
        return if (cn != null) get(cn) else {
            cn = ClassNode()
            cn.name = name
            OuterClass(this, cn)
        }
    }

    fun getByPackage(`package`: Package): List<Class> = concreteClasses.filter { `package`.isParent(it.`package`) }
}