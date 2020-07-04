// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/asm/Program.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import net.akouryy.{synthescala => base}
import scala.collection.mutable

final case class CDFG(val arrayDefs: toki.ArrayDefMap, val main: CDFGFun)

final class CDFGFun(val fnName: String, val params: Seq[Label]):
  val blocks: mutable.SortedMap[BlockIndex, Block] = mutable.SortedMap[BlockIndex, Block]()
  val jumps: mutable.SortedMap[JumpIndex, Jump] = mutable.SortedMap[JumpIndex, Jump]()

  def inJump(b: Block) = jumps(b.inJumpIndex)

final case class BlockIndex(indices: List[Int]) extends Ordered[BlockIndex] derives Eql:
  override def toString: String = s"Block$indexString"

  def indexString: String = indices.mkString("_")

  import Ordering.Implicits._

  def compare(that: BlockIndex): Int =
    implicitly[Ordering[List[Int]]].compare(indices, that.indices)

object BlockIndex:
  private[this] var cnt = -1

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
  /*names: Map[String, Int /* node idx */],*/ nodes: Set[Node],
  inJumpIndex: JumpIndex, outJump: JumpIndex,
):
  def defs: Set[Label] = nodes.flatMap:
    case Node.Input(_) => Nil
    case nd => nd.written

  def uses: Set[Label] = nodes.flatMap:
    case Node.Output(_) => Nil
    case nd => nd.read

  lazy val writeMap: Map[Label, Node] = nodes.flatMap(nd => nd.written.map(_ -> nd)).toMap

  lazy val readMap: Map[Label, Seq[Node]] =
    val res = mutable.Map.empty[Label, List[Node]]
    for nd <- nodes; id <- nd.read do
      res(id) = nd :: res.getOrElse(id, Nil)
    res.toMap

  lazy val inputs: Set[Node] = nodes.filter(_.isInput)

  def stateToNodes(sche: schedule.Schedule): collection.MultiDict[State, Node] =
    mutable.SortedMultiDict.from:
      for node <- nodes if !node.isInput yield
        sche.nodeStates(i, node) -> node
end Block

enum Node derives Eql:
  case Input(name: Label)
  case Const(value: Long, name: Label)
  case Output(name: Label)
  case BinOp(op: base.BinOp, left: Label, right: Label, ans: Label)
  case Call(fn: String, args: Seq[Label], ret: Label)
  case Get(arr: Label, index: Label, ret: Label)

  override lazy val hashCode = scala.util.hashing.MurmurHash3.productHash(this)

  def isInput: Boolean = this match
    case _: (Input | Const) => true
    case _ => false

  def read: Seq[Label] = this match
    case Input(_) => Nil
    case Const(_, _) => Nil
    case Output(n) => Seq(n)
    case BinOp(_, l, r, _) => Seq(l, r)
    case Call(_, as, _) => as
    case Get(_, index, _) => Seq(index)

  def written: Option[Label] = this match
    case Input(n) => Some(n)
    case Const(_, n) => Some(n)
    case Output(_) => None
    case BinOp(_, _, _, a) => Some(a)
    case Call(_, _, r) => Some(r)
    case Get(_, _, ret) => Some(ret)

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
