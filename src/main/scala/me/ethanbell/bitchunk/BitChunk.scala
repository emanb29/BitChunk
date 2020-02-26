package me.ethanbell.bitchunk

import scala.collection.BitSet
import scala.collection.immutable.BitSet.{fromBitMask => BitMask}
import Util._
import org.apache.commons.codec.binary.Hex

object BitChunk {
  // def apply(bs: BitSet): BitChunk = BitChunk(bs.max, bs)
  def safe(n: Int)(bs: BitSet): Seq[BitChunk] =
    if (bs.isEmpty) Seq(BitChunk.zeros(n))
    else
      (for {
        chunkStart <- 0 to bs.max by n // we count up from the 0th bit to the nth, and chunked based on that
        chunkEnd            = chunkStart + n
        bitsInChunk         = bs.range(chunkStart, chunkEnd)
        bitsAdjustedToStart = bitsInChunk.map(_ - chunkStart)
      } yield BitChunk(n, bitsAdjustedToStart)).reverse // results should be big-endian

  // TODO all these apply methods need to add 2^n to any negative values
  def apply(b: Byte): BitChunk = BitChunk(8, BitMask(Array(unsignedAsLong(b.toLong, 8))))

  def apply(c: Char): BitChunk = BitChunk(c.toByte)

  def apply(s: Short): BitChunk = BitChunk(16, BitMask(Array(unsignedAsLong(s.toLong, 16))))

  def apply(i: Int): BitChunk = BitChunk(32, BitMask(Array(unsignedAsLong(i.toLong, 32))))

  def apply(l: Long): BitChunk = BitChunk(64, BitMask(Array(l)))

  def apply(ls: Seq[Long]): BitChunk = BitChunk(ls.length, BitMask(ls.toArray))

  def fromBytes(bs: Seq[Byte]): BitChunk = bs.map(apply).reduceOption(_ ++ _).getOrElse(empty)

  // TODO this is not very elegant
  def apply(bi: BigInt): BitChunk = fromHexString(bi.toByteArray.dropWhile(_ == 0).toSeq.asHexBytes)

  def fromHexString(hexStr: String): BitChunk = {
    val bytesStr  = if (hexStr.substring(0, 2) == "0x") hexStr.drop(2) else hexStr
    val bytes     = Hex.decodeHex(bytesStr).toSeq
    val meatyBits = bytes.map(BitChunk.apply).reduce(_ ++ _)
    val leftPadBits =
      if (meatyBits.n < bytes.length * 8) BitChunk.zeros(bytes.length * 8 - meatyBits.n)
      else empty
    leftPadBits ++ meatyBits
  }

  val empty = zeros(0)

  def zeros(n: Int) = BitChunk(n, BitSet.empty)
}

/**
 * A BitChunk is a BitSet with a fixed length
 */
case class BitChunk(n: Int, bs: BitSet) {
  require(
    bs.isEmpty || bs.max < n,
    s"A BitChunk should be constructed with the `safe` method when the max bit may exceed the n-bound. n was $n; bs was $bs",
  )

  def apply(position: Int): Boolean = bs.contains(position)

  def grouped(newN: Int): Seq[BitChunk] = {
    require(newN != 0, "Cannot coerce into any number of 0-length chunks")
    val meatyBits       = BitChunk.safe(newN)(bs)
    val remainingChunks = Math.ceil((n - newN * meatyBits.length).toDouble / newN).toInt
    List.fill(remainingChunks)(BitChunk.zeros(newN)) ++ meatyBits
  }

  def toBytes(): Seq[Byte] = grouped(8).map { (bc: BitChunk) =>
    bc.bs.toBitMask.head.toByte
  }

  def &(other: BitChunk) = BitChunk(Math.max(n, other.n), bs & other.bs)

  def |(other: BitChunk) = BitChunk(Math.max(n, other.n), bs | other.bs)

  def ^(other: BitChunk) = BitChunk(Math.max(n, other.n), bs ^ other.bs)

  def ++(other: BitChunk) = BitChunk(n + other.n, bs.map(_ + other.n) | other.bs)

  // Left drop shift (drop values too big, ie at least n)
  def <<(shift: Int) = BitChunk(n, bs.map(_ + shift).filter(_ < n))

  // Right drop shift (drop values that would become negative)
  def >>(shift: Int) = BitChunk(n, bs.filter(whichBit => (whichBit - shift) >= 0).map(_ - shift))

  // Left circular shift from https://www.techiedelight.com/circular-shift-integer-k-positions/
  def <<>(shift: Int): BitChunk = (this << (shift % n)) | (this >> (n - (shift % n)))

  def reversed: BitChunk =
    BitChunk(n, bs.map { pos =>
      n - (pos + 1)
    })

  def isZeros: Boolean = this.bs.isEmpty

  // Adapted from https://www.geeksforgeeks.org/add-two-numbers-without-using-arithmetic-operators/
  def intAdd(other: BitChunk): BitChunk = {
    // TODO time permitting, rewrite this immutably
    var x = this
    var y = other
    while (!y.isZeros) {
      val carry = x & y
      x = x ^ y
      y = carry << 1;
    }
    x
  }

  def take(bits: Int): BitChunk = grouped(bits).head

  def drop(bits: Int): BitChunk = grouped(bits).drop(1).reduce(_ ++ _).take(n - bits)

  def takeRight(bits: Int): BitChunk = drop(n - bits)

  def toUnsignedBigInt()
    : BigInt             = BigInt(Array(0x00.toByte) ++ toBytes().toArray) // prefix with 0s so generated BigInt is positive
  def toBigInt(): BigInt = BigInt(toBytes().toArray)

  def asBinaryString: String =
    (for {
      whichBit <- 0 until n
    } yield {
      if (apply(whichBit)) '1' else '0'
    }).reverse.mkString("0b", "", "")

  def asHexBytes: String = toBytes().asHexBytes

  override def toString: String = s"BitChunk($n, $asHexBytes)"
}
