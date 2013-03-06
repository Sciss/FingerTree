/*
 * FingerTree.scala
 * (FingerTree)
 *
 * Copyright (c) 2011-2013 Hanns Holger Rutz. All rights reserved.
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 2, june 1991 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License (gpl.txt) along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.fingertree

/**
 * Variant of a finger tree which adds a measure.
 */
object FingerTree {
  def empty[V, A](implicit m: Measure[A, V]): FingerTree[V, A] = new Empty[V](m.zero)

  def apply[V, A](elems: A*)(implicit m: Measure[A, V]): FingerTree[V, A] = {
    // TODO make this more efficient?
    // Maybe not worth the effort, the best we could do is
    // improve O(N logN) to become O(N).
    // However, it might be good for small trees of a few elements, saving some constant factor.
    // (We could overload apply with one, two, three and four element versions)
    var res = empty[V, A]
    elems.foreach(res :+= _)
    res
  }

  implicit private def digitMeasure[V, A](implicit m: Measure[A, V]): Measure[Digit[V, A], V] = new DigitMeasure(m)

  private final class DigitMeasure[V, A](m: Measure[A, V]) extends Measure[Digit[V, A], V] {
    def zero: V = m.zero
    def apply(n: Digit[V, A]): V = n.measure

    def |+|(a: V, b: V): V = m |+|(a, b)
    def |+|(a: V, b: V, c: V): V = m |+|(a, b, c)
  }

  // ---- Trees ----

  final private case class Single[V, A](measure: V, a: A) extends FingerTree[V, A] {
    def head = a
    def headOption: Option[A] = Some(a)

    def last = a
    def lastOption: Option[A] = Some(a)

    def tail(implicit m: Measure[A, V]): Tree = empty[V, A]
    def init(implicit m: Measure[A, V]): Tree = empty[V, A]

    def isEmpty = false

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = {
      val vPrefix = m(b)
      val prefix  = One(vPrefix, b)
      val vSuffix = m(a)
      val suffix  = One(vSuffix, a)
      Deep(m |+|(vPrefix, vSuffix), prefix, empty[V, Digit[V, A1]], suffix)
    }

    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = {
      val vPrefix = m(a)
      val prefix  = One(vPrefix, a)
      val vSuffix = m(b)
      val suffix  = One(vSuffix, b)
      Deep(m |+|(vPrefix, vSuffix), prefix, empty[V, Digit[V, A1]], suffix)
    }

    def ++[A1 >: A](right: FingerTree[V, A1])(implicit m: Measure[A1, V]): FingerTree[V, A1] = ???

    def viewLeft (implicit m: Measure[A, V]): ViewLeft [V, A] = ViewLeftCons [V, A](a, empty[V, A])
    def viewRight(implicit m: Measure[A, V]): ViewRight[V, A] = ViewRightCons[V, A](empty[V, A], a)

    def span(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, Tree) = {
      val e = empty[V, A]
      if (pred(m(a))) {
        (this, e)
      } else {
        (e, this)
      }
    }

    def takeWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree = {
      if (pred(m(a))) this else empty[V, A]
    }

    def dropWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree = {
      if (pred(m(a))) empty[V, A] else this
    }

    def span1(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, A, Tree) = {
      val e = empty[V, A]
      (e, a, e)
    }

    private[fingertree] def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A, Tree) = {
      val e = empty[V, A]
      (e, a, e)
    }

