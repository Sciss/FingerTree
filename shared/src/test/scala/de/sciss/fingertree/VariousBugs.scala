package de.sciss.fingertree

object VariousBugs {
  def main(args: Array[String]): Unit = {
    args.headOption.getOrElse("") match {
      case "--init" => initBug()
      case "--find" => findBug()
    }
  }

  def findBug(): Unit = {
    implicit val m = Measure.Indexed
    //      val r    = 0 to 11
    //      val t    = FingerTree[ Int, Int ]( r: _* )
    //      val res  = t.find1( _ > 7 )
    //      println( res )

    def test(n: Int): Unit = {
      val r   = 0 to n
      val t   = FingerTree[Int, Int](r: _*)
      val res: Seq[Int] = r.map(i => t.find1(_ > i)._2)
      require(res == r)
    }

    test(1000000)

    println("Ok.")
  }

  def initBug(): Unit = {
    implicit val m = Measure.Unit
    val seq1  = FingerTree[Unit, String]("a", "b", "c")
    val seq1i = seq1.init
    val seq2  = seq1.tail
    val seq2i = seq2.init
    val seq3  = seq2.tail
    val seq3i = seq3.init

    val seq1t = seq1.tail

    println("Aqui.")
  }
}
