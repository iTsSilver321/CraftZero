package com.craftzero.world.tile;

import com.craftzero.world.World;

public class NoteBlockTileEntity extends TileEntity {
    private int pitch;
    private int playTicks;

    public NoteBlockTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "note_block";
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = Math.floorMod(pitch, 25);
        markDirty();
    }

    public int getPlayTicks() {
        return playTicks;
    }

    public void cyclePitch() {
        setPitch(pitch + 1);
    }

    public void play(World world) {
        playTicks++;
        markDirty();
    }
}