    private[fingertree] def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A) = {
      (empty[V, A], a) // correct???
    }

    def find1(pred: V => Boolean)(implicit m: Measure[A, V]): A = a

    private[fingertree] def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = (init, a)

    def toList: List[A] = a :: Nil
    def iterator: Iterator[A] = Iterator.single(a)

    override def toString = "(" + a + ")"
  }

  final private case class Deep[V, A](measure: V, prefix: Digit[V, A], tree: FingerTree[V, Digit[V, A]],
                                      suffix: Digit[V, A])
    extends FingerTree[V, A] {

    def isEmpty = false

    def head = prefix.head
    def headOption: Option[A] = Some(prefix.head)

    def last = suffix.last
    def lastOption: Option[A] = Some(suffix.last)

    def tail(implicit m: Measure[A, V]): Tree = viewLeft.tail
    def init(implicit m: Measure[A, V]): Tree = viewRight.init

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = {
      val vb = m(b)
      val vNew = m |+|(vb, measure)
      prefix match {
        case Four(_, d, e, f, g) =>
          val prefix      = Two(m |+|(vb, m(d)), b, d)
          val vTreePrefix = m |+|(m(e), m(f), m(g))
          val treeNew     = tree.+:[Digit[V, A1]](Three(vTreePrefix, e, f, g))
          Deep(vNew, prefix, treeNew, suffix)

        case partial =>
          Deep(vNew, b +: partial, tree, suffix)
      }
    }

    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = {
      val vb = m(b)
      val vNew = m |+|(vb, measure)
      suffix match {
        case Four(_, g, f, e, d) =>
          val vTreeSuffix = m |+|(m(g), m(f), m(e))
          val treeNew     = tree.:+[Digit[V, A1]](Three(vTreeSuffix, g, f, e))
          val suffix      = Two(m |+|(m(d), vb), d, b)
          Deep(vNew, prefix, treeNew, suffix)
        case partial =>
          Deep(vNew, prefix, tree, partial :+ b)
      }
    }

    def ++[A1 >: A](right: FingerTree[V, A1])(implicit m: Measure[A1, V]): FingerTree[V, A1] = ???

    def viewLeft(implicit m: Measure[A, V]): ViewLeft[V, A] =
      ViewLeftCons(prefix.head, deepLeft(prefix.tail, tree, suffix))

    def viewRight(implicit m: Measure[A, V]): ViewRight[V, A] =
      ViewRightCons(deepRight(prefix, tree, suffix.init), suffix.last)

    def span(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, Tree) =
      if (pred(measure)) {
        // split point lies after the last element of this tree
        (this, empty[V, A])
      } else {
        // predicate turns true inside the tree
        val (left, elem, right) = span1(pred, m.zero)
        (left, elem +: right)
      }

    def takeWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree =
      if (pred(measure)) {
        // split point lies after the last element of this tree
        this
      } else {
        // predicate turns true inside the tree
        val (left, _) = takeWhile1(pred, m.zero)
        left
      }

    def dropWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree =
      if (pred(measure)) {
        // split point lies after the last element of this tree
        empty[V, A]
      } else {
        // predicate turns true inside the tree
        val (elem, right) = dropWhile1(pred, m.zero)
        (elem +: right)
      }

    private def deepLeft(pr: MaybeDigit[V, A], tr: FingerTree[V, Digit[V, A]], sf: Digit[V, A])
                        (implicit m: Measure[A, V]): Tree = {
      if (pr.isEmpty) {
        tr.viewLeft match {
          case ViewLeftCons(a, tr1) => Deep(m |+|(a.measure, tr1.measure, sf.measure), a, tr1, sf)
          case _ => sf.toTree
        }
      } else {
        val prd = pr.get
        Deep(m |+|(prd.measure, tr.measure, sf.measure), prd, tr, sf)
      }
    }

    private def deepRight(pr: Digit[V, A], tr: FingerTree[V, Digit[V, A]], sf: MaybeDigit[V, A])
                         (implicit m: Measure[A, V]): Tree = {
      if (sf.isEmpty) {
        tr.viewRight match {
          case ViewRightCons(tr1, a) => Deep(m |+|(pr.measure, tr1.measure, a.measure), pr, tr1, a)
          case _ => pr.toTree
        }
      } else {
        val sfd = sf.get
        Deep(m |+|(pr.measure, tr.measure, sfd.measure), pr, tr, sfd)
      }
    }

    def span1(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, A, Tree) = span1(pred, m.zero)

    private[fingertree] def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A, Tree) = {
      val vPrefix = m |+|(init, prefix.measure)
      if (pred(vPrefix)) {
        val vTree = m |+|(vPrefix, tree.measure)
        if (pred(vTree)) {
          // in suffix
          val (l, x, r) = suffix.span1(pred, vTree)
          (deepRight(prefix, tree, l), x, r.toTree)
        } else {
          // split point found in middle
          val (ml, xs, mr) = tree.span1(pred, vPrefix)
          val (l, x, r) = xs.span1(pred, m |+|(vPrefix, ml.measure))
          (deepRight(prefix, ml, l), x, deepLeft(r, mr, suffix))
        }
      } else {
        // split point found in prefix
        val (l, x, r) = prefix.span1(pred, init)
        (l.toTree, x, deepLeft(r, tree, suffix))
      }
    }

    private[fingertree] def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A) = {
      val vPrefix = m |+|(init, prefix.measure)
      if (pred(vPrefix)) {
        val vTree = m |+|(vPrefix, tree.measure)
        if (pred(vTree)) {
          // in suffix
          val (l, x) = suffix.takeWhile1(pred, vTree)
          (deepRight(prefix, tree, l), x)
        } else {
          // split point found in middle
          val (ml, xs) = tree.takeWhile1(pred, vPrefix)
          val (l, x) = xs.takeWhile1(pred, m |+|(vPrefix, ml.measure))
          (deepRight(prefix, ml, l), x)
        }
      } else {
        // split point found in prefix
        val (l, x) = prefix.takeWhile1(pred, init)
        (l.toTree, x)
      }
    }

    private[fingertree] def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, Tree) = {
      val vPrefix = m |+|(init, prefix.measure)
      if (pred(vPrefix)) {
        val vTree = m |+|(vPrefix, tree.measure)
        if (pred(vTree)) {
          // in suffix
          val (x, r) = suffix.dropWhile1(pred, vTree)
          (x, r.toTree)
        } else {
          // split point found in middle
          val (ml, xs, mr) = tree.span1(pred, vPrefix)
          val (x, r) = xs.dropWhile1(pred, m |+|(vPrefix, ml.measure))
          (x, deepLeft(r, mr, suffix))
        }
      } else {
        // split point found in prefix
        val (x, r) = prefix.dropWhile1(pred, init)
        (x, deepLeft(r, tree, suffix))
      }
    }

    def find1(pred: V => Boolean)(implicit m: Measure[A, V]): A = find1(pred, m.zero)._2

    private[fingertree] def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = {
      val vPrefix = m |+|(init, prefix.measure)
      if (pred(vPrefix)) {
        // found in prefix
        prefix.find1(pred, init)
      } else {
        val vTree = m |+|(vPrefix, tree.measure)
        if (pred(vTree)) {
          // found in middle
          val (vTreeLeft, xs) = tree.find1(pred, vPrefix)
          xs.find1(pred, vTreeLeft)
        } else {
          // in suffix
          suffix.find1(pred, vTree)
        }
      }
    }

    def toList: List[A] = iterator.toList

    def iterator: Iterator[A] = {
      // Iterators compose nicely, ++ and flatMap are still lazy
      prefix.iterator ++ tree.iterator.flatMap(_.iterator) ++ suffix.iterator
    }

    override def toString = "(" + prefix + ", " + tree + ", " + suffix + ")"
  }

  final private case class Empty[V](measure: V) extends FingerTree[V, Nothing] {
    def isEmpty = true

    def head = throw new NoSuchElementException("head of empty finger tree")
    def headOption: Option[Nothing] = None

    def last = throw new NoSuchElementException("last of empty finger tree")
    def lastOption: Option[Nothing] = None

    def tail(implicit m: Measure[Nothing, V]): Tree =
      throw new UnsupportedOperationException("tail of empty finger tree")

    def init(implicit m: Measure[Nothing, V]): Tree =
      throw new UnsupportedOperationException("init of empty finger tree")

    def +:[A1](a1: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = Single(m(a1), a1)
    def :+[A1](a1: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1] = Single(m(a1), a1)

    def ++[A1 >: Nothing](right: FingerTree[V, A1])(implicit m: Measure[A1, V]): FingerTree[V, A1] = right

    def viewLeft (implicit m: Measure[Nothing, V]): ViewLeft [V, Nothing] = ViewNil[V]()
    def viewRight(implicit m: Measure[Nothing, V]): ViewRight[V, Nothing] = ViewNil[V]()

    def span(pred: V => Boolean)(implicit m: Measure[Nothing, V]): (Tree, Tree) = (this, this)

    def takeWhile(pred: V => Boolean)(implicit m: Measure[Nothing, V]): Tree = this
    def dropWhile(pred: V => Boolean)(implicit m: Measure[Nothing, V]): Tree = this

    def span1(pred: V => Boolean)(implicit m: Measure[Nothing, V]): (Tree, Nothing, Tree) =
      throw new UnsupportedOperationException("span1 on empty finger tree")

    private[fingertree] def span1(pred: V => Boolean, init: V)(implicit m: Measure[Nothing, V]): (Tree, Nothing, Tree) =
      throw new UnsupportedOperationException("span1 on empty finger tree")

    private[fingertree] def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[Nothing, V]): (Tree, Nothing) =
      throw new UnsupportedOperationException("takeWhile1 on empty finger tree")

    def find1(pred: V => Boolean)(implicit m: Measure[Nothing, V]): Nothing =
      throw new UnsupportedOperationException("find1 on empty finger tree")

    private[fingertree] def find1(pred: V => Boolean, init: V)(implicit m: Measure[Nothing, V]): (V, Nothing) =
      throw new UnsupportedOperationException("find1 on empty finger tree")

    def toList: List[Nothing] = Nil

    def iterator: Iterator[Nothing] = Iterator.empty

    override def toString = "()"
  }

  // ---- Views ----

  sealed trait ViewLeft[V, +A] {
    def head: A
    def tail: FingerTree[V, A]
  }

  sealed trait ViewRight[V, +A] {
    def init: FingerTree[V, A]
    def last: A
  }

  final case class ViewLeftCons [V, A](head: A, tail: FingerTree[V, A]) extends ViewLeft[V, A]
  final case class ViewRightCons[V, A](init: FingerTree[V, A], last: A) extends ViewRight[V, A]

  final case class ViewNil[V]() extends ViewLeft[V, Nothing] with ViewRight[V, Nothing] {
    private def notSupported(what: String) = throw new NoSuchElementException(what + " of empty view")

    def head: Nothing = notSupported("head")
    def last: Nothing = notSupported("last")

    def tail: FingerTree[V, Nothing] = notSupported("tail")
    def init: FingerTree[V, Nothing] = notSupported("init")
  }

  // ---- Digits ----

  private sealed trait MaybeDigit[V, +A] {
    protected type Tree = FingerTree[V, A]

    def isEmpty: Boolean
    def toTree(implicit m: Measure[A, V]): Tree
    def get: Digit[V, A]
  }

  private final case class Zero[V]() extends MaybeDigit[V, Nothing] {
    def isEmpty = true
    def toTree(implicit m: Measure[Nothing, V]): Tree = empty[V, Nothing]
    def get = throw new UnsupportedOperationException("get")
  }

  private sealed trait Digit[ V, +A ] extends MaybeDigit[ V, A ] {
    /**
     * It is an open question whether caching the measurements of digits is preferable or not. As Hinze and
     * Paterson write: "Because the length of the buffer is bounded by a constant, the number of ‘⊕’ operations
     * is also bounded. Another possibility is to cache the measure of a digit, adding to the cost of digit
     * construction but yielding a saving when computing the measure. The choice between these strategies
     * would depend on the expected balance of query and modification operations, but they would differ only
     * by a constant factor."
     *
     * The advantage of having the measurement stored (as we currently do) is that there is essentially no
     * difference between `Two` and `Node2` and `Three` and `Node3`, thus we use digits where Hinze and Paterson
     * use distinguished nodes.
     */
    def measure: V

    def head: A
    def last: A

    def tail(implicit m: Measure[A, V]): MaybeDigit[V, A]
    def init(implicit m: Measure[A, V]): MaybeDigit[V, A]

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1]
    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1]

    def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A)
    def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A, MaybeDigit[V, A])

    def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A)
    def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, MaybeDigit[V, A])

    // def toTree( implicit m: Measure[ A, V ]) : Tree

    def toList: List[A]
    def iterator: Iterator[A]
  }

  final private case class One[V, A](measure: V, a1: A) extends Digit[V, A] {
    def isEmpty = false
    def get: Digit[V, A] = this

    def head = a1
    def last = a1

    def tail(implicit m: Measure[A, V]): MaybeDigit[V, A] = Zero[V]()
    def init(implicit m: Measure[A, V]): MaybeDigit[V, A] = Zero[V]()

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] = Two(m |+|(m(b), measure), b, a1)
    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] = Two(m |+|(measure, m(b)), a1, b)

    def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = (init, a1)
    def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A, MaybeDigit[V, A]) = {
      val e = Zero[V]()
      (e, a1, e)
    }

    def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A) = {
      (Zero[V](), a1) // correct???
    }

    def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, MaybeDigit[V, A]) = {
      (a1, Zero[V]()) // correct???
    }

    def toTree(implicit m: Measure[A, V]): Tree = Single(measure, a1)

    def toList: List[A] = a1 :: Nil

    def iterator: Iterator[A] = Iterator.single(a1)

    override def toString = "(" + a1 + ")"
  }

  final private case class Two[V, A](measure: V, a1: A, a2: A) extends Digit[V, A] {
    def isEmpty = false
    def get: Digit[V, A] = this

    def head = a1
    def last = a2

    def tail(implicit m: Measure[A, V]): MaybeDigit[V, A] = One(m(a2), a2)
    def init(implicit m: Measure[A, V]): MaybeDigit[V, A] = One(m(a1), a1)

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] = Three(m |+|(m(b), measure), b, a1, a2)
    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] = Three(m |+|(measure, m(b)), a1, a2, b)

    def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = {
      val v1 = m |+|(init, m(a1))
      if (pred(v1)) (init, a1) else (v1, a2)
    }

    def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val v1  = m |+|(init, va1)
      val e   = Zero[V]()
      if (pred(v1)) {
        (One(va1, a1), a2, e) // (a1), a2, ()
      } else {
        (e, a1, One(m(a2), a2)) // (), a1, (a2)
      }
    }

    def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A) = {
      val va1 = m(a1)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        (One(va1, a1), a2) // (a1), a2
      } else {
        val e = Zero[V]()
        (e, a1) // (), a1
      }
    }

    def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        val e = Zero[V]()
        (a2, e) // a2, ()
      } else {
        (a1, One(m(a2), a2)) // a1, (a2)
      }
    }

    def toTree(implicit m: Measure[A, V]): Tree = {
      Deep(measure, One(m(a1), a1), empty[V, Digit[V, A]], One(m(a2), a2))
    }

    def toList: List[A] = a1 :: a2 :: Nil

    def iterator: Iterator[A] = toList.iterator

    override def toString = "(" + a1 + ", " + a2 + ")"
  }

  final private case class Three[V, A](measure: V, a1: A, a2: A, a3: A) extends Digit[V, A] {
    def isEmpty = false
    def get: Digit[V, A] = this

    def head = a1
    def last = a3

    def tail(implicit m: Measure[A, V]): MaybeDigit[V, A] = Two(m |+|(m(a2), m(a3)), a2, a3)
    def init(implicit m: Measure[A, V]): MaybeDigit[V, A] = Two(m |+|(m(a1), m(a2)), a1, a2)

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] =
      Four(m |+|(m(b), measure), b, a1, a2, a3)

    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): Digit[V, A1] =
      Four(m |+|(measure, m(b)), a1, a2, a3, b)

    def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = {
      val v1 = m |+|(init, m(a1))
      if (pred(v1)) (init, a1)
      else {
        val v12 = m |+|(v1, m(a2))
        if (pred(v12)) (v1, a2) else (v12, a3)
      }
    }

    def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val va2 = m(a2)
      val v1 = m |+|(init, va1)
      if (pred(v1)) {
        if (pred(m |+|(v1, va2))) {
          (Two(m |+|(va1, va2), a1, a2), a3, Zero[V]()) // (a1, a2), a3, ()
        } else {
          (One(va1, a1), a2, One(m(a3), a3)) // (a1), a2, (a3)
        }
      } else {
        (Zero[V](), a1, Two(m |+|(va2, m(a3)), a2, a3)) // (), a1, (a2, a3)
      }
    }

    def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A) = {
      val va1 = m(a1)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        val va2 = m(a2)
        if (pred(m |+|(v1, va2))) {
          (Two(m |+|(va1, va2), a1, a2), a3) // (a1, a2), a3
        } else {
          (One(va1, a1), a2) // (a1), a2
        }
      } else {
        // (), a1
        (Zero[V](), a1)
      }
    }

    def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val va2 = m(a2)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        if (pred(m |+|(v1, va2))) {
          (a3, Zero[V]()) // a3, ()
        } else {
          (a2, One(m(a3), a3)) // a2, (a3)
        }
      } else {
        (a1, Two(m |+|(va2, m(a3)), a2, a3)) // a1, (a2, a3)
      }
    }

    def toTree(implicit m: Measure[A, V]): Tree = {
      Deep(measure, Two(m |+|(m(a1), m(a2)), a1, a2), empty[V, Digit[V, A]], One(m(a3), a3))
    }

    def toList: List[A] = a1 :: a2 :: a3 :: Nil

    def iterator: Iterator[A] = toList.iterator

    override def toString = "(" + a1 + ", " + a2 + ", " + a3 + ")"
  }

  final private case class Four[V, A](measure: V, a1: A, a2: A, a3: A, a4: A) extends Digit[V, A] {
    def isEmpty = false
    def get: Digit[V, A] = this

    def head = a1
    def last = a4

    def tail(implicit m: Measure[A, V]): MaybeDigit[V, A] =
      Three(m |+|(m(a2), m(a3), m(a4)), a2, a3, a4)

    def init(implicit m: Measure[A, V]): MaybeDigit[V, A] =
      Three(m |+|(m(a1), m(a2), m(a3)), a1, a2, a3)

    def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]) =
      throw new UnsupportedOperationException("+: on digit four")

    def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]) =
      throw new UnsupportedOperationException(":+ on digit four")

    def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A) = {
      val v1 = m |+|(init, m(a1))
      if (pred(v1)) (init, a1)
      else {
        val v12 = m |+|(v1, m(a2))
        if (pred(v12)) (v1, a2)
        else {
          val v123 = m |+|(v12, m(a3))
          if (pred(v123)) (v12, a3) else (v123, a4)
        }
      }
    }

    def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val va2 = m(a2)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        val v12 = m |+|(v1, va2)
        val va3 = m(a3)
        if (pred(v12)) {
          val va12 = m |+|(va1, va2)
          if (pred(m |+|(v12, va3))) {
            (Three(m |+|(va12, va3), a1, a2, a3), // (a1, a2, a3), a4, ()
              a4,
              Zero[V]())
          } else {
            (Two(va12, a1, a2), // (a1, a2), a3, (a4)
              a3,
              One(m(a4), a4))
          }
        } else {
          (One(va1, a1), // (a1), a2, (a3, a4)
            a2,
            Two(m |+|(va3, m(a4)), a3, a4))
        }
      } else {
        (Zero[V](), // (), a1, (a2, a3, a4)
          a1,
          Three(m |+|(va2, m(a3), m(a4)), a2, a3, a4))
      }
    }

    def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (MaybeDigit[V, A], A) = {
      val va1 = m(a1)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        val va2 = m(a2)
        val v12 = m |+|(v1, va2)
        if (pred(v12)) {
          val va3 = m(a3)
          val va12 = m |+|(va1, va2)
          if (pred(m |+|(v12, va3))) {
            (Three(m |+|(va12, va3), a1, a2, a3), a4) // (a1, a2, a3), a4
          } else {
            (Two(va12, a1, a2), a3) // (a1, a2), a3
          }
        } else {
          (One(va1, a1), a2) // (a1), a2
        }
      } else {
        (Zero[V](), a1) // (), a1
      }
    }

    def dropWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (A, MaybeDigit[V, A]) = {
      val va1 = m(a1)
      val va2 = m(a2)
      val v1  = m |+|(init, va1)
      if (pred(v1)) {
        val v12 = m |+|(v1, va2)
        val va3 = m(a3)
        if (pred(v12)) {
          if (pred(m |+|(v12, va3))) {
            (a4, Zero[V]()) // a4, ()
          } else {
            (a3, One(m(a4), a4)) // a3, (a4)
          }
        } else {
          (a2, Two(m |+|(va3, m(a4)), a3, a4)) // a2, (a3, a4)
        }
      } else {
        (a1, Three(m |+|(va2, m(a3), m(a4)), a2, a3, a4)) // a1, (a2, a3, a4)
      }
    }

    def toTree(implicit m: Measure[A, V]): Tree = {
      Deep(measure, Two(m |+|(m(a1), m(a2)), a1, a2), empty[V, Digit[V, A]], Two(m |+|(m(a3), m(a4)), a3, a4))
    }

    def toList: List[A] = a1 :: a2 :: a3 :: a4 :: Nil

    def iterator: Iterator[A] = toList.iterator

    override def toString = "(" + a1 + ", " + a2 + ", " + a3 + ", " + a4 + ")"
  }
}

