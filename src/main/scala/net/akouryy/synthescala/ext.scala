package net.akouryy.synthescala

def [T](seq: Iterable[T]).soleElement: T =
  assert(seq.sizeIs == 1, seq)
  seq.head
