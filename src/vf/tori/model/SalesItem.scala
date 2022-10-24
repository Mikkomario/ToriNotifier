package vf.tori.model

import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible

object SalesItem extends FromModelFactoryWithSchema[SalesItem]
{
	override val schema = ModelDeclaration("name" -> StringType)
	
	override protected def fromValidatedModel(model: Model) = apply(model("name"), model("price"))
}

/**
 * Represents an item on sale (or being given away)
 * @author Mikko Hilpinen
 * @since 11.9.2022, v0.2
 * @param name Name of this item
 * @param price Price of this item in euros, if known
 */
case class SalesItem(name: String, price: Option[Double] = None) extends ModelConvertible
{
	override def toModel = Model.from("name" -> name, "price" -> price)
	
	override def toString = price match {
		case Some(price) => s"$name: $price €"
		case None => name
	}
}
