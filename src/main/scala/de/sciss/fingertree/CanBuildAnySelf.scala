package de.sciss.fingertree

import collection._
import collection.generic._
import scala.collection.mutable.Builder
//import Scalaz._

private[fingertree] trait CanBuildAnySelf[CC[_]] {
  import CanBuildAnySelf._

  def builder[A, B]: CanBuildSelf[CC, A, B]

  final def apply[A, B](): Builder[B, CC[B]] = builder[A, B].apply

  final def apply[A, B](f: CC[A]): Builder[B, CC[B]] = builder[A, B].apply(f)
}

private[fingertree] object CanBuildAnySelf {
  type CanBuildSelf[CC[_], A, B] = CanBuildFrom[CC[A], B, CC[B]]

  type CanBuildSelfExistential[CC[_]] = CanBuildFrom[CC[A], B, CC[B]] forSome {type A; type B}

  implicit def GenericCanBuildSelf[CC[_] : CanBuildSelfExistential]: CanBuildAnySelf[CC] = new CanBuildAnySelf[CC] {
    // TODO: Is this ever unsafe?
    def builder[A, B] = implicitly[CanBuildSelfExistential[CC]].asInstanceOf[CanBuildSelf[CC, A, B]]
  }
}