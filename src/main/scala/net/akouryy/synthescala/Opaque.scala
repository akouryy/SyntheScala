package net.akouryy.synthescala

opaque type NodeID = Int

def (nid: NodeID).num: Int = nid
def (num: Int).nodeID: NodeID = num

object NodeID:
  private var cnt = -1

  def generate(): NodeID =
    cnt += 1
    cnt

  def reset(): Unit = cnt = -1

given Eql[NodeID, NodeID] = Eql.derived
/** @see https://gitter.im/lampepfl/dotty?at=5dce7b6635889012b110dc98 */
given Ordering[NodeID] = Ordering.Int
