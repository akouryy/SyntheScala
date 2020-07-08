// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/asm/Program.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import net.akouryy.{synthescala => base}
import scala.collection.mutable

final case class CDFG(val arrayDefs: toki.ArrayDefMap, val main: CDFGFun):
  def node(blockIndex: BlockIndex, nodeID: NodeID): Node =
    main.blocks(blockIndex).nodes(nodeID)

final class CDFGFun(val fnName: String, val retTyp: toki.Type, val params: Seq[toki.Entry]):
  val blocks: mutable.SortedMap[BlockIndex, Block] = mutable.SortedMap[BlockIndex, Block]()
  val jumps: mutable.SortedMap[JumpIndex, Jump] = mutable.SortedMap[JumpIndex, Jump]()

  def inJump(b: Block) = jumps(b.inJumpIndex)

  def apply(bi: BlockIndex): Block = blocks(bi)
  def apply(ji: JumpIndex): Jump = jumps(ji)

final case class BlockIndex(indices: List[Int]) extends Ordered[BlockIndex] derives Eql:
  override def toString: String = s"Block$indexString"

  def indexString: String = indices.mkString("_")

  import Ordering.Implicits._

  def compare(that: BlockIndex): Int =
    implicitly[Ordering[List[Int]]].compare(indices, that.indices)

object BlockIndex:
  private var cnt = -1

  def generate(prefix: BlockIndex = BlockIndex(Nil)): BlockIndex = {
    cnt += 1
    BlockIndex(prefix.indices :+ cnt) // O(n)
  }

  def reset() = cnt = -1

final case class JumpIndex(indices: List[Int]) extends Ordered[JumpIndex] derives Eql:
  override def toString: String = s"Jump$indexString"

  def indexString: String = indices.mkString("_")

  import Ordering.Implicits._

  def compare(that: JumpIndex): Int = implicitly[Ordering[List[Int]]].compare(indices, that.indices)

object JumpIndex:
  private[this] var cnt = -1

  def generate(prefix: JumpIndex = JumpIndex(Nil)): JumpIndex =
    cnt += 1
    JumpIndex(prefix.indices :+ cnt) // O(n)

  def reset() = cnt = -1

case class Block(
  i: BlockIndex,
  inputs: Seq[Label], outputs: Seq[Label],
  /*names: Map[String, Int /* node idx */],*/ nodes: Map[NodeID, Node],
  arrayDeps: UnweightedGraph[NodeID],
  inJumpIndex: JumpIndex, outJump: JumpIndex,
):
  def defs: Iterable[Label] = nodes.values.flatMap(_.written)

  def uses: Iterable[Label] = nodes.values.flatMap(_.read)

  lazy val writeMap: Map[Label, NodeID] =
    nodes.valuesIterator.flatMap(nd => nd.written.map(_ -> nd.id)).toMap

  lazy val readMap: Map[Label, Seq[NodeID]] =
    val res = mutable.Map.empty[Label, List[NodeID]]
    for nd <- nodes.valuesIterator; lab <- nd.read do
      res(lab) = nd.id :: res.getOrElse(lab, Nil)
    res.toMap

  def getWritingNode(label: Label): Option[NodeID] =
    Option.unless(inputs.contains(label))(writeMap(label))

  def getReadingNodes(label: Label): Seq[NodeID] =
    if outputs.contains(label)
      readMap.getOrElse(label, Seq.empty)
    else
      readMap(label)

  def states(using sche: schedule.Schedule): IndexedSeq[State] =
    stateToNodes(sche).keySet.toIndexedSeq.sorted

  def stateToNodes(sche: schedule.Schedule): collection.MultiDict[State, Node] =
    mutable.SortedMultiDict.from:
      for (nid -> node) <- nodes.toSeq yield
        sche.nodeStates(nid) -> node
end Block

enum Node derives Eql:
  val id: NodeID

  case Const(val id: NodeID, value: Long, name: Label)
  case BinOp(val id: NodeID, op: base.BinOp, left: VC, right: VC, ans: Label)
  case Call(val id: NodeID, fn: String, args: Seq[Label], ret: Label)
  case GetReq(val id: NodeID, awa: NodeID, arr: Label, index: Label)
  /** GetAwait */
  case GetAwa(val id: NodeID, req: NodeID, arr: Label, ret: Label)
  case Put(val id: NodeID, arr: Label, index: Label, value: Label)

  override lazy val hashCode = scala.util.hashing.MurmurHash3.productHash(this)

  def withID: (NodeID, Node) = id -> this

  def isMemoryRelated: Boolean = this match
    case _: (GetReq | GetAwa | Put) => true
    case _ => false

  def read: Seq[Label] = this match
    case _: Const => Nil
    case BinOp(_, _, l, r, _) => Seq(l.getV, r.getV).flatten
    case Call(_, _, as, _) => as
    case GetReq(_, _, _, index) => Seq(index)
    case _: GetAwa => Nil
    case Put(_, _, index, value) => Seq(index, value)

  def written: Option[Label] = this match
    case Const(_, _, n) => Some(n)
    case BinOp(_, _, _, _, a) => Some(a)
    case Call(_, _, _, r) => Some(r)
    case _: GetReq => None
    case GetAwa(_, _, _, ret) => Some(ret)
    case _: Put => None

end Node

enum Jump:
  val i: JumpIndex

  case StartFun(i: JumpIndex, outBlock: BlockIndex)

  case Return(i: JumpIndex, value: Label, inBlock: BlockIndex)

  case TailCall(i: JumpIndex, fn: String, args: Seq[Label], inBlock: BlockIndex)

  case Branch(
    i: JumpIndex, cond: Label, inBlock: BlockIndex, truBlock: BlockIndex, flsBlock: BlockIndex,
  )
    // assert(input < tru && input < fls)

  /**
    @param inNames inNames[0...inBlocks.size][0...outNames.size]
  */
  case Merge(
    i: JumpIndex, override val inBlocks: IndexedSeq[BlockIndex],
    inNames: IndexedSeq[IndexedSeq[Label]],
    outBlock: BlockIndex, outNames: IndexedSeq[Label],
  )
    // assert(inputs.forall(_.bi < output))

  def inBlocks: Seq[BlockIndex] = this match
    case StartFun(_, _) => Nil
    case Return(_, _, ib) => Seq(ib)
    case TailCall(_, _, _, ib) => Seq(ib)
    case Branch(_, _, ib, _, _) => Seq(ib)
    case Merge(_, ibs, _, _, _) => ibs

  def outBlocks: Seq[BlockIndex] = this match
    case StartFun(_, ob) => Seq(ob)
    case _: (Return | TailCall) => Nil
    case Branch(_, _, _, tb, fb) => Seq(tb, fb)
    case Merge(_, _, _, ob, _) => Seq(ob)

end Jump
