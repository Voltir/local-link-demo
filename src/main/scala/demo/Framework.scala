package demo

import org.scalajs.dom
import org.scalajs.dom.Element
import rx._

import scala.util.{Failure, Success}
import scalatags.JsDom.all._

/**
 * A minimal binding between Scala.Rx and Scalatags and Scala-Js-Dom
 */
object Framework {
  /**
   * Wraps reactive strings in spans, so they can be referenced/replaced
   * when the Rx changes.
   */
  implicit def RxStr[T](r: Rx[T])(implicit f: T => Modifier, ctx: Ctx.Owner): Modifier = {
    rxMod(Rx(span(r())))
  }

  /**
   * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
   * to propagate changes into the DOM via the element's ID. Monkey-patches
   * the Obs onto the element itself so we have a reference to kill it when
   * the element leaves the DOM (e.g. it gets deleted).
   */
  implicit def rxMod[T <: HtmlTag](r: Rx[T])(implicit ctx: Ctx.Owner): Modifier = {
    def rSafe = r.toTry match {
      case Success(v) => v.render
      case Failure(e) => span(e.toString, backgroundColor := "red").render
    }
    var last = rSafe
    r.triggerLater {
      val newLast = rSafe
      last.parentElement.replaceChild(newLast, last)
      last = newLast
    }
    bindNode(last)
  }
  implicit def RxAttrValue[T](implicit ctx: Ctx.Owner, av: AttrValue[T]) = new AttrValue[Rx[T]]{
    def apply(t: Element, a: Attr, r: Rx[T]): Unit = {
      r.trigger { av.apply(t, a, r.now)}
    }
  }
  implicit def RxStyleValue[T](implicit ctx: Ctx.Owner, sv: StyleValue[T]) = new StyleValue[Rx[T]]{
    def apply(t: Element, s: Style, r: Rx[T]): Unit = {
      r.trigger { sv.apply(t, s, r.now) }
    }
  }

}
