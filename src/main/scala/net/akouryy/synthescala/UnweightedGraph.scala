package net.akouryy.synthescala

import scala.collection.mutable

class UnweightedGraph[T]():
  private[synthescala] val edges = mutable.Map.empty[T, mutable.Set[T]]
  private val revEdges = mutable.Map.empty[T, mutable.Set[T]]

  def goForward(vertex: T): collection.Set[T] = edges(vertex)
  def goBackward(vertex: T): collection.Set[T] = revEdges(vertex)

  def addVertex(v: T): Unit =
    edges(v) = mutable.Set.empty
    revEdges(v) = mutable.Set.empty

  def addEdge(v: T, w: T): Unit =
    edges(v) += w
    revEdges(w) += v

  def addEdge(vw: (T, T)): Unit = addEdge(vw._1, vw._2)

  override def toString: String = s"UnweightedGraph($edges)"
