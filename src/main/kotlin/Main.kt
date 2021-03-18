package ai.pieper

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required


suspend fun main(args: Array<String>) {
    val parser = ArgParser("java -jar cassandra-loader.jar")
    val repository by parser.option(ArgType.String, shortName = "r", description = "Repository").required()
    val console by parser.option(ArgType.Boolean, shortName = "c", description = "Enable console").default(false)
    val checkDelay by parser.option(ArgType.Int, shortName = "d", description = "Check delay").default(3000)
    val launchDelay by parser.option(ArgType.Int, shortName = "l", description = "Launch delay").default(15000)
    parser.parse(args)
    val loader = Loader(repository, console, checkDelay.toLong(), launchDelay.toLong())
    Runtime.getRuntime().addShutdownHook(Thread {
        loader.shutdown()
        println("Shutdown Cassandra Loader ...")
    })
    loader.loop()
}
