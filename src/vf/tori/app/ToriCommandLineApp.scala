package vf.tori.app

import utopia.access.http.Status
import vf.tori.controller.SalesItems

import scala.io.StdIn

/**
 * A command line application that opens the browser whenever new items are on sale
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.2
 */
object ToriCommandLineApp extends App
{
	Status.setup()
	
	SalesItems.startUpdates()
	
	println("Started checking for sales item updates in the background.\nPress enter to quit.")
	StdIn.readLine()
	println("\nStopping the background updates.\nSee you next time!")
}
