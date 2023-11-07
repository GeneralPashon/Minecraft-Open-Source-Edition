package minecraftose.client.level;

import jpize.Jpize;
import minecraftose.client.chunk.ClientChunk;
import minecraftose.client.chunk.builder.ChunkBuilder;
import minecraftose.client.entity.LocalPlayer;
import minecraftose.main.chunk.ChunkUtils;
import minecraftose.main.chunk.storage.ChunkPos;
import minecraftose.main.level.ChunkManager;
import minecraftose.main.level.ChunkManagerUtils;
import minecraftose.main.network.packet.s2c.game.S2CPacketChunk;
import minecraftose.main.network.packet.c2s.game.C2SPacketChunkRequest;
import jpize.util.time.FpsCounter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

import static minecraftose.main.level.ChunkManagerUtils.distToChunk;

public class ClientChunkManager extends ChunkManager {

    public static final int BUILD_CHUNK_CORES = 1;

    
    private final ClientLevel level;
    public final FpsCounter tps;

    private final ChunkBuilder[] chunkBuilders;
    private final ExecutorService executor;

    private Thread thread;
    private volatile boolean chunksLoadIsEnd;

    private final ConcurrentHashMap<ChunkPos, Long> requestedChunks;
    private final CopyOnWriteArrayList<ChunkPos> frontiers;
    private final ConcurrentHashMap<ChunkPos, ClientChunk> allChunks;
    private final ConcurrentLinkedDeque<ClientChunk> toBuildQueue;


    public ClientChunkManager(ClientLevel level){
        this.level = level;

        this.chunkBuilders = new ChunkBuilder[BUILD_CHUNK_CORES];
        for(int i = 0; i < chunkBuilders.length; i++)
            chunkBuilders[i] = new ChunkBuilder(this);

        this.executor = Executors.newScheduledThreadPool(BUILD_CHUNK_CORES, runnable->{
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });

        this.requestedChunks = new ConcurrentHashMap<>();
        this.frontiers = new CopyOnWriteArrayList<>();
        this.allChunks = new ConcurrentHashMap<>();
        this.toBuildQueue = new ConcurrentLinkedDeque<>();
        
        this.tps = new FpsCounter();
    }
    
    public ClientLevel getLevel(){
        return level;
    }


    public ChunkBuilder[] getChunkBuilders(){
        return chunkBuilders;
    }

    private int getFreeBuilderIndex(){
        for(int i = 0; i < chunkBuilders.length; i++)
            if(chunkBuilders[i].getState() == 0)
                return i;
            // else
            //     System.out.print(" " + i + ": " + chunkBuilders[i].getState());
        return -1;
    }

    private int index;

    private void build(ClientChunk chunk){
        chunkBuilders[0].build(chunk);

        // System.out.println("wait for index");
        // while(index == -1 || chunkBuilders[index].getState() != 0)
        //     index = getFreeBuilderIndex();
        // System.out.println(index);

        //final int finalIndex = index;
        //executor.execute(() -> {
        //    try{
        //        chunkBuilders[finalIndex].build(chunk);
        //    }catch(Exception e){
        //        System.out.println(e);
        //    }
        //});

        //index++;
        //if(index == BUILD_CHUNK_CORES)
        //    index = 0;
    }
    
    
    public void startLoadChunks(){
        thread = new Thread(() -> {
            chunksLoadIsEnd = false;
            try{
                while(!Thread.currentThread().isInterrupted()){
                    tps.count();
                    findChunks();
                    buildChunks();
                    checkChunks();

                    Thread.yield();
                }
            }catch(Exception ignored){ }
            chunksLoadIsEnd = true;
        }, "ClientChunkManager-Thread");

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }
    
    public void dispose(){
        executor.shutdownNow();
        try{
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }catch(InterruptedException ignored){ }
        thread.interrupt();
        while(!chunksLoadIsEnd); //: NOT ENDED!!!!!!!!!!!!!!!!
        System.out.println("ENDED");
        clear();
    }

    public void reset(){
        dispose();
        clear();
        startLoadChunks();
    }

