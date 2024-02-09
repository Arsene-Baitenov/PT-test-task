## About The Project
It is the 4th variant of PT test task.

There is `class Predictor` with `predict` method that predict output of the given `Java file` as a list of integers.

## Usage
Initialize an object of the `class Predictor`. 
```kotlin
val p = Predictor()
```
Call the `predict` method with the passed path to the `Java file` or a line containing code.
```kotlin
val f = File(some_path)
println(p.predict(f.toPath())) // print list of results
```