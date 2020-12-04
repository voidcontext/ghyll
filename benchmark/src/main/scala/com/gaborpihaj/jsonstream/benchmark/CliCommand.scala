package com.gaborpihaj.jsonstream.benchmark

import com.monovore.decline.{Command, Opts}

sealed trait CliCommand

object CliCommand {
  case object GenerateSampleJson extends CliCommand
  case object BenchmarkCirce extends CliCommand
  case object BenchmarkJsonStream extends CliCommand

  val generateSampleJson = Opts.subcommand(
    Command(name = "generate-sample", header = "Generates a big enough JSON file for benchmarks")(
      Opts(GenerateSampleJson)
    )
  )

  val benchmarkCirce =
    Opts.subcommand(Command(name = "circe", header = "Runs the benchmarks using circe")(Opts(BenchmarkCirce)))
  val benchmarkJsonStream = Opts.subcommand(
    Command(name = "json-stream", header = "Runs the benchmarks using json-stream")(Opts(BenchmarkJsonStream))
  )
}