    public void clear(){
        toBuildQueue   .clear();
        requestedChunks.clear();
        frontiers      .clear();

        for(ClientChunk chunk: allChunks.values())
            chunk.getMeshStack().dispose();
        allChunks.clear();
    }

    public void reload(){
        dispose();
        clear();
        startLoadChunks();
    }
    
    
    private void findChunks(){
        if(frontiers.isEmpty()){
            final LocalPlayer player = level.getGame().getPlayer();

            putFrontier(new ChunkPos(
                ChunkUtils.getChunkPos(player.getPosition().xFloor()),
                ChunkUtils.getChunkPos(player.getPosition().zFloor())
            ));
        }
        
        for(ChunkPos frontierPos: frontiers){
            ensureFrontier(frontierPos.getNeighbor(-1,  0));
            ensureFrontier(frontierPos.getNeighbor( 1,  0));
            ensureFrontier(frontierPos.getNeighbor( 0, -1));
            ensureFrontier(frontierPos.getNeighbor( 0,  1));
        }

        frontiers.removeIf(this::isOffTheGrid);
    }
    
    private void ensureFrontier(ChunkPos chunkPos){
        if(frontiers.contains(chunkPos) || isOffTheGrid(chunkPos))
            return;

        putFrontier(chunkPos);
    }

    private void putFrontier(ChunkPos frontierPos){
        frontiers.add(frontierPos);

        if(!allChunks.containsKey(frontierPos) && !requestedChunks.containsKey(frontierPos) && toBuildQueue.stream().noneMatch(chunk -> chunk.getPosition().equals(frontierPos))){
            getLevel().getGame().getConnectionHandler().sendPacket(new C2SPacketChunkRequest(frontierPos.x, frontierPos.z));
            requestedChunks.put(frontierPos, System.currentTimeMillis());
        }
    }
    
    private void buildChunks(){
        while(!toBuildQueue.isEmpty()){
            final ClientChunk chunk = toBuildQueue.poll();
            if(chunk == null)
                continue;
            
            if(isOffTheGrid(chunk.getPosition()))
                continue;

            build(chunk);
        }
    }
    
    
    public void checkChunks(){
        for(ClientChunk chunk: allChunks.values())
            if(isOffTheGrid(chunk.getPosition()))
                unloadChunk(chunk);
        
        for(Map.Entry<ChunkPos, Long> entry: requestedChunks.entrySet())
            if(System.currentTimeMillis() - entry.getValue() > 500)
                requestedChunks.remove(entry.getKey());
    }
    
    
    public void receivedChunk(S2CPacketChunk packet){
        final ClientChunk chunk = packet.getChunk(level);

        allChunks.put(chunk.getPosition(), chunk);
        requestedChunks.remove(chunk.getPosition());

        chunk.rebuild(false);
        ChunkManagerUtils.rebuildNeighborChunks(chunk);
    }
    
    
    public void unloadChunk(ClientChunk chunk){
        Jpize.execSync(() -> chunk.getMeshStack().dispose());
        allChunks.remove(chunk.getPosition());
    }
    
    public void rebuildChunk(ClientChunk chunk, boolean important){
        if(!toBuildQueue.contains(chunk)){
            if(important)
                toBuildQueue.addFirst(chunk);
            else
                toBuildQueue.addLast(chunk);
        }
    }
    
    
    @Override
    public ClientChunk getChunk(ChunkPos chunkPos){
        return allChunks.get(chunkPos);
    }
    
    @Override
    public ClientChunk getChunk(int chunkX, int chunkZ){
        return getChunk(new ChunkPos(chunkX, chunkZ));
    }
    
    
    public Collection<ClientChunk> getAllChunks(){
        return allChunks.values();
    }

    
    private boolean isOffTheGrid(ChunkPos chunkPos){
        final LocalPlayer player = level.getGame().getPlayer();
        if(player == null)
            return true;

        final int renderDist = level.getGame().getSession().getOptions().getRenderDistance();
        final float distToChunk = distToChunk(chunkPos.x, chunkPos.z, player.getPosition());

        return distToChunk > renderDist;
    }

}
