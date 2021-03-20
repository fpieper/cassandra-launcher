package ai.pieper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.channels.UnresolvedAddressException
import java.nio.file.Paths


suspend fun fetchEtags(urls: List<String>, client: HttpClient): List<String> {
    try {
        val etags = urls.map { client.head<HttpResponse>(it).etag() }
        if (etags.contains(null)) {
            return emptyList()
        }
        return etags.requireNoNulls()
    } catch (e: ClientRequestException) {
    } catch (e: UnresolvedAddressException) {
        println("Error: repository address not resolvable.")
    } catch (e: HttpRequestTimeoutException) {
        println("Error: repository HTTP timeout.")
    } catch (e: Exception) {
        println("Error: ${e.localizedMessage}")
    }
    return emptyList()
}

suspend fun downloadFiles(urls: List<String>, destination: File, client: HttpClient): Boolean {
    try {
        val contents = urls.map{
            Pair(it.split("/").last(), client.get<ByteArray>(it))
        }
        for ((fileName, content) in contents) {
            val file = Paths.get(destination.name, fileName).toFile()
            file.writeBytes(content)
        }
        return true
    } catch (e: ClientRequestException) {
    } catch (e: UnresolvedAddressException) {
        println("Error: repository address not resolvable.")
    } catch (e: HttpRequestTimeoutException) {
        println("Error: repository HTTP timeout.")
    } catch (e: Exception) {
        println("Error: ${e.localizedMessage}")
    }
    return false
}

fun getJavaPath(): String {
    val path = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    return if (System.getProperty("os.name").startsWith("Win")) "$path.exe" else path
}

class Launcher(
    repository: String,
    enableConsole: Boolean,
    private val interval: Long,
    private val timeout: Long
) {
    private val repository: String = repository.trimEnd('/')
    private val configFileName = "default.config"
    private val jarFileName = "hackation.jar"
    private val workdir = File("cassandra-node")
    private val urls = listOf(this.repository + "/$configFileName", this.repository + "/$jarFileName")
    private val javaPath = getJavaPath()
    private val launchArguments = "-jar $jarFileName -config $configFileName${if (enableConsole) " -console" else ""}"
    private var runningProcess: Process? = null

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
        }
        engine {
            endpoint {
                connectTimeout = 5000
                connectAttempts = 5
            }
        }
    }

    private val isRunning: Boolean
        get() = runningProcess?.isAlive == true

    suspend fun loop() {
        var previousEtags: List<String> = emptyList()
        while (true) {
            val newEtags = fetchEtags(urls, client)
            previousEtags = if (isRunning) {
                handleRunning(previousEtags, newEtags)
            } else {
                handleNotRunning(newEtags)
            }
            delay(interval)
        }
    }

    private fun handleRunning(previousEtags: List<String>, newEtags: List<String>): List<String> {
        if (previousEtags == newEtags) {
            return newEtags
        }
        shutdown()
        return emptyList()
    }

    private suspend fun handleNotRunning(newEtags: List<String>): List<String> {
        if (newEtags.isEmpty()) {
            println("Repository not ready. Sleeping ...")
            return emptyList()
        }
        if (!prepareLaunch(newEtags)) {
            return emptyList()
        }
        launch()
        return newEtags
    }

    private suspend fun prepareLaunch(etags: List<String>): Boolean {
        workdir.deleteRecursively()
        workdir.mkdirs()
        val waitingDelay = (timeout * 1.5).toLong()
        for (second in (waitingDelay / 1000) downTo 1) {
            println("Waiting $second seconds to prepare launch ...")
            delay(1000)
        }
        if (etags != fetchEtags(urls, client)) {
            return false
        }
        println("Loading configuration and jar ...")
        if (!downloadFiles(urls, workdir, client)) {
            return false
        }
        if (etags != fetchEtags(urls, client)) {
            return false
        }
        return true
    }

    private fun launch() {
        println("Launching Cassandra ...")
        runningProcess = ProcessBuilder(
            javaPath,
            *(launchArguments.split(" ").toTypedArray())
        )
            .inheritIO()
            .directory(workdir)
            .start()
    }

    fun shutdown() {
        println("Shutting down Cassandra ...")
        runningProcess?.destroy()
    }
}
