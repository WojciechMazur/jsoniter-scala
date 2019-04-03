package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class GeoJSONBenchmark extends CommonParams {
  var obj: GeoJSON = readFromArray[GeoJSON](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): GeoJSON = JsonStringInput.read[GeoJSON](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): GeoJSON = decode[GeoJSON](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readJacksonScala(): GeoJSON = jacksonMapper.readValue[GeoJSON](jsonBytes)

  @Benchmark
  def readJsoniterScala(): GeoJSON = readFromArray[GeoJSON](jsonBytes)

  @Benchmark
  def readPlayJson(): GeoJSON = Json.parse(jsonBytes).as[GeoJSON](geoJSONFormat)

  @Benchmark
  def readSprayJson(): GeoJSON = JsonParser(jsonBytes).convertTo[GeoJSON](geoJSONJsonFormat)

  @Benchmark
  def readUPickle(): GeoJSON = read[GeoJSON](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(geoJSONFormat))

  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson(geoJSONJsonFormat).compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}