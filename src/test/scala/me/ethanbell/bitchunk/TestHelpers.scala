package me.ethanbell.bitchunk

import java.util.NoSuchElementException

import org.scalacheck.Gen

trait TestHelpers {
  implicit final class sampleOps[+T](gen: Gen[T]) {

    /**
     * Materialize a value from a generator up to a number of tries, or else throw an exception
     * @param tries The number of attempts to make to pull from the generator
     * @throws NoSuchElementException if the generator cannot pull a value within `tries`attempts
     * @return a generated values
     */
    @throws[NoSuchElementException]
    @scala.annotation.tailrec
    def getOrThrow(tries: Int = 100): T =
      if (tries <= 0) throw new NoSuchElementException(s"No value could be generated from $gen")
      else
        gen.sample match {
          case Some(t) => t
          case None    => getOrThrow(tries - 1)
        }
  }
}