sealed trait FingerTree[ V, +A ] {
  import FingerTree._

  protected type Tree = FingerTree[V, A]

  /**
   * Queries whether the tree is empty or not
   *
   * @return  `true` if the tree is empty
   */
  def isEmpty: Boolean

  /**
   * Queries the measure of the tree, which might be its size or sum
   *
   * @return  the measure of the tree
   */
  def measure: V

  /**
   * Returns the first (left-most) element in the tree. Throws a runtime exception if performed on an empty tree.
   *
   * @return  the head element
   */
  def head: A

  /**
   * Returns the first (left-most) element in the tree as an option.
   *
   * @return  the head element (`Some`), or `None` if the tree is empty
   */
  def headOption: Option[A]

  /**
   * Returns a copy of the tree with the first (head) element removed. Throws a runtime exception if performed
   * on an empty tree.
   *
   * @param m the measure used to update the tree's structure
   * @return  the new tree with the first element removed
   */
  def tail(implicit m: Measure[A, V]): Tree

  /**
   * Returns the last (right-most) element in the tree. Throws a runtime exception if performed on an empty tree.
   *
   * @return  the last element
   */
  def last: A

  /**
   * Returns the last (right-most) element in the tree as an option.
   *
   * @return  the last element (`Some`), or `None` if the tree is empty
   */
  def lastOption: Option[A]

