package minecraftose.server.gen;

import jpize.math.Mathc;
import jpize.math.Maths;
import jpize.math.function.FastNoiseLite;
import jpize.math.util.Random;
import minecraftose.client.block.BlockClient;
import minecraftose.client.block.ClientBlocks;
import minecraftose.main.biome.Biome;
import minecraftose.main.biome.BiomeMap;
import minecraftose.main.chunk.storage.Heightmap;
import minecraftose.main.chunk.storage.HeightmapType;
import minecraftose.server.chunk.ServerChunk;
import minecraftose.server.gen.pool.BlockPool;
import minecraftose.server.gen.structure.*;

import java.util.Objects;

import static minecraftose.main.chunk.ChunkUtils.SIZE;

public class DefaultGenerator extends ChunkGenerator{
    
    private static final int SEA_LEVEL = 64;
    private static final float RIVER_MIN = 0.485F;
    private static final float RIVER_MAX = 0.505F;

    private final FastNoiseLite
        continentalnessNoise, erosionNoise, peaksValleysNoise, temperatureNoise, humidityNoise, riverNoise;

    private final Random random;
    

    private DefaultGenerator(){
        this.continentalnessNoise = new FastNoiseLite();
        this.erosionNoise = new FastNoiseLite();
        this.peaksValleysNoise = new FastNoiseLite();
        this.temperatureNoise = new FastNoiseLite();
        this.humidityNoise = new FastNoiseLite();
        this.riverNoise = new FastNoiseLite();

        this.continentalnessNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.continentalnessNoise.setFrequency(0.001F); // FIX
        this.continentalnessNoise.setFractalOctaves(7);

        this.humidityNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.humidityNoise.setFrequency(0.0004F); // FIX
        this.humidityNoise.setFractalOctaves(7);

        this.erosionNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.erosionNoise.setFrequency(0.004F); // FIX
        this.erosionNoise.setFractalOctaves(7);

        this.peaksValleysNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.peaksValleysNoise.setFrequency(0.02F);
        this.peaksValleysNoise.setFractalOctaves(7);

        this.temperatureNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.temperatureNoise.setFrequency(0.0008F); // FIX
        this.temperatureNoise.setFractalOctaves(7);

        this.riverNoise.setFractalType(FastNoiseLite.FractalType.FBm);
        this.riverNoise.setFrequency(0.0009F); // FIX
        this.riverNoise.setFractalOctaves(7);

        this.random = new Random();
    }


