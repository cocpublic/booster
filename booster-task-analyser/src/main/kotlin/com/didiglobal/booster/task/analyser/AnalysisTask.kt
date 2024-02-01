package com.didiglobal.booster.task.analyser

import com.android.build.gradle.api.BaseVariant
import com.didiglobal.booster.BOOSTER
import com.didiglobal.booster.cha.asm.AsmClassSetCache
import com.didiglobal.booster.gradle.AGP
import com.didiglobal.booster.gradle.GTE_V7_2
import com.didiglobal.booster.kotlinx.touch
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import java.io.File

abstract class AnalysisTask : DefaultTask() {

    @get:Internal
    var variant: BaseVariant? = null

    @get:Internal
    lateinit var classSetCache: AsmClassSetCache

    @Internal
    final override fun getGroup(): String = BOOSTER

    @Internal
    abstract override fun getDescription(): String

    abstract fun analyse()

    /**
     * only agp>=7.2.0 this map is not empty
     */
    @get:Internal
    val sourceSetMap: Map<String, String> by lazy(::initSourceSetMap)

    private fun initSourceSetMap(): Map<String, String> {
        // if agp >=7.2.0 create map<Relative Path，Absolute Path> from sourceSetMap file
        return if (GTE_V7_2 && variant != null) {
            val variant = requireNotNull(this.variant)
            val artifacts: FileCollection? = AGP.run { variant.allArtifacts["SOURCE_SET_PATH_MAP"] }
            artifacts?.filter {
                it.exists()
            }?.flatMap {
                it.readLines()
            }?.map {
                it.split(" ")
            }?.filter { it.size >= 2 }
                ?.associateBy(keySelector = { it[0] }, valueTransform = { it[1] }) ?: emptyMap()
        } else emptyMap()
    }
}

fun File.compatAapt2ResSourcePath(sourceSetMap:Map<String,String>): File {
    if (sourceSetMap.isEmpty()) {
        return this
    }
    val array = this.absolutePath.split(":")

    val key = if(array.isNotEmpty())array[0].substringAfterLast("/") else ""
    return if (key.isNotEmpty() && sourceSetMap.containsKey(key))
        File("${sourceSetMap[key]}${array[1]}")
    else this
}

internal val AnalysisTask.reportDir: File
    get() = project.buildDir
            .resolve("reports")
            .resolve(Build.ARTIFACT)
            .resolve(javaClass.kotlin.category)
            .resolve(variant?.dirName ?: ".")

internal fun AnalysisTask.report(name: String): File {
    return reportDir.resolve(name).resolve("index.${name}").touch()
}
