package vf.tori.app

import utopia.access.http.{Status, StatusGroup}
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.Request
import utopia.disciple.http.response.ResponseParser
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.flow.parse.Regex
import utopia.flow.util.IterateLines
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._

import java.nio.charset.StandardCharsets
import scala.collection.immutable.VectorBuilder
import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.util.{Failure, Success}

/**
 * A test application that reads sales items
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.1
 */
object StatusReadTestApp extends App
{
	DataType.setup()
	Status.setup()
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Tori-Notifier").executionContext
	
	val divStartRegex = Regex("\\<div")
	val divEndRegex = Regex("\\<\\/div\\>")
	
	val externalGateway = Gateway(allowBodyParameters = false, allowJsonInUriParameters = false)
	
	println("Requesting data...")
	externalGateway.responseFor(
		Request("https://www.tori.fi/uusimaa?q=polttopuu&cg=0&w=112&st=s&st=g&ca=18&l=0&md=th"))(
		ResponseParser.defaultOnEmpty[Vector[(String, String)]](Vector()) { (stream, headers, status) =>
			if (status.group == StatusGroup.Success) {
				implicit val codec: Codec = Codec(headers.charset.getOrElse(StandardCharsets.ISO_8859_1))
				println("Successful response received. Reading...")
				IterateLines.fromStream(stream) { linesIter =>
					val iter = linesIter.pollable
					// Finds a line with either "Ei tuloksia" (terminator)
					// or "class="add-details-left"" (indicating a sales item)
					Iterator.continually {
						iter.nextWhere { line => line.contains("Ei tuloksia") || line.contains("ad-details-left") }
							.flatMap { firstLine =>
								println(s"Key line found: $firstLine")
								// Case: No results found => terminates
								if (firstLine.contains("Ei tuloksia")) {
									println("No results found")
									None
								}
								// Case: Sales item found => processes it
								else {
									// Parses the whole div
									val divLines = divFrom(firstLine, iter)
									val title = divLines.find { _.contains("li-title") } match {
										case Some(line) => line.afterFirst(">").untilFirst("<")
										case None => ""
									}
									val price = divLines.find { _.contains("list_price ineuros") } match {
										case Some(line) => line.afterFirst(">").untilFirst("<")
										case None => ""
									}
									println(s"Found item:")
									divLines.foreach(println)
									Some(title -> price)
								}
							}
					}.takeWhile { _.isDefined }.flatten.toVector
				}
			}
			else
				Failure(new RequestFailedException(s"Service responded with $status"))
		})
		.waitForResult() match {
			case Success(response) =>
				response.body match {
					case Success(items) =>
						println(s"\n${ items.size } items available:")
						items.foreach { case (title, price) => println(s"$title: $price") }
					case Failure(error) => log(error, response.status.toString)
				}
			case Failure(error) => log(error)
		}
	
	private def divFrom(firstLine: String, linesIter: Iterator[String]) = {
		// Collects the right amount of lines
		var currentDepth = 1
		val linesBuilder = new VectorBuilder[String]()
		linesBuilder += firstLine
		while (currentDepth > 0 && linesIter.hasNext) {
			val line = linesIter.next()
			currentDepth += divStartRegex.startIndexIteratorIn(line).size - divEndRegex.startIndexIteratorIn(line).size
			/*
			if (line.contains("<div"))
				currentDepth += 1
			else if (line.contains("</div>"))
				currentDepth -= 1
			 */
			linesBuilder += line
		}
		
		linesBuilder.result()
	}
}
