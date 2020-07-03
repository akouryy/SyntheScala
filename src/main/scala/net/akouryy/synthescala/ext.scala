package net.akouryy.synthescala

def [T](seq: Iterable[T]).soleElement: T =
  assert(seq.sizeIs == 1, seq)
  seq.head

def [A, B, CC[_], C](iter: collection.IterableOps[A, CC, C]).
zipStrict(that: Iterable[B]): CC[(A, B)] =
  assert(iter.sizeCompare(that) == 0)
  iter.zip(that)

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
