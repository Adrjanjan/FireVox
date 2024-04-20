drop table even_radiation_averages cascade;
drop table odd_radiation_averages cascade;

CREATE MATERIALIZED VIEW IF NOT EXISTS even_radiation_averages AS
SELECT
    p.plane_id,
    sqrt(sqrt(sum(pow(v.even_iteration_temperature, 4))/count(v))) as average
from voxels v join plane_voxels p on v.x = p.voxel_key_x
    and v.x = p.voxel_key_x
    and v.x = p.voxel_key_x
group by p.plane_id;

CREATE MATERIALIZED VIEW IF NOT EXISTS odd_radiation_averages AS
SELECT
    p.plane_id,
    sqrt(sqrt(sum(pow(v.odd_iteration_temperature, 4))/count(v))) as average
from voxels v join plane_voxels p on v.x = p.voxel_key_x
    and v.x = p.voxel_key_x
    and v.x = p.voxel_key_x
group by p.plane_id;