    @Override
    public void generate(ServerChunk chunk){
        final int seed = chunk.getLevel().getConfiguration().getSeed();
        final int baseX = chunk.getPosition().x * SIZE;
        final int baseZ = chunk.getPosition().z * SIZE;

        continentalnessNoise.setSeed(seed);
        erosionNoise.setSeed(seed);
        peaksValleysNoise.setSeed(seed);
        temperatureNoise.setSeed(seed);
        humidityNoise.setSeed(seed);
        riverNoise.setSeed(seed);
        random.setSeed(Objects.hash(seed, baseX, baseZ));

        final Heightmap heightmapUnderwaterSurface = chunk.getHeightMap(HeightmapType.UNDERWATER_SURFACE);
        final Heightmap heightmapSurface = chunk.getHeightMap(HeightmapType.SURFACE);
        final BiomeMap biomes = chunk.getBiomes();

        /** GENERATE SURFACE */

        for(int lx = 0; lx < SIZE; lx++){
            final int x = lx + baseX;
            for(int lz = 0; lz < SIZE; lz++){
                // Calculate base height
                final int z = lz + baseZ;
                final float continentalness = Mathc.pow((continentalnessNoise.getNoise(x, z) - 0.15), 3);
                float height = SEA_LEVEL + continentalness * 32;

                // Detail base height
                float detail = 0;
                for(int y = 0; y < 24; y++)
                    detail += continentalnessNoise.getNoise(x, height + y, z) / 32;

                height += detail * 32;

                // Detail base height 2
                detail = 0;
                for(int y = 0; y < 24; y++)
                    detail += erosionNoise.getNoise(x, height + y, z) / 32;

                height += detail * 64;

                // Search rivers around and smooth
                if(height > SEA_LEVEL){
                    float river = riverNoise.getNoise(x - 64, z - 64) / 2 + 0.5F;
                    if(river > RIVER_MIN && river < RIVER_MAX){

                        final float range = RIVER_MAX - RIVER_MIN;
                        final float cp = (RIVER_MAX + RIVER_MIN) / 2;
                        final float t = Math.abs(river - cp) / range * 2;
                        height = Maths.lerp(SEA_LEVEL - 2, SEA_LEVEL, t);

                    }else{

                        final int transitionRadius = 7;

                        float minRiverDistance = 100;
                        for(int i = lx - transitionRadius; i <= lx + transitionRadius; i++){
                            for(int j = lz - transitionRadius; j <= lz + transitionRadius; j++){

                                river = riverNoise.getNoise(i + baseX - 64, j + baseZ - 64) / 2 + 0.5F;
                                if(river > RIVER_MIN && river < RIVER_MAX){

                                    final float distance = Mathc.sqrt((i - lx) * (i - lx) + (j - lz) * (j - lz));
                                    minRiverDistance = Math.min(minRiverDistance, distance);
                                }
                            }
                        }
                        if(minRiverDistance != 100){
                            final float min = Math.min(SEA_LEVEL, height);
                            final float max = Math.max(SEA_LEVEL, height);
                            final float t = Maths.quintic(Math.min(1, minRiverDistance / transitionRadius));
                            height = Maths.lerp(min, max, t);
                        }
                    }
                }

                // Fill stone blocks
                for(int y = 0; y <= height; y++)
                    chunk.setBlockFast(lx, y, lz, ClientBlocks.STONE);
            }
        }

        heightmapSurface.update();
        heightmapUnderwaterSurface.updateFrom(heightmapSurface);

        /** BIOMES */

        for(int lx = 0; lx < SIZE; lx++){
            final int x = lx + baseX;
            for(int lz = 0; lz < SIZE; lz++){
                final int z = lz + baseZ;

                final float temperature = temperatureNoise.getNoise(x, z) / 2 + 0.5F;
                final float height = heightmapSurface.getHeight(lx, lz);
                final float humidity = humidityNoise.getNoise(x, z) / 2 + 0.5F;
                final float river = riverNoise.getNoise(x - 64, z - 64) / 2 + 0.5F;

                final Biome biome;
                if(river > RIVER_MIN && river < RIVER_MAX){ // RIVER
                    if(temperature < 0.3)
                        biome = Biome.ICE_RIVER;
                    else
                        biome = Biome.RIVER;

                }else if(height < SEA_LEVEL){ // OCEAN
                    if(temperature < 0.3)
                        biome = Biome.ICE_SEA;
                    else
                        biome = Biome.SEA;

                }else if(height <= SEA_LEVEL + 7 * peaksValleysNoise.getNoise(x, z)){ // BEACH
                    if(temperature < 0.3)
                        biome = Biome.SNOWY_BEACH;
                    else
                        biome = Biome.BEACH;

                }else{ // SURFACE
                    if(temperature < 0.4){
                        if(humidity < 0.8)
                            biome = Biome.FOREST;
                        else if(humidity < 0.5)
                            biome = Biome.TAIGA;
                        else// if(humidity < 1)
                            biome = Biome.SNOWY_TAIGA;

                    }else if(temperature < 0.7){
                        if(humidity < 0.1)
                            biome = Biome.SAVANNA;
                        else if(humidity < 0.65)
                            biome = Biome.FOREST;
                        else// if(humidity < 1)
                            biome = Biome.TAIGA;

                    }else{// if(temperature < 1)
                        if(humidity < 0.7)
                            biome = Biome.DESERT;
                        else if(humidity < 0.4)
                            biome = Biome.WINDSWEPT_HILLS;
                        else// if(humidity < 1)
                            biome = Biome.FOREST;
                    }
                }

                biomes.setBiome(lx, lz, biome);
            }
        }

        /** SURFACE */

        for(int lx = 0; lx < SIZE; lx++){
            final int x = lx + baseX;
            for(int lz = 0; lz < SIZE; lz++){
                final int z = lz + baseZ;

                final int height = heightmapSurface.getHeight(lx, lz);
                final Biome biome = biomes.getBiome(lx, lz);

                if(chunk.getBlock(lx, height, lz) == ClientBlocks.WATER)
                    continue;

                // SELECT SURFACE BLOCKS
                final BlockClient surfaceBlock = switch(biome){
                    case DESERT, BEACH, SNOWY_BEACH -> ClientBlocks.SAND;
                    case SNOWY_TAIGA -> ClientBlocks.SNOWY_GRASS_BLOCK;
                    case TAIGA -> {
                        if(peaksValleysNoise.getNoise(x, z) < 0.2)
                            yield ClientBlocks.PODZOL;
                        yield ClientBlocks.GRASS_BLOCK;
                    }
                    case RIVER, ICE_RIVER, SEA, ICE_SEA -> ClientBlocks.DIRT;
                    default -> ClientBlocks.GRASS_BLOCK;
                };
                final BlockClient subsurfaceBlock = switch(biome){
                    case DESERT, BEACH, SNOWY_BEACH -> ClientBlocks.SAND;
                    default -> ClientBlocks.DIRT;
                };

                // SET SURFACE BLOCKS
                chunk.setBlockFast(lx, height, lz, surfaceBlock);

                final int subsurfaceLayerHeight = random.random(2, 5);
                for(int y = height - subsurfaceLayerHeight; y < height; y++)
                    chunk.setBlockFast(lx, y, lz, subsurfaceBlock);

                // WATER, ICE
                switch(biome){
                    case RIVER, SEA -> {
                        for(int y = height; y <= SEA_LEVEL; y++)
                            if(chunk.getBlock(lx, y, lz) == ClientBlocks.AIR)
                                chunk.setBlockFast(lx, y, lz, ClientBlocks.WATER);
                    }
                    case ICE_RIVER, ICE_SEA -> {
                        for(int y = height; y <= SEA_LEVEL; y++)
                            if(chunk.getBlock(lx, y, lz) == ClientBlocks.AIR)
                                chunk.setBlockFast(lx, y, lz, ClientBlocks.ICE);
                    }
                }

            }
        }

        /** GRASS */

        for(int lx = 0; lx < SIZE; lx++){
            for(int lz = 0; lz < SIZE; lz++){
                final int height = heightmapSurface.getHeight(lx, lz);
                final Biome biome = biomes.getBiome(lx, lz);

                switch(biome){
                    case FOREST -> {
                        if(random.randomBoolean(0.05))
                            chunk.setBlockFast(lx, height + 1, lz, ClientBlocks.GRASS);
                    }
                    case TAIGA, SNOWY_TAIGA -> {
                        if(random.randomBoolean(0.005))
                            chunk.setBlockFast(lx, height + 1, lz, ClientBlocks.GRASS);
                    }
                }

            }
        }

    }

