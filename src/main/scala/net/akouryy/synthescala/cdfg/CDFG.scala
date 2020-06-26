// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/asm/Program.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

final class CDFG:
  val blocks: mutable.SortedMap[BlockIndex, Block] = mutable.SortedMap[BlockIndex, Block]()
  val jumps: mutable.SortedMap[JumpIndex, Jump] = mutable.SortedMap[JumpIndex, Jump]()

final case class BlockIndex(indices: List[Int]) extends Ordered[BlockIndex]:
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

final case class JumpIndex(indices: List[Int]) extends Ordered[JumpIndex]:
  override def toString: String = s"Jump$indexString"

  def indexString: String = indices.mkString("_")

  import Ordering.Implicits._

  def compare(that: JumpIndex): Int = implicitly[Ordering[List[Int]]].compare(indices, that.indices)

object JumpIndex:
  private[this] var cnt = -1

  def generate(prefix: JumpIndex = JumpIndex(Nil)): JumpIndex =
    cnt += 1
    JumpIndex(prefix.indices :+ cnt) // O(n)

case class Block(
  i: BlockIndex,
  /*names: Map[String, Int /* node idx */],*/ nodes: Set[Node],
  inJump: JumpIndex, outJump: JumpIndex,
):
  def defs: Set[String] = nodes.flatMap:
    case Node.Input(_) => Nil
    case nd => nd.written

  def uses: Set[String] = nodes.flatMap:
    case Node.Output(_) => Nil
    case nd => nd.read

enum Node:
  case Input(name: String)
  case Const(value: Long, name: String)
  case Output(name: String)
  case BinOp(op: String, left: String, right: String, ans: String)
  case Call(fn: String, args: Seq[String], ret: String)

  def read: Seq[String] = this match
    case Input(_) => Nil
    case Const(_, _) => Nil
    case Output(n) => Seq(n)
    case BinOp(_, l, r, _) => Seq(l, r)
    case Call(_, as, _) => as

  def written: Option[String] = this match
    case Input(n) => Some(n)
    case Const(_, n) => Some(n)
    case Output(_) => None
    case BinOp(_, _, _, a) => Some(a)
    case Call(_, _, r) => Some(r)

end Node

enum Jump:
  val i: JumpIndex

  case StartFun(i: JumpIndex, outBlock: BlockIndex)

  case Return(i: JumpIndex, value: String, inBlock: BlockIndex)

  case Branch(
    i: JumpIndex, cond: String, inBlock: BlockIndex, truBlock: BlockIndex, flsBlock: BlockIndex,
  )
    // assert(input < tru && input < fls)

  /**
    @param inNames inNames[0...inBlocks.size][0...outNames.size]
  */
  case Merge(
    i: JumpIndex, override val inBlocks: IndexedSeq[BlockIndex],
    inNames: IndexedSeq[IndexedSeq[String]],
    outBlock: BlockIndex, outNames: IndexedSeq[String],
  )
    // assert(inputs.forall(_.bi < output))

  def inBlocks: Seq[BlockIndex] = this match
    case StartFun(_, _) => Nil
    case Return(_, _, ib) => Seq(ib)
    case Branch(_, _, ib, _, _) => Seq(ib)
    case Merge(_, ibs, _, _, _) => ibs

  def outBlocks: Seq[BlockIndex] = this match
    case StartFun(_, ob) => Seq(ob)
    case Return(_, _, _) => Nil
    case Branch(_, _, _, tb, fb) => Seq(tb, fb)
    case Merge(_, _, _, ob, _) => Seq(ob)

end Jump
