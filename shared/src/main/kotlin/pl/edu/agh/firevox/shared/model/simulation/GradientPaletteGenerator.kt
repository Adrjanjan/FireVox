package pl.edu.agh.firevox.shared.model.simulation

class GradientPaletteGenerator {

    companion object {
        fun generateGradientColors(
            startColor: Colour,
            middleColor: Colour,
            endColor: Colour,
            numColors: Int
        ): List<Colour> {
            val colorList = mutableListOf<Colour>()

            for (i in 0 until numColors) {
                val ratio = i.toDouble() / (numColors - 1)
                val red = interpolateColorComponent(
                    startColor.r,
                    middleColor.r,
                    endColor.r,
                    ratio
                )
                val green = interpolateColorComponent(
                    startColor.g,
                    middleColor.g,
                    endColor.g,
                    ratio
                )
                val blue = interpolateColorComponent(
                    startColor.b,
                    middleColor.b,
                    endColor.b,
                    ratio
                )

                colorList.add(Colour(i, red, green, blue, 1))
            }

            return colorList
        }

        private fun interpolateColorComponent(start: Int, middle: Int, end: Int, ratio: Double) =
            ((1 - ratio) * (1 - ratio) * start + 2 * (1 - ratio) * ratio * middle + ratio * ratio * end).toInt()
    }
}