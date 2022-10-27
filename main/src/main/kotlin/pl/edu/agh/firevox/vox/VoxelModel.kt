package pl.edu.agh.firevox.vox

import java.io.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class VoxModel constructor(var sizeX: Int, var sizeY: Int, var sizeZ: Int, val maxSize: Int) {

    companion object {
        fun construct(materialChunk: MainChunk, maxSize: Int): VoxModel {
            TODO("Not implemented yet")
            // iterate over chu
            //
            //

        }
    }

    var palette = Palette()
    val voxels = mutableListOf<Voxel>()

    fun setColor(index: Int, color: Int) {
        palette.setColor(index, color)
    }

    fun getMaterial(index: Int): Material? {
        return palette.getMaterial(index)
    }

    fun addVoxel(x: Int, y: Int, z: Int, i: Int) {
        if (x in 0 until maxSize && (y in 0 until maxSize) && (z in 0 until maxSize)) {
            voxels.add(Voxel(x, y, z, i))
        }
        palette.setUsed(i)
    }

    fun add(model: VoxModel, x: Int, y: Int, z: Int) = add(
            model = model,
            x = x,
            y = y,
            z = z,
            centerX = false,
            centerY = false,
            centerZ = false,
            flipX = false,
            flipY = false,
            flipZ = false,
            rotateX = 0,
            rotateY = 0,
            rotateZ = 0
        )

    fun scale(f: Int): VoxModel {
        if (f == 1) {
            return this;
        } else {
            val oldVoxels = voxels
            val newVoxels = mutableListOf<Voxel>()
            for ((x1, y1, z1, i) in oldVoxels) {
                val x = x1 * f
                val y = y1 * f
                val z = z1 * f
                for (dx in 0 until f) {
                    for (dy in 0 until f) {
                        for (dz in 0 until f) {
                            addVoxel(x + dx, y + dy, z + dz, i)
                        }
                    }
                }
            }
            sizeX = (sizeX * f).coerceAtMost(maxSize)
            sizeY = (sizeY * f).coerceAtMost(maxSize)
            sizeZ = (sizeZ * f).coerceAtMost(maxSize)
            return this;
        }
    }

    fun add(
        model: VoxModel,
        x: Int,
        y: Int,
        z: Int,
        centerX: Boolean,
        centerY: Boolean,
        centerZ: Boolean,
        flipX: Boolean,
        flipY: Boolean,
        flipZ: Boolean,
        rotateX: Int,
        rotateY: Int,
        rotateZ: Int
    ) {
        palette.merge(model)
        val rot = Array(3) { FloatArray(3) }
        rot[0][0] = (cos(rotateY) * cos(rotateZ)).toFloat()
        rot[0][1] = (cos(rotateZ) * sin(rotateX) * sin(rotateY) - cos(rotateX) * sin(rotateZ)).toFloat()
        rot[0][2] = (cos(rotateX) * cos(rotateZ) * sin(rotateY) + sin(rotateX) * sin(rotateZ)).toFloat()
        rot[1][0] = (cos(rotateY) * sin(rotateZ)).toFloat()
        rot[1][1] = (cos(rotateX) * cos(rotateZ) + sin(rotateX) * sin(rotateY) * sin(rotateZ)).toFloat()
        rot[1][2] = (cos(rotateX) * sin(rotateY) * sin(rotateZ) - cos(rotateZ) * sin(rotateX)).toFloat()
        rot[2][0] = -sin(rotateY).toFloat()
        rot[2][1] = (cos(rotateY) * sin(rotateX)).toFloat()
        rot[2][2] = (cos(rotateX) * cos(rotateY)).toFloat()
        for ((x1, y1, z1, i) in model.voxels) {
            val vx = if (flipX) model.sizeX - x1 - 1 else x1
            val vy = if (flipY) model.sizeY - y1 - 1 else y1
            val vz = if (flipZ) model.sizeZ - z1 - 1 else z1
            val fx = vx - model.sizeX / 2f
            val fy = vy - model.sizeY / 2f
            val fz = vz - model.sizeZ / 2f
            val rx: Int =
                (rot[0][0] * fx + rot[0][1] * fy + rot[0][2] * fz + (if (centerX) 1 else model.sizeX / 2f) + x).toInt()
            val ry: Int =
                (rot[1][0] * fx + rot[1][1] * fy + rot[1][2] * fz + (if (centerY) 1 else model.sizeY / 2f) + y).toInt()
            val rz: Int =
                (rot[2][0] * fx + rot[2][1] * fy + rot[2][2] * fz + (if (centerZ) 1 else model.sizeZ / 2f) + z).toInt()
            if (rx in 0..maxSize && (ry in 0..maxSize) && (rz in 0..maxSize)) {
                addVoxel(rx, ry, rz, i)
            }
        }
    }

    fun clipToVoxels() {
        for ((x, y, z) in voxels) {
            sizeX = max(sizeX, x + 1)
            sizeY = max(sizeY, y + 1)
            sizeZ = max(sizeZ, z + 1)
        }
    }

    @Throws(IOException::class)
    fun splitIntoTiles(dirName: String, tileSize: Int) {
        val nx = ceil((sizeX / tileSize.toFloat()).toDouble()).toInt()
        val ny = ceil((sizeY / tileSize.toFloat()).toDouble()).toInt()
        val nz = ceil((sizeZ / tileSize.toFloat()).toDouble()).toInt()
        val models = Array(nx) {
            Array(ny) {
                arrayOfNulls<VoxModel>(nz)
            }
        }
        val results = ArrayList<VoxModel>()
        for (ix in 0 until nx) {
            val offsetX = ix * tileSize
            val subSizeX = min(offsetX + tileSize, sizeX)
            for (iy in 0 until ny) {
                val offsetY = iy * tileSize
                val subSizeY = Math.min(offsetY + tileSize, sizeY)
                for (iz in 0 until nz) {
                    val offsetZ = iz * tileSize
                    val subSizeZ = Math.min(offsetZ + tileSize, sizeZ)
                    val model = VoxModel(subSizeX, subSizeY, subSizeZ, maxSize)
                    model.palette = palette
                    models[ix][iy][iz] = model
                    results.add(model)
                }
            }
        }
        for ((x1, y1, z1, i) in voxels) {
            val ix = x1 / tileSize
            val iy = y1 / tileSize
            val iz = z1 / tileSize
            val x = x1 - ix * tileSize
            val y = y1 - iy * tileSize
            val z = z1 - iz * tileSize
            models[ix][iy][iz]!!.addVoxel(x, y, z, i)
        }
        val dir = File(dirName)
        dir.mkdirs()
        val textOut = PrintWriter("$dirName.txt")
        textOut.println("// Generated by FireVoxProperties (https://github.com/larvalabs/FireVoxProperties)")
        textOut.println("mv_import 2048")
        for (ix in 0 until nx) {
            for (iy in 0 until ny) {
                for (iz in 0 until nz) {
                    val model = models[ix][iy][iz]
                    val name = dirName + "_" + ix + "_" + iy + "_" + iz + ".vox"
                    val voxFile = File(dir, name)
                    val outputStream = FileOutputStream(voxFile)
                    VoxFormatParser.write(model!!, outputStream)
                    outputStream.close()
                    val offsetX = ix * tileSize
                    val offsetY = iy * tileSize
                    val offsetZ = iz * tileSize
                    textOut.println(offsetX.toString() + "\t" + offsetY + "\t" + offsetZ + "\t" + voxFile.absolutePath)
                }
            }
        }
        textOut.close()
    }
}

fun sin(angle: Int) = when (angle % 360) {
    90 -> 1
    180 -> 0
    270 -> -1
    else -> 0
}


fun cos(angle: Int) = when (angle % 360) {
    90, 270 -> 0
    180 -> -1
    else -> 1
}

