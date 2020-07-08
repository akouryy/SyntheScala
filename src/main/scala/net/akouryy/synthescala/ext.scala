package net.akouryy.synthescala

def [T](seq: Iterable[T]).soleElement: T =
  assert(seq.sizeIs == 1, seq)
  seq.head

def [A, B, CC[_], C](iter: collection.IterableOps[A, CC, C]).zipStrict
(that: Iterable[B]): CC[(A, B)] =
  assert(iter.sizeCompare(that) == 0)
  iter.zip(that)

def [A](seq: collection.Seq[A]).getIndexOf(elem: A): Option[Int] = seq.getIndexOf(elem, 0)

def [A](seq: collection.Seq[A]).getIndexOf(elem: A, from: Int): Option[Int] =
  val i = seq.indexOf(elem, from)
  Option.when(i >= 0)(i)

def [A](seq: collection.Seq[A]).getIndexWhere(pred: A => Boolean, from: Int = 0): Option[Int] =
  val i = seq.indexWhere(pred, from)
  Option.when(i >= 0)(i)

def (self: String).tr(from: String, to: String): String =
  def trChars(s: String): Seq[Char] =
    """\\-|(.)-(.)""".r.replaceAllIn(s, m =>
      if m.group(1) == null
        "-"
      else
        m.group(1).charAt(0).to(m.group(2).charAt(0)).mkString
    )

  val convs = trChars(from).zipStrict(trChars(to)).toMap
  self.map(c => convs.getOrElse(c, c))

def (i: Int).width: Int = i.toBinaryString.length
def (i: Long).width: Int = i.toBinaryString.length

def [T](i: T).clamp(low: T, high: T)(using ord: Ordering[T]): T =
  if ord.lt(i, low)
    low
  else if ord.gt(i, high)
    high
  else
    i
