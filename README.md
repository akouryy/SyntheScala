# SyntheScala

Experimental high-level synthesizer from Scala 3

## LICENSE

Some part of the source codes in this project was taken from [https://github.com/cpu2019-5/anscaml](anscaml).

## Internals

Scala → `AST` → `CDFG` → `ScheduledCDFG` → `ScheduledCDFG` with `Bindings` → `FSMD` → SystemVerilog
