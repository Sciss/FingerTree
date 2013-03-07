/*
 * IndexedSeq.scala
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

object IndexedSeq {
  private implicit val measure = Measure.Indexed

  def empty[A]: IndexedSeq[A] = new Impl[A](FingerTree.empty[Int, A])

  def apply[A](elems: A*): IndexedSeq[A] = new Impl[A](FingerTree.apply[Int, A](elems: _*))

  private final class Impl[A](protected val tree: FingerTree[Int, A]) extends IndexedSeq[A] {
    protected def m: Measure[A, Int] = measure

    protected def wrap(tree: FingerTree[Int, A]): IndexedSeq[A] = new Impl(tree)

    protected def isSizeGtPred  (i: Int) = _ > i
    protected def isSizeLteqPred(i: Int) = _ <= i

    def size: Int = tree.measure

    override def toString = tree.iterator.mkString("Seq(", ", ", ")")
  }
}
sealed trait IndexedSeq[A] extends IndexedSeqLike[Int, A, IndexedSeq[A]]