  /**
   * Drops the last element of the tree.
   *
   * @return  the tree where the last element has been removed
   */
  def init(implicit m: Measure[A, V]): Tree

  /**
   * Prepends an element to the tree.
   *
   * @param b the element to prepend
   * @param m the measure used to update the tree's measure
   * @return  the new tree with the element prepended
   */
  def +:[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1]

  /**
   * Appends an element to the tree.
   *
   * @param b the element to append
   * @param m the measure used to update the tree's structure
   * @return  the new tree with the element appended
   */
  def :+[A1 >: A](b: A1)(implicit m: Measure[A1, V]): FingerTree[V, A1]

  def ++[A1 >: A](right: FingerTree[V, A1])(implicit m: Measure[A1, V]): FingerTree[V, A1]

  def viewLeft (implicit m: Measure[A, V]): ViewLeft[V, A]
  def viewRight(implicit m: Measure[A, V]): ViewRight[V, A]

  /**
   * Creates an `Iterator` over the elements of the tree
   *
   * @return  a fresh `Iterator` for the tree elements
   */
  def iterator: Iterator[A]

  /**
   * Converts the tree to a `List` representation.
   *
   * @return  a `List` constructed from the elements in the tree
   */
  def toList: List[A]

  /**
   * Same as `span1`, but drops the discerning element, instead only returning the left and right tree.
   * Unlike `span1`, this is an allowed operation on an empty tree.
   *
   * @param pred a test function applied to the elements of the tree from left to right, until a
   *             the test returns `false`.
   * @return  the split tree, as a `Tuple2` with the left and the right tree
   */
  def span(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, Tree)

