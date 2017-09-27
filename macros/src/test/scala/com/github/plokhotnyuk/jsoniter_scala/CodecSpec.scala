package com.github.plokhotnyuk.jsoniter_scala

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.jsoniter.spi.{Config, JsoniterSpi}
import Codec._
import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable._
import scala.collection.mutable

case class KeyOverridden(@key("new_key") oldKey: String) //FIXME: fail tests when move to end of source file

class CodecSpec extends WordSpec with Matchers {
  "Codec" should {
    "serialize and deserialize primitives" in {
      verifySerDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
    }
    "serialize and deserialize boxed primitives" in {
      verifySerDeser(materialize[BoxedPrimitives],
        BoxedPrimitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
    }
    "serialize and deserialize standard types" in {
      verifySerDeser(materialize[StandardTypes],
        StandardTypes("VVV", BigInt("123456789012345678901234567890"), BigDecimal("1234567890.12345678901234567890")),
        """{"s":"VVV","bi":123456789012345678901234567890,"bd":1234567890.12345678901234567890}""".getBytes)
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(materialize[Enums], Enums(LocationType.GPS), """{"lt":1}""".getBytes)
    }
    "serialize and deserialize value classes" in {
      verifySerDeser(materialize[ValueClassTypes],
        ValueClassTypes(UserId("123abc"), OrderId(123123)),
        """{"uid":"123abc","oid":123123}""".getBytes)
    }
    "serialize and deserialize options" in {
      verifySerDeser(materialize[Options],
        Options(Option("VVV"), Option(BigInt(4)), Option(Set())),
        """{"os":"VVV","obi":4,"osi":[]}""".getBytes)
    }
    "serialize and deserialize mutable iterables" in {
      verifySerDeser(materialize[MutableIterables],
        MutableIterables(mutable.MutableList("1", "2", "3"), mutable.Set(BigInt(4)),
          mutable.ArraySeq(mutable.LinkedHashSet(1, 2), mutable.LinkedHashSet())),
        """{"l":["1","2","3"],"s":[4],"ls":[[1,2],[]]}""".getBytes)
    }
    "serialize and deserialize immutable iterables" in {
      verifySerDeser(materialize[ImmutableIterables],
        ImmutableIterables(List("1", "2", "3"), Set(BigInt(4), BigInt(5), BigInt(6)), IndexedSeq(Set(1, 2), Set())),
        """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes)
    }
    "serialize and deserialize mutable maps" in {
      verifySerDeser(materialize[MutableMaps],
        MutableMaps(mutable.HashMap("1" -> BigInt("11")), mutable.SortedMap(1L -> 1.1f, 2L -> 2.2f),
          mutable.OpenHashMap(1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2 -> mutable.LinkedHashMap())),
        """{"ms":{"1":11},"mi":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes)
    }
    "serialize and deserialize immutable maps" in {
      verifySerDeser(materialize[ImmutableMaps],
        ImmutableMaps(HashMap("1" -> BigInt("11")), SortedMap(1L -> 1.1f, 2L -> 2.2f), Map(1 -> Map(3.toShort -> 3.3), 2 -> Map())),
        """{"ms":{"1":11},"mi":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes)
    }
    "serialize and deserialize mutable long maps" in {
      verifySerDeser(materialize[MutableIntLongMaps],
        MutableIntLongMaps(mutable.LongMap(1L -> 1.1), mutable.LongMap(3L -> "33")),
        """{"im":{"1":1.1},"lm":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize immutable int and long maps" in {
      verifySerDeser(materialize[ImmutableIntLongMaps],
        ImmutableIntLongMaps(IntMap(1 -> 1.1, 2 -> 2.2), LongMap(3L -> "33")),
        """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize mutable & immutable bitsets" in {
      verifySerDeser(materialize[BitSets], BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6)),
        """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes)
    }
    "serialize and deserialize with keys defined by fields" in {
      verifySerDeser(materialize[CamelAndSnakeCase],
        CamelAndSnakeCase("VVV", "XXX"),
        """{"camelCase":"VVV","snake_case":"XXX"}""".getBytes)
    }
    "serialize and deserialize indented JSON" in {
      withConfig(_.indentionStep(2)) {
        verifySerDeser(materialize[Indented], Indented("VVV", List("X", "Y", "Z")),
          """{
            |  "f1": "VVV",
            |  "f2": [
            |    "X",
            |    "Y",
            |    "Z"
            |  ]
            |}""".stripMargin.getBytes)
      }
    }
    "serialize and deserialize UTF-8 keys and values without hex encoding" in {
      verifySerDeser(materialize[UTF8KeysAndValues], UTF8KeysAndValues("ვვვ"), """{"გასაღები":"ვვვ"}""".getBytes("UTF-8"))
    }
    "serialize and deserialize with keys overridden by annotation" in {
      verifySerDeser(materialize[KeyOverridden], KeyOverridden("VVV"), """{"new_key":"VVV"}""".getBytes)
    }
    "deserialize but don't serialize default values that defined for fields" in {
      verifySerDeser(materialize[Defaults], Defaults(), """{}""".getBytes)
    }
    "don't serialize and deserialize transient and non constructor defined fields" in {
      verifySerDeser(materialize[Transient], Transient("VVV"), """{"required":"VVV"}""".getBytes)
    }
    "don't serialize fields null or none values" in {
      verifySer(materialize[NullOrNoneValues],
        NullOrNoneValues("VVV", null, null, NullOrNoneValues(null, null, null, null, None), None),
        """{"str":"VVV","nv":{}}""".getBytes)
    }
    "don't serialize fields empty collections" in {
      verifySer(materialize[EmptyIterables], EmptyIterables(List(), Set(), List()), """{}""".getBytes)
    }
    "don't deserialize unknown fields" in {
      verifyDeser(materialize[Unknown], Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes)
    }
    "throw exception in case of missing required fields detected during deserialization" in {
      assert(intercept[Exception] {
        verifyDeser(materialize[Required], Required("VVV", 0, None), """{"req1":"VVV"}""".getBytes)
      }.getMessage.contains("""decode: missing required field(s) "req2""""))
    }
  }

  def verifySerDeser[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    verifySer(codec, obj, json)
    verifyDeser(codec, obj, json)
  }

  def verifySer[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    val baos = new ByteArrayOutputStream
    codec.write(obj, baos)
    toString(baos.toByteArray) shouldBe toString(json)
    toString(codec.write(obj)) shouldBe toString(json)
  }

  def verifyDeser[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    //FIXME: Failing when launched by 'sbt test'
    //codec.read(new ByteArrayInputStream(json)) shouldBe obj
    codec.read(json) shouldBe obj
  }

  def withConfig(configBuilder: Config.Builder => Config.Builder)(f: => Unit): Unit = {
    val currentConfig = JsoniterSpi.getCurrentConfig
    JsoniterSpi.setCurrentConfig(configBuilder(new Config.Builder).build)
    try f finally JsoniterSpi.setCurrentConfig(currentConfig)
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, "UTF-8")
}

case class UserId(value: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class BoxedPrimitives(b: java.lang.Byte, s: java.lang.Short, i: java.lang.Integer, l: java.lang.Long,
                           bl: java.lang.Boolean, ch: java.lang.Character, dbl: java.lang.Double, f: java.lang.Float)

case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

object LocationType extends Enumeration {
  type LocationType = Value
  val GPS: LocationType = Value(1)
  val IP: LocationType = Value(2)
  val UserProvided: LocationType = Value(3)
}
case class Enums(lt: LocationType.LocationType)

case class ValueClassTypes(uid: UserId, oid: OrderId)

case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]])

case class MutableIterables(l: mutable.MutableList[String], s: mutable.Set[BigInt], ls: mutable.ArraySeq[mutable.LinkedHashSet[Int]])

case class ImmutableIterables(l: List[String], s: Set[BigInt], ls: IndexedSeq[Set[Int]])

case class MutableMaps(ms: mutable.HashMap[String, BigInt], mi: mutable.SortedMap[Long, Float], mm: mutable.OpenHashMap[Int, mutable.LinkedHashMap[Short, Double]])

case class ImmutableMaps(ms: HashMap[String, BigInt], mi: SortedMap[Long, Float], mm: Map[Int, Map[Short, Double]])

case class MutableIntLongMaps(im: mutable.LongMap[Double], lm: mutable.LongMap[String])

case class ImmutableIntLongMaps(im: IntMap[Double], lm: LongMap[String])

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

case class CamelAndSnakeCase(camelCase: String, snake_case: String)

case class Indented(f1: String, f2: List[String])

case class UTF8KeysAndValues(გასაღები: String)

case class Defaults(s: String = "VVV", i: Int = 1, bi: BigInt = BigInt(-1))

case class Transient(required: String, @transient transient: String = "default") {
  val ignored: String = required + "_" + transient
}

case class NullOrNoneValues(str: String, bi: BigInt, bd: BigDecimal, nv: NullOrNoneValues, opt: Option[String])

case class EmptyIterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

case class Unknown()

case class Required(req1: String, req2: Int, opt: Option[String])
