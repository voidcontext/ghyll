package com.gaborpihaj.jsonstream.benchmark

import com.monovore.decline.{Command, Opts}

sealed trait CliCommand

object CliCommand {
  case object GenerateSampleJson extends CliCommand
  case class BenchmarkCirce(rounds: Int) extends CliCommand
  case class BenchmarkJsonStream(rounds: Int) extends CliCommand

  val generateSampleJson =
    Opts.subcommand(
      Command(name = "generate-sample", header = "Generates a big enough JSON file for benchmarks")(
        Opts(GenerateSampleJson)
      )
    )

  val roundOpts =
    Opts
      .option[Int](
        long = "rounds",
        short = "r",
        help = "How many times the parser should run"
      )
      .withDefault(1)

  val benchmarkCirce =
    Opts.subcommand(Command(name = "circe", header = "Runs the benchmarks using circe")(roundOpts.map(BenchmarkCirce)))

  val benchmarkJsonStream =
    Opts.subcommand(
      Command(name = "json-stream", header = "Runs the benchmarks using json-stream")(
        roundOpts.map(BenchmarkJsonStream)
      )
    )
}
