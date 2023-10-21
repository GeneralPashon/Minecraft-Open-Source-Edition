package minecraftose.main.net.packet.serverbound;

import jpize.net.tcp.packet.IPacket;
import minecraftose.server.net.PlayerGameConnection;
import jpize.util.io.JpizeInputStream;
import jpize.util.io.JpizeOutputStream;

import java.io.IOException;

public class SBPacketRenderDistance extends IPacket<PlayerGameConnection>{
    
    public static final int PACKET_ID = 8;
    
    public SBPacketRenderDistance(){
        super(PACKET_ID);
    }
    
    
    public int renderDistance;
    
    public SBPacketRenderDistance(int renderDistance){
        this();
        this.renderDistance = renderDistance;
    }
    
    
    @Override
    public void write(JpizeOutputStream stream) throws IOException{
        stream.writeInt(renderDistance);
    }
    
    @Override
    public void read(JpizeInputStream stream) throws IOException{
        renderDistance = stream.readInt();
    }
    
    @Override
    public void handle(PlayerGameConnection handler){
        handler.renderDistance(this);
    }
    
}
