package me.ethanbell.bitchunk

object Util {

  implicit class ByteSeqOps(val bs: Seq[Byte]) {
    def trimLeft(): Seq[Byte] =
      bs.dropWhile(_ == 0x00.toByte)

    def trimRight(): Seq[Byte] =
      bs.reverse.dropWhile(_ == 0x00.toByte).reverse

    def asHexBytes: String =
      bs.map(byte => "%02x".format(byte)).mkString("0x", "", "")
  }

  def unsignedAsLong(l: Long, bits: Int) =
    if (l < 0) {
      // println(s"representing $bits-bit $l as $a")
      l + Math.pow(2, bits.toDouble).toLong
    } else l
}
