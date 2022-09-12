package vf.tori.controller

import utopia.access.http.StatusGroup
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.Request
import utopia.disciple.http.response.ResponseParser
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.async.AsyncExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.Regex
import utopia.flow.util.IterateLines
import utopia.flow.util.logging.Logger
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import vf.tori.model.SalesItem

import java.nio.charset.StandardCharsets
import scala.collection.immutable.VectorBuilder
import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.util.Failure

/**
 * Reads items being given away, or on sale from Tori.fi
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.2
 */
object ReadSalesItems
{
	private val divStartRegex = Regex("\\<div")
	private val divEndRegex = Regex("\\<\\/div\\>")
	
	private val externalGateway = Gateway(Vector(JsonBunny),
		allowBodyParameters = false, allowJsonInUriParameters = false)
	
	/**
	 * Performs a sales item search
	 * @param search The search string (e.g. "polttopuu")
	 * @param exc Implicit execution context
	 * @param log Implicit logger
	 * @return Items on sale (async). May contain a failure.
	 */
	def apply(search: String)(implicit exc: ExecutionContext, log: Logger) = {
		externalGateway.responseFor(
			Request(s"https://www.tori.fi/uusimaa?q=$search&cg=0&w=112&st=s&st=g&ca=18&l=0&md=th"))(
			ResponseParser.defaultOnEmpty[Vector[SalesItem]](Vector()) { (stream, headers, status) =>
				if (status.group == StatusGroup.Success) {
					implicit val codec: Codec = Codec(headers.charset.getOrElse(StandardCharsets.ISO_8859_1))
					IterateLines.fromStream(stream) { linesIter =>
						val iter = linesIter.pollable
						// Finds a line with either "Ei tuloksia" (terminator)
						// or "class="add-details-left"" (indicating a sales item)
						Iterator.continually {
							iter.nextWhere { line => line.contains("Ei tuloksia") || line.contains("ad-details-left") }
								.flatMap { firstLine =>
									// Case: No results found => terminates
									if (firstLine.contains("Ei tuloksia"))
										None
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
										Some(SalesItem(title, price.untilFirst("&").trim.double))
									}
								}
						}.takeWhile { _.isDefined }.flatten.toVector
					}
				}
				else
					Failure(new RequestFailedException(s"Service responded with $status"))
			})
			.tryMapIfSuccess { response =>
				if (response.isSuccess)
					response.body
				else
					Failure(new RequestFailedException(s"Service responded with status ${ response.status }"))
			}
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
