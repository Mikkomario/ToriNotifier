package vf.tori.controller

import utopia.flow.async.process.LoopingProcess
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.container.ObjectsFileContainer
import utopia.flow.parse.file.container.SaveTiming.OnJvmClose
import utopia.flow.parse.file.FileExtensions._
import vf.tori.model.SalesItem
import vf.tori.util.Common._

import java.awt.Desktop
import java.net.URI
import scala.util.{Failure, Success, Try}

/**
 * Caches and manages sales item data locally
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.2
 */
object SalesItems
{
	// ATTRIBUTES   -------------------------
	
	private val container = new ObjectsFileContainer("data/sales-items.json", SalesItem, OnJvmClose)
	private lazy val updateLoop = LoopingProcess.static(10.minutes) { _ =>
		ReadSalesItems("polttopuu").onComplete { _.flatten match {
			case Success(items) => container.current = items
			case Failure(error) => log(error, "Sales items reading failed")
		} }
	}
	
	
	// INITIAL CODE -------------------------
	
	// Opens the browser every time new items are added
	pointer.addListener { change =>
		if (change.newValue.exists { item => !change.oldValue.contains(item) }) {
			val result = {
				if (Desktop.isDesktopSupported) {
					val desktop = Desktop.getDesktop
					if (desktop.isSupported(Desktop.Action.BROWSE))
						Try { desktop.browse(new URI(
							"https://www.tori.fi/uusimaa?q=polttopuu&cg=0&w=112&st=s&st=g&ca=18&l=0&md=th")) }
					else
						Failure(new UnsupportedOperationException("Internet browsing is not supported in desktop"))
				}
				else
					Failure(new UnsupportedOperationException("Desktop operations are not supported"))
			}
			result.failure.foreach { log(_, "Couldn't open the browser") }
		}
		
		// Also prints updates to console
		println("\n---")
		println(s"${ Now.toLocalDateTime }: Sales items updated")
		change.newValue.foreach { item => println(s"\t- $item") }
		println("---")
	}
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return The current sales items data
	 */
	def current = container.current
	/**
	 * @return Pointer to the current sales items data
	 */
	def pointer = container.pointer.valueView
	
	
	// OTHER    -----------------------------
	
	/**
	 * Starts automated background updates
	 */
	def startUpdates() = updateLoop.runAsync()
}
