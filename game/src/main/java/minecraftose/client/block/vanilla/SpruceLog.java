package minecraftose.client.block.vanilla;

import minecraftose.client.block.Block;
import minecraftose.client.block.BlockRotation;
import minecraftose.client.block.model.BlockModel;
import minecraftose.client.block.shape.BlockCollide;
import minecraftose.client.block.shape.BlockCursor;
import minecraftose.client.chunk.mesh.ChunkMeshType;
import minecraftose.client.resources.GameResources;
import minecraftose.main.Dir;
import minecraftose.main.audio.BlockSoundPack;
import minecraftose.main.chunk.ChunkUtils;

public class SpruceLog extends Block{

    public SpruceLog(int id){
        super(id);
    }

    @Override
    public void load(GameResources resources){
        final BlockModel model = new BlockModel(ChunkMeshType.SOLID)
            .sideXZFaces(resources.getBlockRegion("spruce_log"))
            .yFaces(resources.getBlockRegion("spruce_log_top"));

        newState()
                .setSolid(true)
                .setLightLevel(0)
                .setOpacity(ChunkUtils.MAX_LIGHT_LEVEL)
                .setTranslucent(false)
                .setSoundPack(BlockSoundPack.WOOD)
                .setFacing(Dir.POSITIVE_Y)
                .setModel(model)
                .setCollide(BlockCollide.SOLID)
                .setCursor(BlockCursor.SOLID);

        newState()
                .setSolid(true)
                .setLightLevel(0)
                .setOpacity(ChunkUtils.MAX_LIGHT_LEVEL)
                .setTranslucent(false)
                .setSoundPack(BlockSoundPack.WOOD)
                .setFacing(Dir.NEGATIVE_Z)
                .setModel(model.rotated(BlockRotation.X90))
                .setCollide(BlockCollide.SOLID)
                .setCursor(BlockCursor.SOLID);

        newState()
                .setSolid(true)
                .setLightLevel(0)
                .setOpacity(ChunkUtils.MAX_LIGHT_LEVEL)
                .setTranslucent(false)
                .setSoundPack(BlockSoundPack.WOOD)
                .setFacing(Dir.POSITIVE_X)
                .setModel(model.rotated(BlockRotation.Z90))
                .setCollide(BlockCollide.SOLID)
                .setCursor(BlockCursor.SOLID);
    }

}