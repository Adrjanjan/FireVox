# FireVox
Fire simulation based on voxel models of buildings

# Verification Tests
Tests are present under `worker/src/test/kotlin/pl/edu/agh/firevox/worker/physics/verification`
Each test is separate unit, everything is set up so that whole architecture required for running verification test
is set up automatically. 
To run tests easier use `kotest` plugin in `IntelliJ`. 
Every material's and engine's parameters are set explicitly in each test case. 
To see the results of the test verify which results are present for given test case. 
For `.csv` files use scripts present in `graphs-generation` or modify accordingly.
For `.vox` files use voxel editor i.e. `MagicaVoxel` or `Goxel`.  


FDS scripts are present in `fds-scripts` tu run them run:
`fds path_to_fds_file.fds`

To view results:
`smokeview path_to_fds_file`
