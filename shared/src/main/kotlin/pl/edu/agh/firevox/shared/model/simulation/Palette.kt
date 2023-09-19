package pl.edu.agh.firevox.shared.model.simulation

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*
import kotlin.jvm.Transient

@Entity
@Table(name = "palette")
data class Palette(
    @Id
    val paletteName: String,

    @ElementCollection
    val colours: Set<Colour>
) {

    companion object {
        @Transient
        val temperaturePalette = Palette(
            temperaturePaletteName, setOf(
                Colour(1, 0, 0, 255),
                Colour(2, 0, 2, 252),
                Colour(3, 0, 3, 250),
                Colour(4, 0, 5, 248),
                Colour(5, 0, 7, 247),
                Colour(6, 0, 9, 245),
                Colour(7, 0, 11, 243),
                Colour(8, 0, 13, 241),
                Colour(9, 0, 15, 239),
                Colour(10, 0, 17, 237),
                Colour(11, 0, 19, 235),
                Colour(12, 0, 21, 233),
                Colour(13, 0, 23, 231),
                Colour(14, 0, 24, 229),
                Colour(15, 0, 26, 227),
                Colour(16, 0, 28, 225),
                Colour(17, 1, 30, 223),
                Colour(18, 1, 31, 221),
                Colour(19, 1, 33, 220),
                Colour(20, 1, 35, 218),
                Colour(21, 1, 37, 216),
                Colour(22, 1, 38, 214),
                Colour(23, 1, 40, 212),
                Colour(24, 2, 42, 210),
                Colour(25, 2, 43, 208),
                Colour(26, 2, 45, 207),
                Colour(27, 2, 47, 205),
                Colour(28, 2, 48, 203),
                Colour(29, 3, 50, 201),
                Colour(30, 3, 51, 199),
                Colour(31, 3, 53, 198),
                Colour(32, 3, 54, 196),
                Colour(33, 4, 56, 194),
                Colour(34, 4, 57, 192),
                Colour(35, 4, 59, 191),
                Colour(36, 4, 60, 189),
                Colour(37, 5, 62, 187),
                Colour(38, 5, 63, 185),
                Colour(39, 5, 65, 184),
                Colour(40, 6, 66, 182),
                Colour(41, 6, 67, 180),
                Colour(42, 6, 69, 179),
                Colour(43, 7, 70, 177),
                Colour(44, 7, 71, 175),
                Colour(45, 7, 73, 174),
                Colour(46, 8, 74, 172),
                Colour(47, 8, 75, 170),
                Colour(48, 8, 77, 169),
                Colour(49, 9, 78, 167),
                Colour(50, 9, 79, 165),
                Colour(51, 9, 80, 164),
                Colour(52, 10, 82, 162),
                Colour(53, 10, 83, 160),
                Colour(54, 11, 84, 159),
                Colour(55, 11, 85, 157),
                Colour(56, 12, 86, 156),
                Colour(57, 12, 87, 154),
                Colour(58, 12, 89, 153),
                Colour(59, 13, 90, 151),
                Colour(60, 13, 91, 149),
                Colour(61, 14, 92, 148),
                Colour(62, 14, 93, 146),
                Colour(63, 15, 94, 145),
                Colour(64, 15, 95, 143),
                Colour(65, 16, 96, 142),
                Colour(66, 16, 97, 140),
                Colour(67, 17, 98, 139),
                Colour(68, 17, 99, 137),
                Colour(69, 18, 100, 136),
                Colour(70, 18, 101, 134),
                Colour(71, 19, 102, 133),
                Colour(72, 20, 102, 131),
                Colour(73, 20, 103, 130),
                Colour(74, 21, 104, 129),
                Colour(75, 21, 105, 127),
                Colour(76, 22, 106, 126),
                Colour(77, 23, 107, 124),
                Colour(78, 23, 107, 123),
                Colour(79, 24, 108, 122),
                Colour(80, 24, 109, 120),
                Colour(81, 25, 110, 119),
                Colour(82, 26, 111, 117),
                Colour(83, 26, 111, 116),
                Colour(84, 27, 112, 115),
                Colour(85, 28, 113, 113),
                Colour(86, 28, 113, 112),
                Colour(87, 29, 114, 111),
                Colour(88, 30, 115, 109),
                Colour(89, 30, 115, 108),
                Colour(90, 31, 116, 107),
                Colour(91, 32, 116, 105),
                Colour(92, 32, 117, 104),
                Colour(93, 33, 118, 103),
                Colour(94, 34, 118, 101),
                Colour(95, 35, 119, 100),
                Colour(96, 35, 119, 99),
                Colour(97, 36, 120, 98),
                Colour(98, 37, 120, 96),
                Colour(99, 38, 121, 95),
                Colour(100, 39, 121, 94),
                Colour(101, 39, 121, 93),
                Colour(102, 40, 122, 92),
                Colour(103, 41, 122, 90),
                Colour(104, 42, 123, 89),
                Colour(105, 43, 123, 88),
                Colour(106, 43, 123, 87),
                Colour(107, 44, 124, 86),
                Colour(108, 45, 124, 84),
                Colour(109, 46, 124, 83),
                Colour(110, 47, 125, 82),
                Colour(111, 48, 125, 81),
                Colour(112, 49, 125, 80),
                Colour(113, 49, 125, 79),
                Colour(114, 50, 126, 78),
                Colour(115, 51, 126, 76),
                Colour(116, 52, 126, 75),
                Colour(117, 53, 126, 74),
                Colour(118, 54, 126, 73),
                Colour(119, 55, 126, 72),
                Colour(120, 56, 127, 71),
                Colour(121, 57, 127, 70),
                Colour(122, 58, 127, 69),
                Colour(123, 59, 127, 68),
                Colour(124, 60, 127, 67),
                Colour(125, 61, 127, 66),
                Colour(126, 62, 127, 65),
                Colour(127, 63, 127, 64),
                Colour(128, 64, 127, 63),
                Colour(129, 65, 127, 62),
                Colour(130, 66, 127, 61),
                Colour(131, 67, 127, 60),
                Colour(132, 68, 127, 59),
                Colour(133, 69, 127, 58),
                Colour(134, 70, 127, 57),
                Colour(135, 71, 127, 56),
                Colour(136, 72, 126, 55),
                Colour(137, 73, 126, 54),
                Colour(138, 74, 126, 53),
                Colour(139, 75, 126, 52),
                Colour(140, 76, 126, 51),
                Colour(141, 78, 126, 50),
                Colour(142, 79, 125, 49),
                Colour(143, 80, 125, 49),
                Colour(144, 81, 125, 48),
                Colour(145, 82, 125, 47),
                Colour(146, 83, 124, 46),
                Colour(147, 84, 124, 45),
                Colour(148, 86, 124, 44),
                Colour(149, 87, 123, 43),
                Colour(150, 88, 123, 43),
                Colour(151, 89, 123, 42),
                Colour(152, 90, 122, 41),
                Colour(153, 92, 122, 40),
                Colour(154, 93, 121, 39),
                Colour(155, 94, 121, 39),
                Colour(156, 95, 121, 38),
                Colour(157, 96, 120, 37),
                Colour(158, 98, 120, 36),
                Colour(159, 99, 119, 35),
                Colour(160, 100, 119, 35),
                Colour(161, 101, 118, 34),
                Colour(162, 103, 118, 33),
                Colour(163, 104, 117, 32),
                Colour(164, 105, 116, 32),
                Colour(165, 107, 116, 31),
                Colour(166, 108, 115, 30),
                Colour(167, 109, 115, 30),
                Colour(168, 111, 114, 29),
                Colour(169, 112, 113, 28),
                Colour(170, 113, 113, 28),
                Colour(171, 115, 112, 27),
                Colour(172, 116, 111, 26),
                Colour(173, 117, 111, 26),
                Colour(174, 119, 110, 25),
                Colour(175, 120, 109, 24),
                Colour(176, 122, 108, 24),
                Colour(177, 123, 107, 23),
                Colour(178, 124, 107, 23),
                Colour(179, 126, 106, 22),
                Colour(180, 127, 105, 21),
                Colour(181, 129, 104, 21),
                Colour(182, 130, 103, 20),
                Colour(183, 131, 102, 20),
                Colour(184, 133, 102, 19),
                Colour(185, 134, 101, 18),
                Colour(186, 136, 100, 18),
                Colour(187, 137, 99, 17),
                Colour(188, 139, 98, 17),
                Colour(189, 140, 97, 16),
                Colour(190, 142, 96, 16),
                Colour(191, 143, 95, 15),
                Colour(192, 145, 94, 15),
                Colour(193, 146, 93, 14),
                Colour(194, 148, 92, 14),
                Colour(195, 149, 91, 13),
                Colour(196, 151, 90, 13),
                Colour(197, 153, 89, 12),
                Colour(198, 154, 87, 12),
                Colour(199, 156, 86, 12),
                Colour(200, 157, 85, 11),
                Colour(201, 159, 84, 11),
                Colour(202, 160, 83, 10),
                Colour(203, 162, 82, 10),
                Colour(204, 164, 80, 9),
                Colour(205, 165, 79, 9),
                Colour(206, 167, 78, 9),
                Colour(207, 169, 77, 8),
                Colour(208, 170, 75, 8),
                Colour(209, 172, 74, 8),
                Colour(210, 174, 73, 7),
                Colour(211, 175, 71, 7),
                Colour(212, 177, 70, 7),
                Colour(213, 179, 69, 6),
                Colour(214, 180, 67, 6),
                Colour(215, 182, 66, 6),
                Colour(216, 184, 65, 5),
                Colour(217, 185, 63, 5),
                Colour(218, 187, 62, 5),
                Colour(219, 189, 60, 4),
                Colour(220, 191, 59, 4),
                Colour(221, 192, 57, 4),
                Colour(222, 194, 56, 4),
                Colour(223, 196, 54, 3),
                Colour(224, 198, 53, 3),
                Colour(225, 199, 51, 3),
                Colour(226, 201, 50, 3),
                Colour(227, 203, 48, 2),
                Colour(228, 205, 47, 2),
                Colour(229, 207, 45, 2),
                Colour(230, 208, 43, 2),
                Colour(231, 210, 42, 2),
                Colour(232, 212, 40, 1),
                Colour(233, 214, 38, 1),
                Colour(234, 216, 37, 1),
                Colour(235, 218, 35, 1),
                Colour(236, 220, 33, 1),
                Colour(237, 221, 31, 1),
                Colour(238, 223, 30, 1),
                Colour(239, 225, 28, 0),
                Colour(240, 227, 26, 0),
                Colour(241, 229, 24, 0),
                Colour(242, 231, 23, 0),
                Colour(243, 233, 21, 0),
                Colour(244, 235, 19, 0),
                Colour(245, 237, 17, 0),
                Colour(246, 239, 15, 0),
                Colour(247, 241, 13, 0),
                Colour(248, 243, 11, 0),
                Colour(249, 245, 9, 0),
                Colour(250, 247, 7, 0),
                Colour(251, 248, 5, 0),
                Colour(252, 250, 3, 0),
                Colour(253, 252, 2, 0),
                Colour(254, 253, 1, 0),
                Colour(255, 255, 0, 0),
            )
        )
        @Transient
        val basePalette = Palette (
            baseName,
            mutableSetOf(
//                Colour(1, 0, 0, 0, 0), //AIR
                Colour(1, 102, 102, 102), // SMOKE
                // Bronze
                Colour(2, 153, 34, 0), // WOOD
                Colour(3, 204, 68, 0), // WOOD_HEATED
                Colour(4, 255, 102, 0), // WOOD_BURNING
                Colour(5, 102, 0, 0), // WOOD_BURNT
                // Green
                Colour(6, 0, 119, 0), // PLASTIC
                Colour(7, 0, 187, 0), // PLASTIC_HEATED
                Colour(8, 0, 255, 0), // PLASTIC_BURNING
                Colour(9, 0, 51, 0), // PLASTIC_BURNT
                // Pink
                Colour(10, 153, 0, 153), // TEXTILE
                Colour(11, 204, 0, 204), // TEXTILE_HEATED //179?
                Colour(12, 255, 0, 255), // TEXTILE_BURNING
                Colour(13, 102, 0, 102), // TEXTILE_BURNT
                // Blue
                Colour(14, 0, 0, 102), // METAL
                Colour(15, 0, 0, 153), // METAL_HEATED
                Colour(16, 0, 0, 204), // METAL_HOT
                Colour(17, 0, 0, 255), // METAL_VERY_HOT
                // Yellow
                Colour(18, 102, 102, 0), // GLASS
                Colour(19, 153, 153, 0), // GLASS_HEATED
                Colour(20, 204, 204, 0), // GLASS_HOT
                Colour(21, 255, 255, 0), // GLASS_VERY_HOT
                // Gray
                Colour(22, 34, 34, 34), // CONCRETE
                // Red
                Colour(23, 255, 0, 0), // FLAME
                // White
                Colour(24, 255, 255, 255), // WATER
            ).also { set ->
                for (i in set.maxOf { it.index }..256)
                set.add(Colour(i, 0, 0, 0, 0))
            }
        )
    }

}

const val temperaturePaletteName = "Temperature Palette"
const val baseName = "Base Palette"

@Repository
interface PaletteRepository : JpaRepository<Palette, UUID> {

    fun findByPaletteName(name: String): Palette?
}

@Embeddable
data class Colour(
    val index: Int,
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255
)