    @Override
    public void decorate(ServerChunk chunk){
        final BlockPool pool = chunk.getLevel().getBlockPool();
        pool.setGeneratingChunk(chunk);

        final int seed = chunk.getLevel().getConfiguration().getSeed();
        final int baseX = chunk.getPosition().x * SIZE;
        final int baseZ = chunk.getPosition().z * SIZE;

        continentalnessNoise.setSeed(seed);
        erosionNoise.setSeed(seed);
        peaksValleysNoise.setSeed(seed);
        temperatureNoise.setSeed(seed);
        humidityNoise.setSeed(seed);
        riverNoise.setSeed(seed);
        random.setSeed(Objects.hash(seed, baseX, baseZ));

        // final Heightmap heightmapUnderwaterSurface = chunk.getHeightMap(HeightmapType.UNDERWATER_SURFACE);
        final Heightmap heightmapSurface = chunk.getHeightMap(HeightmapType.SURFACE);
        final BiomeMap biomes = chunk.getBiomes();

        /** STRUCTURES */

        for(int lx = 0; lx < SIZE; lx++){
            final int x = lx + baseX;
            boolean prevCactusGen = false;

            for(int lz = 0; lz < SIZE; lz++){
                final int z = lz + baseZ;

                final int height = heightmapSurface.getHeight(lx, lz);
                final Biome biome = biomes.getBiome(lx, lz);

                switch(biome){
                    case DESERT -> {
                        if(random.randomBoolean(0.008) && !prevCactusGen){
                            Cactus.generate(pool, x, height + 1, z, random);
                            prevCactusGen = true;
                        }else if(random.randomBoolean(0.00001))
                            DesertPyramid.generate(pool, x, height - 1, z, random);
                        else if(random.randomBoolean(0.00005))
                            MiniPyramid.generate(pool, x, height, z, random);
                    }

                    case FOREST -> {
                        if(random.randomBoolean(0.008)){
                            if(random.randomBoolean(0.7))
                                Tree.generateOakTree(pool, x, height + 1, z, random);
                            else
                                Tree.generateBirchTree(pool, x, height + 1, z, random);
                        }else if(random.randomBoolean(0.00002))
                            House.generate(pool, x, height, z, random);
                    }

                    case TAIGA -> {
                        if(random.randomBoolean(0.01))
                            Tree.generateSpruceTree(pool, x, height + 1, z, random);

                        else if(random.randomBoolean(0.00005))
                            Tower.generate(pool, x, height, z, random);
                    }

                    case SNOWY_TAIGA -> {
                        if(random.randomBoolean(0.003))
                            Tree.generateSpruceTree(pool, x, height + 1, z, random);

                        else if(random.randomBoolean(0.00008))
                            Tower.generate(pool, x, height, z, random);
                    }
                }

            }
        }

    }
    
    
    private static DefaultGenerator instance;

    public static DefaultGenerator getInstance(){
        if(instance == null)
            instance = new DefaultGenerator();

        return instance;
    }

    @Override
    public String getID(){
        return "default";
    }

}
