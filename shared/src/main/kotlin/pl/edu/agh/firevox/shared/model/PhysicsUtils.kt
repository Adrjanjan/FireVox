package pl.edu.agh.firevox.shared.model


fun Double.toKelvin(): Double = this + 273.15
fun Int.toKelvin(): Double = this + 273.15
fun Double.toCelsius() = this - 273.15