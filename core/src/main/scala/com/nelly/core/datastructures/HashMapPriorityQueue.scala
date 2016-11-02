package com.nelly.core.datastructures


import scala.collection.mutable.{ArrayBuffer, HashMap}

/**
 * * :Problem: PriorityQueue implementation does not reorder or maintain priority when the items stored within them
 *  are mutated. 
 *  
 *  This data structure, is a hash map backed max heap. 
 *  The hash map enable O(1) item lookup and O(logn) to rearrange a mutated object : In contrast,
 *  *  if current implementation of Priority Queue supported this ability, it will cost O(n)
 *  
 *  * Use cases, include
 *    1.  Counting things and also been able to figure out the item with the most count efficiently
 *  *  *  
 * @param ordering
 * @tparam T
 */
class HashMapPriorityQueue[T](implicit ordering: Ordering[T]) {

  val nodes = new ArrayBuffer[T]()
  val indexedMap = new HashMap[T, Int]()
  
  
  def size(): Int = nodes.length
  
  def orderingId() : String = ordering.toString

  /**
   * *  O(1)
   * @param t
   * @return
   */
  def contains(t: T) : Boolean = indexedMap.get(t) match {
    case Some(e) => true
    case _ => false
  }

  /**
   * * O(1) 
   * * This gets a stored version of this element if it exists otherwise None 
   * @param t
   * @return
   */
  def getStoredVersion(t: T) : Option[T] = indexedMap.get(t) match {
    case Some(e) => Option(nodes(e))
    case _ => None
  }

  /**
   * * O(1)
   * @return Some(T) if nodes has at least one element otherwise None
   */
  def peek(): Option[T] = size() match {
      case 0 => None
      case _ => Option(nodes.head)
  }
  
  /**
   * * O(n) Worst case, are elements are equal since we are not removing the elements which will necesitate calling
   * * percolateDown/trickleUp
   * *  
  In the case of max values with duplicates  
        <ordKey:45, id:aa>
       /                   \
      /                     \
   <ordKey:45, id:aab> <ordKey:45, id:aac>  => [<ordKey:45, id:aa>,<ordKey:45, id:aab>,<ordKey:45, id:aac>]
     /                          \
    /                            \ 
  <ordKey:4, id:caa>        <ordKey:5, id:baa>
 */
  def peekValues(): Seq[T] = {
    val items = size()
    items match {
      case 0 => Seq.empty
      case _ => peekValues(0, Seq(),items)
    }
  }
  

  private[this] def peekValues(start: Int, acc: Seq[T], items: Int): Seq[T] = if( 0 <= start && start < items) {
    val compare = ordering.compare(nodes(0), nodes(start)) 
    if( 0 == compare){
      val nAcc = acc ++ Seq(nodes(start)) 
      nAcc ++ peekValues(left(start), Seq(), items) ++ peekValues(right(start), Seq(), items)
    } else acc
  } else {
    acc
  }
  
  
  /**
   * * O(n)
   * @return true if the structure is a valid heap; false otherwise.
   */
  def isMaxHeap:  Boolean = {
   
    def isValidOrder(index: Int, root: T) :Boolean ={
      val child = nodes(index)
      val compare = ordering.compare(root, child)
      if(compare < 0) false else true
    }

    val items = size()

    Range(0, items).forall(
       index => {
         val root = nodes(index)
         val leftChildIndex = left(index)

         if(leftChildIndex < items){
           isValidOrder(leftChildIndex, root)
         } else {
           val rightChildIndex = left(index)
           if(rightChildIndex < items) isValidOrder(rightChildIndex, root) else true
         }
       }
    )
    
  }
  
  def isEmpty(): Boolean = size == 0

  /**
   * removes max item from list and maintains max heap invariant
   * @return max item
   */
  def  poll() :Option[T] = synchronized {
    val items = size()
    items match {
      case 0 => None
      case _ => {
        val root  = nodes(0)
        indexedMap.remove(root)
        if(items != 1){
          val last = nodes.last
          this.set(0, last)
          percolateDown(0, items)
        }
        nodes.dropRight(1)
        Option(root)
      }
    }
  }

  /**
   * Inserts/updates the element into the heap
   * @param elm the element to be inserted/updated
   */
  def put(elm: T) :Unit= {

    val items = size()
    indexedMap.get(elm) match {
      case Some(i) => {
        if(i.toString.contains("announce")){
          println(s" announce:: ${nodes(i)}")
          
        }
        set(i, elm)
        repairHeap(i)
      } //found in map
      case None => { // not found
        indexedMap.put(elm, items)
        nodes += elm
        trickleUp(items)
      }
    }
  }

  /**
   * * THe object that has changed, call this method rearrange the heap
   * @param culprit
   * @return
   */
  def repairHeap(culprit: T) :Boolean = {
    indexedMap.get(culprit) match {
      case Some(index) => { repairHeap(index); true }
      case None => false
    }
  }
  
  
  private[this] def repairHeap(i: Int){
    percolateDown(i, size())
    trickleUp(i)
  }

  
  private[this] def trickleUp(index: Int) :Unit =  index match {
      case 0 =>
      case _ if index > 0 => {
        val parentIndex = parent(index)
        val currentNode = nodes(index)
        val parentNode = nodes(parentIndex)
        val compareTo = ordering.compare(parentNode, currentNode)
        if(compareTo < 0){
          swap(index, parentIndex)
          trickleUp(parentIndex)
        }
      }
      case _ =>
  }

  private[this] def percolateDown(index: Int, items: Int) :Unit = {

		val leftChildIndex = left(index)
		val rightChildIndex = right(index)
		val root = nodes(index)

		var largestIndex = index
		var currentLargest  = root

		if(leftChildIndex < items){

      val leftChild = nodes(leftChildIndex)
			if(ordering.compare(leftChild, currentLargest) > 0){
        currentLargest = leftChild
        largestIndex = leftChildIndex
			}

		}

		if(rightChildIndex < items){

      val rightChild = nodes(rightChildIndex)
      if(ordering.compare(rightChild, currentLargest) > 0){
        currentLargest = rightChild
        largestIndex = rightChildIndex
      }
		}

		if(largestIndex != index){
      swap(index, largestIndex)
      percolateDown(largestIndex, items)
		}

	}

   
  private[this] def parent(index: Int) :Int = ((index+1) / 2)-1

  private[this] def left(index: Int) :Int = (2*(index+1))-1
  
  private[this] def right(index: Int) :Int = 2*(index+1)

  private[this] def  set(index: Int , e: T) :Unit = synchronized{
    nodes.update(index, e)
    indexedMap.put(e, index)
  }

  private[this] def swap( a: Int,  b: Int) :Unit = synchronized {
    val o = nodes(a)
    this.set(a,nodes(b))
    this.set(b,o)
  }
}
