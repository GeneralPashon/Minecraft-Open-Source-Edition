package minecraftose.server.gen.structure;

import jpize.math.util.Random;
import minecraftose.client.block.ClientBlocks;
import minecraftose.main.level.structure.Structure;
import minecraftose.server.chunk.ServerChunk;
import minecraftose.server.gen.pool.BlockPool;
import minecraftose.server.level.ServerLevel;

public class Tower{

    public static void generate(BlockPool pool, int x, int y, int z, Random random){
        final int offsetY = -8;
        final int radius = 12;
        final int floors = random.random(6, 12);
        final int floorHeight = 4;
        final int peakY = floors * (floorHeight + 1);

        for(int i = 0; i < floors; i++){
            final int floorBaseY = i * (floorHeight + 1);

            Structure.circleFilledXZ(pool, x, y + offsetY + floorBaseY, z, radius, ClientBlocks.MOSSY_COBBLESTONE);

            for(int j = 0; j < floorHeight; j++)
                Structure.circleXZ(pool, x, y + offsetY + floorBaseY + j + 1, z, radius, ClientBlocks.STONE_BRICKS);
        }

        Structure.circleFilledXZ(pool, x, y + offsetY + peakY, z, radius, ClientBlocks.STONE);

        pool.genBlock(x, y + offsetY + peakY + 1, z, ClientBlocks.GLASS);
        pool.genBlock(x, y + offsetY + peakY + 2, z, ClientBlocks.LAMP);
        // level.getBlockLight().increase(chunk, x, y + offsetY + peakY + 2, z, MAX_LIGHT_LEVEL);
        pool.genBlock(x, y + offsetY + peakY + 3, z, ClientBlocks.WATER);
    }

    private static void pillar(ServerChunk chunk, int x, int y, int z){
        final ServerLevel level = chunk.getLevel();

        for(int i = 0; i < 16; i++){
            level.genBlock(x - 1, y + i, z - 1, ClientBlocks.STONE);
            level.genBlock(x    , y + i, z - 1, ClientBlocks.STONE);
            level.genBlock(x + 1, y + i, z - 1, ClientBlocks.STONE);
            level.genBlock(x - 1, y + i, z    , ClientBlocks.STONE);
            level.genBlock(x    , y + i, z    , ClientBlocks.STONE);
            level.genBlock(x + 1, y + i, z    , ClientBlocks.STONE);
            level.genBlock(x - 1, y + i, z + 1, ClientBlocks.STONE);
            level.genBlock(x    , y + i, z + 1, ClientBlocks.STONE);
            level.genBlock(x + 1, y + i, z + 1, ClientBlocks.STONE);
        }

        level.genBlock(x, y + 16, z, ClientBlocks.GLASS);
        level.genBlock(x, y + 17, z, ClientBlocks.LAMP);
        level.genBlock(x, y + 18, z, ClientBlocks.WATER);
    }

}
