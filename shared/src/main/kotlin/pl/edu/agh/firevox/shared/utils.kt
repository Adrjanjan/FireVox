package pl.edu.agh.firevox.shared

import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView


fun verifyInbound(k: VoxelKey, modelSize: SimulationSizeView) =
    if (k.x < 0 || k.y < 0 || k.z < 0) false
    else (k.x < modelSize.sizeX && k.y < modelSize.sizeY && k.z < modelSize.sizeZ)