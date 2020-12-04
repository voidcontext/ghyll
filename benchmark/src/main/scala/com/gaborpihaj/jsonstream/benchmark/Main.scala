package com.gaborpihaj.jsonstream.benchmark

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.LocalDate

import scala.io.Source
import scala.math.Ordering.Implicits._

import cats.effect.{ExitCode, IO}
import cats.instances.either._
import cats.syntax.apply._
import com.gaborpihaj.jsonstream.StreamingDecoder
import com.gaborpihaj.jsonstream.StreamingDecoder.StreamingDecoderError
import com.gaborpihaj.jsonstream.benchmark.CliCommand._
import com.gaborpihaj.jsonstream.benchmark.Data.{DataSet, Item, PricePoint}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.circe.parser.decode
import io.circe.syntax._

object Main
    extends CommandIOApp(
      name = "benchmark",
      header = "Benchmarking tools for com.gaborpihaj.jsonstream"
    ) {

  val sampleFile = Paths.get("benchmark/generated.json")
  val mb = 1024 * 1024
  val runtime = Runtime.getRuntime

  def findLatestPrice(prices: List[PricePoint]): BigDecimal =
    prices
      .foldLeft(LocalDate.of(1000, 1, 1) -> BigDecimal.valueOf(0)) { (acc, p) =>
        if (acc._1 <= p.date) p.date -> p.price
        else acc
      }
      ._2

  def totalPrice(dataset: DataSet) =
    dataset.values.foldLeft(BigDecimal.valueOf(0))((sum, item) => sum + findLatestPrice(item.prices))

  def memoryUsage(): IO[Long] =
    IO.delay((runtime.totalMemory() - runtime.freeMemory()) / mb)

  def printMemoryUsage: IO[Unit] =
    memoryUsage().flatMap(m => IO.delay(println(s"Max Memory used: ${m}")))

  def main: Opts[IO[ExitCode]] =
    (generateSampleJson orElse[CliCommand] benchmarkCirce orElse[CliCommand] benchmarkJsonStream).map {
      case GenerateSampleJson =>
        Data
          .generate[IO]
          .map(_.asJson.toString)
          .flatMap(json => IO.delay(Files.write(sampleFile, json.getBytes(StandardCharsets.UTF_8))))
          .as(ExitCode.Success)
      case BenchmarkCirce     =>
        for {
          _            <- IO.delay(println("parse using circe"))
          result       <- IO.delay(decode[DataSet](Source.fromFile(sampleFile.toUri()).mkString))
          errorOrTotal <- IO.delay(result.map(totalPrice))
          _            <- IO.delay(println(errorOrTotal))
          _            <- printMemoryUsage

        } yield ExitCode.Success

      case BenchmarkJsonStream =>
        for {
          _            <- IO.delay(println("parse using json-stream"))
          errorOrTotal <-
            StreamingDecoder[IO]()
              .decode[Item](sampleFile.toFile())
              .use(
                _.map(_.map(kv => findLatestPrice(kv._2.prices))).compile
                  .fold[Either[StreamingDecoderError, BigDecimal]](Right(BigDecimal.valueOf(0)))((sum, t) =>
                    (sum, t).mapN(_ + _)
                  )
              )
          _            <- IO.delay(println(errorOrTotal))
          _            <- printMemoryUsage

        } yield ExitCode.Success
    }

}
