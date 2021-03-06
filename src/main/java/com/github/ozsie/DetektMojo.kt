package com.github.ozsie

import org.apache.maven.model.Dependency
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

const val CREATE_BASELINE = "-cb"
const val DEBUG = "--debug"
const val DISABLE_DEFAULT_RULE_SET = "-dd"
const val PARALLEL = "--parallel"
const val BASELINE = "-b"
const val CONFIG = "-c"
const val CONFIG_RESOURCE = "-cr"
const val FILTERS = "-f"
const val INPUT = "-i"
const val OUTPUT = "-o"
const val OUTPUT_NAME = "-on"
const val PLUGINS = "-p"

const val MDP_ID = "com.github.ozsie:detekt-maven-plugin"

const val DOT = "."
const val SLASH = "/"
const val SEMICOLON = ";"

abstract class DetektMojo : AbstractMojo() {
    @Parameter(property = "detekt.baseline", defaultValue = "")
    var baseline = ""

    @Parameter(property = "detekt.config", defaultValue = "")
    var config: String = ""

    @Parameter(property = "detekt.config-resource", defaultValue = "")
    var configResource = ""

    @Parameter(property = "detekt.debug", defaultValue = "false")
    var debug = false

    @Parameter(property = "detekt.disable-default-rulesets", defaultValue = "false")
    var disableDefaultRuleSets = false

    @Parameter(property = "detekt.filters")
    var filters = ArrayList<String>()

    @Parameter(property = "detekt.help", defaultValue = "false")
    var help = false

    @Parameter(property = "detekt.input", defaultValue = "\${basedir}/src")
    var input = "\${basedir}/src"

    @Parameter(property = "detekt.output", defaultValue = "\${basedir}/detekt")
    var output = "\${basedir}/detekt"

    @Parameter(property = "detekt.output-name", defaultValue = "")
    var outputName = ""

    @Parameter(property = "detekt.parallel", defaultValue = "false")
    var parallel = false

    @Parameter(property = "detekt.plugins")
    var plugins = ArrayList<String>()

    @Parameter(defaultValue = "\${project}", readonly = true)
    var mavenProject: MavenProject? = null

    @Parameter(defaultValue = "\${settings.localRepository}", readonly = true)
    var localRepoLocation = "\${settings.localRepository}"

    internal fun getCliSting() = ArrayList<String>().apply {
        useIf(debug, DEBUG)
                .useIf(disableDefaultRuleSets, DISABLE_DEFAULT_RULE_SET)
                .useIf(parallel, PARALLEL)
                .useIf(baseline.isNotEmpty(), BASELINE, baseline)
                .useIf(config.isNotEmpty(), CONFIG, config)
                .useIf(configResource.isNotEmpty(), CONFIG_RESOURCE, configResource)
                .useIf(filters.isNotEmpty(), FILTERS, filters.joinToString(SEMICOLON))
                .useIf(input.isNotEmpty(), INPUT, input)
                .useIf(output.isNotEmpty(), OUTPUT, output)
                .useIf(outputName.isNotEmpty(), OUTPUT_NAME, outputName)
                .useIf(plugins.isNotEmpty(), PLUGINS, plugins.buildPluginPaths(mavenProject, localRepoLocation))
    }

    internal fun <T> ArrayList<T>.log(): ArrayList<T> = apply {
        StringBuilder().apply {
            forEach { append(it).append(" ") }
            log.info("Args: $this".trim())
        }
    }
}

internal fun <T> ArrayList<T>.useIf(w: Boolean, vararg value: T) = apply { if (w) addAll(value) }

internal fun ArrayList<String>.buildPluginPaths(mavenProject: MavenProject?, localRepoLocation: String) =
        StringBuilder().apply {
            mavenProject?.let {
                this.buildPluginPaths(this@buildPluginPaths, it.getPlugin(MDP_ID), localRepoLocation)
            }
        }.toString().removeSuffix(SEMICOLON)

internal fun StringBuilder.buildPluginPaths(plugins: ArrayList<String>, mdp: Plugin, root: String) {
    plugins.forEach { plugin ->
        if (File(plugin).exists()) {
            append(plugin).append(SEMICOLON)
        } else {
            mdp.dependencies
                    ?.filter { plugin == it.getIdentifier() }
                    ?.forEach { append(it asPath root).append(SEMICOLON) }
        }
    }
}

internal infix fun Dependency.asPath(root: String) =
        "$root/${groupId.asPath()}/$artifactId/$version/$artifactId-$version.jar"

internal fun Dependency.getIdentifier() = "$groupId:$artifactId"

internal fun String.asPath() = replace(DOT, SLASH)
