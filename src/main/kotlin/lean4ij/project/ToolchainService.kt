package lean4ij.project

import com.google.common.io.Resources
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
class ElanService {

    val elanHomePath = Path.of(System.getProperty("user.home"), ".elan")
    val elanBinPath = elanHomePath.resolve("bin")
    val elanPath = elanBinPath.resolve("elan")

    fun getDefaultLakePath(): Path {
        val elanBinPath = Path.of(System.getProperty("user.home"), ".elan", "bin")
        return elanBinPath.resolve("lake")
    }
    fun getDefaultElanPath(): Path {
        val elanBinPath = Path.of(System.getProperty("user.home"), ".elan", "bin")
        return elanBinPath.resolve("elan")
    }

    /**
     * All versions are extracted locally from the lean4 repo with the following shell command:
     * ```
     * git --no-pager tag|grep v4|python -c 'import sys; print("".join(sorted(sys.stdin,key=lambda x:tuple(map(int,x.replace("v","").replace("-rc", ".").replace("-m", ".").split("."))), reverse=True)))'
     * ```
     * TODO maybe it can fetch locally or update in the pipeline
     */
    fun toolchains(includeRemote: Boolean): List<String> {
        return javaClass.classLoader.getResource("toolchains.txt").readText().split("\n")
    }

    fun commandForRunningElan(arguments: String, project: Project, environment: Map<String, String>) : GeneralCommandLine {
        val command = mutableListOf(elanPath.toString())
        // TODO what if it's empty?
        if (arguments.isNotEmpty()) {
            // TODO DRY this
            command.addAll(arguments.split(Regex("\\s+")))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            this.workDirectory = Path.of(project.basePath!!).toFile()
            this.environment.putAll(environment)
        }
    }

}

/**
 * TODO this is project level service
 *      there should be a global service similar for system level elan/lake
 */
@Service(Service.Level.PROJECT)
class ToolchainService(val project: Project) {
    // set to true when the toolchain could not properly be
    // initialized
    var toolChainPath: Path? = null
    var lakePath:  Path? = null
    var leanPath: Path? = null

    // This is for creating new project
    var defaultLakePath: Path? = null
    var defaultElanPath: Path? = null

    companion object {
        // TODO DRY this with the above elan service
        private val ARGUMENT_SEPARATOR = Regex("\\s+")
        const val TOOLCHAIN_FILE_NAME = "lean-toolchain"

        fun expectedToolchainPath(project: Project): Path {
            return Path.of(project.basePath!!, TOOLCHAIN_FILE_NAME)
        }
    }

    fun expectedToolchainPath(): Path {
        return expectedToolchainPath(this.project)
    }

    fun toolchainNotFound(): Boolean {
        return !expectedToolchainPath().toFile().isFile
    }

    /**
     * Run a lean file using lake env, for lean it's ran as the command
     * `lean --run <file>`,
     * but using lake it handles the imports like Mathlib
     * TODO test arguments and working directory
     */
    fun commandLineForRunningLeanFile(filePath: String, arguments: String = ""): GeneralCommandLine {
        val command = mutableListOf(lakePath.toString(), "env", "lean", "--run", filePath)
        if (arguments.isNotEmpty()) {
            command.addAll(arguments.split(ARGUMENT_SEPARATOR))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            // TODO it seems that running a file with lake requires the project root as the work directory
            this.workDirectory = Path.of(project.basePath!!).toFile()
        }
    }

    fun commandForRunningLake(arguments: String, environments: Map<String, String> = mapOf()): GeneralCommandLine {
        val command = mutableListOf(lakePath.toString())
        // TODO what if it's empty?
        if (arguments.isNotEmpty()) {
            command.addAll(arguments.split(ARGUMENT_SEPARATOR))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            this.workDirectory = Path.of(project.basePath!!).toFile()
            this.environment.putAll(environments)
        }
    }
}