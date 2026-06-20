package com.craftzero.world;

import com.craftzero.entity.Entity;
import com.craftzero.world.tile.TileEntity;

/**
 * World-thread staging surface for structure output that is not block data.
 */
public interface GeneratedStructureSink {
    void stageGeneratedTileEntity(TileEntity tileEntity);

    void stageGeneratedEntity(Entity entity);
}
