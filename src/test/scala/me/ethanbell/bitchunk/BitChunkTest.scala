package me.ethanbell.bitchunk

import org.scalatest.funsuite.AnyFunSuite
import org.scalacheck._
import org.scalatestplus.scalacheck.Checkers

import scala.collection.BitSet

class BitChunkTest extends AnyFunSuite with Checkers with TestHelpers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 400, sizeRange = 256, workers = 1)
  implicit val genBitSet: Gen[BitSet] =
    Gen.containerOf[Set, Int](Gen.posNum[Int].map(_ - 1)).map(idxs => BitSet(idxs.toSeq: _*))
  implicit val genBitChunks: Gen[Seq[BitChunk]] = for {
    bs <- genBitSet
    n  <- Gen.posNum[Int]
  } yield BitChunk.safe(n)(bs)
  implicit val genBitChunk: Gen[BitChunk] = genBitChunks.map(_.reduce(_ ++ _))
  test(
    "Byte-generate BitChunks should always return a total of n bits of BitChunk"
  ) {
    check(Prop.forAll { (bs: Seq[Byte]) =>
      BitChunk.fromBytes(bs).n === bs.length * 8
    })
  }
  test(
    "Padded grouped BitChunks should have consistent sizes"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n = Gen.choose(1, bc.n).getOrThrow()
      bc.groupedLeftPadded(n).map(_.n).forall(_ === n)
    })
  }
  test(
    "Padded grouped BitChunks should have combined size at least == original size"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n = Gen.choose(1, bc.n).getOrThrow()
      bc.groupedLeftPadded(n).map(_.n).sum >= bc.n
    })
  }
  test(
    "Unpadded grouped BitChunks should have consistent sizes except possibly the last"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n               = Gen.choose(1, bc.n).getOrThrow()
      val (heads :+ tail) = bc.grouped(n)
      heads.map(_.n).forall(_ === n) && (tail.n <= n)
    })
  }
  test(
    "Unpadded grouped BitChunks should have combined size exactly == original size"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n = Gen.choose(1, bc.n).getOrThrow()
      bc.grouped(n).map(_.n).sum === bc.n
    })
  }
  test(
    "Left-dropped grouped BitChunks should have consistent sizes except possibly the first"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n               = Gen.choose(1, bc.n).getOrThrow()
      val (head +: tails) = bc.groupedDroppingLeft(n)
      tails.map(_.n).forall(_ === n) && (head.n <= n)
    })
  }
  test(
    "Left-dropped grouped BitChunks should have combined size exactly == original size"
  ) {
    check(Prop.forAll(genBitChunk.suchThat(_.n >= 1)) { (bc: BitChunk) =>
      val n = Gen.choose(1, bc.n).getOrThrow()
      println(s"Chose $n and $bc")
      bc.groupedDroppingLeft(n).map(_.n).sum === bc.n
    })
  }
  test("0x012012 and 0xabcabc grouped examples should behave correctly") {
    BitChunk.fromHexString("abcabc").grouped(16) === Seq(
      BitChunk.fromHexString("abca"),
      BitChunk.fromHexString("bc")
    )
    BitChunk.fromHexString("012012").grouped(16) === Seq(
      BitChunk.fromHexString("0120"),
      BitChunk.fromHexString("12")
    )
    BitChunk.fromHexString("abcabc").groupedLeftPadded(16) === Seq(
      BitChunk.fromHexString("00ab"),
      BitChunk.fromHexString("cabc")
    )
    BitChunk.fromHexString("012012").groupedLeftPadded(16) === Seq(
      BitChunk.fromHexString("0001"),
      BitChunk.fromHexString("2012")
    )
    BitChunk.fromHexString("abcabc").groupedDroppingLeft(16) === Seq(
      BitChunk.fromHexString("ab"),
      BitChunk.fromHexString("cabc")
    )
    BitChunk.fromHexString("012012").groupedDroppingLeft(16) === Seq(
      BitChunk.fromHexString("01"),
      BitChunk.fromHexString("2012")
    )
  }
}
