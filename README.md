# SPOTON - Type-Based Targeted Mutation for Generator-Based fuzzer

SpotOn is a Java fuzzer that uses the program types to generate mutants for fuzzing. SpotOn is built on top of  [Zest](https://github.com/rohanpadhye/JQF) a generator-based fuzzer that uses programmable generators to construct inputs. In general, generator-based fuzzers like Zest do not have direct control over the program input, instead, they control the input to the generators which in turn controls the program input.
