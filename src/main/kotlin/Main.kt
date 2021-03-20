package ai.pieper

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required


suspend fun main(args: Array<String>) {
    val parser = ArgParser("java -jar cassandra-loader.jar")
    val repository by parser.option(ArgType.String, shortName = "r", description = "Repository").required()
    val console by parser.option(ArgType.Boolean, shortName = "c", description = "Enable console").default(false)
    val interval by parser.option(ArgType.Int, shortName = "i", description = "Check interval in seconds").default(2)
    val timeout by parser.option(ArgType.Int, shortName = "t", description = "Request timeout in seconds").default(30)
    parser.parse(args)
    val loader = Launcher(repository, console, interval.toLong() * 1000, timeout.toLong() * 1000)
    Runtime.getRuntime().addShutdownHook(Thread {
        loader.shutdown()
        println("Shutting down Cassandra Loader ...")
    })
    loader.loop()
}