  /**
   * Traverses the tree until a predicate on an element becomes `false`, and then splits the tree,
   * returning the elements before that element (the prefix for which the predicate holds),
   * the element itself (the first for which the predicate does not hold), and the remaining elements.
   *
   * This method is somewhat analogous to the `span` method in standard Scala collections, the difference
   * being that the predicate tests the tree's measure and not individual elements.
   *
   * Note that the returned discerning element corresponds to the last element in the tree, if
   * `pred` returns `true` for every element (rather than a runtime exception being thrown).
   *
   * If the tree is empty, this throws a runtime exception.
   *
   * @param pred a test function applied to the elements of the tree from left to right, until a
   *             the test returns `true`.
   * @return  the split tree, as a `Tuple3` with the left tree, the discerning element, and the right tree
   */
  def span1(pred: V => Boolean)(implicit m: Measure[A, V]): (Tree, A, Tree)

  private[fingertree] def span1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A, Tree)

  /**
   * Traverses the tree until a predicate on an element becomes `true`, and then returns that
   * element. Note that if `pred` returns `false` for every element, the last element in the
   * tree is returned (rather than a runtime exception being thrown).
   *
   * If the tree is empty, this throws a runtime exception.
   *
   * @param pred a test function applied to the elements of the tree from left to right, until a
   *             the test returns `true`.
   * @return  the discerning element
   */
  def find1(pred: V => Boolean)(implicit m: Measure[A, V]): A

  private[fingertree] def find1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (V, A)

  //   /**
//    * Appends two elements to the tree.
//    *
//    * @param b1 the first element to append (this will become the before-last element in the tree)
//    * @param b2 the second element to append (this will become the last element in the tree)
//    * @param m the measure used to update the tree's structure
//    * @return  the new tree with the elements appended
//    */
//   def append2[ A1 >: A ]( b1: A1, b2: A1 )( implicit m: Measure[ A1, V ]) : FingerTree[ V, A1 ]

  def takeWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree
  def dropWhile(pred: V => Boolean)(implicit m: Measure[A, V]): Tree

  private[fingertree] def takeWhile1(pred: V => Boolean, init: V)(implicit m: Measure[A, V]): (Tree, A)
}
