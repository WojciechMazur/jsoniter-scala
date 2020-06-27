package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfYearMonthsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfYearMonthsReading = new ArrayOfYearMonthsReading {
    setup()
  }
  
  "ArrayOfYearMonthsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(1) = '{'.toByte
      b.jsonBytes(2) = '}'.toByte
      b.jsonBytes(3) = ','.toByte
      b.jsonBytes(4) = '"'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
    }
  }
}