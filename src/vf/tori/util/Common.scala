package vf.tori.util

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.ThreadPool
import utopia.flow.parse.JsonParser
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
 * Contains commonly used values
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.2
 */
object Common
{
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Tori-Notifier").executionContext
	implicit val jsonParser: JsonParser = JsonBunny
}
