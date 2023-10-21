package minecraftose.client.options;

import jpize.Jpize;
import jpize.files.Resource;
import jpize.glfw.key.Key;
import minecraftose.client.Minecraft;
import minecraftose.client.control.camera.GameCamera;
import minecraftose.main.SharedConstants;
import jpize.util.io.FastReader;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Options{

    public static int UNLIMITED_FPS_THRESHOLD = 256;

    public static int DEFAULT_FIELD_OF_VIEW = 85;
    public static int DEFAULT_RENDER_DISTANCE = 14;
    public static float DEFAULT_MOUSE_SENSITIVITY = 1F;
    public static float DEFAULT_BRIGHTNESS = 0.5F;
    public static boolean DEFAULT_FOG = true;

    public static int DEFAULT_MAX_FRAME_RATE = 0; // VSync
    public static boolean DEFAULT_FULLSCREEN = false;
    public static boolean DEFAULT_SHOW_FPS = false;
    public static boolean DEFAULT_FIRST_PERSON_MODEL = false;
    public static String DEFAULT_HOST = "0.0.0.0:" + SharedConstants.DEFAULT_PORT;


    private final Minecraft session;
    private final Resource fileResource;
    
    private String host = DEFAULT_HOST;
    private final Map<KeyMapping, Key> keyMappings;
    private int fieldOfView = DEFAULT_FIELD_OF_VIEW;
    private int renderDistance = DEFAULT_RENDER_DISTANCE;
    private int maxFrameRate = DEFAULT_MAX_FRAME_RATE; // 0 - VSync; [1, 255]; 256 - Unlimited
    private boolean fullscreen = DEFAULT_FULLSCREEN;
    private boolean showFps = DEFAULT_SHOW_FPS;
    private float mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
    private boolean fog = DEFAULT_FOG;
    private float brightness = DEFAULT_BRIGHTNESS;
    private boolean firstPersonModel = DEFAULT_FIRST_PERSON_MODEL;
    

    public Options(Minecraft session, String gameDirPath){
        this.session = session;

        this.keyMappings = new HashMap<>();
        this.fileResource = new Resource(gameDirPath + "options.txt", true);
        this.fileResource.create();
    }
    

    public void load(){
        final FastReader reader = fileResource.getReader();

        try{
            while(reader.hasNext()){
                String[] parts = reader.nextLine().split(" : ");
                if(parts.length != 2)
                    continue;

                String value = parts[1].trim();

                String[] keyParts = parts[0].split("\\.");
                if(keyParts.length != 2)
                    continue;

                String category = keyParts[0];
                String key = keyParts[1];

                // category.key : value
                switch(category){
                    case "remote" -> {
                        switch(key){
                            case "host" -> setRemoteHost(value);
                        }
                    }
                    case "graphics" -> {
                        switch(key){
                            case "fieldOfView" -> setFieldOfView(Integer.parseInt(value));
                            case "renderDistance" -> setRenderDistance(Integer.parseInt(value));
                            case "maxFramerate" -> setMaxFrameRate(Integer.parseInt(value));
                            case "fullscreen" -> setFullscreen(Boolean.parseBoolean(value));
                            case "showFps" -> setShowFPS(Boolean.parseBoolean(value));
                            case "brightness" -> setBrightness(Float.parseFloat(value));
                            case "fog" -> setFog(Boolean.parseBoolean(value));
                            case "firstPersonModel" -> setFirstPersonModel(Boolean.parseBoolean(value));
                        }
                    }
                    case "key" -> setKey(KeyMapping.valueOf(key.toUpperCase()), Key.valueOf(value.toUpperCase()));
                    case "control" -> {
                        switch(key){
                            case "mouseSensitivity" -> setMouseSensitivity(Float.parseFloat(value));
                        }
                    }
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void save(){
        final PrintStream out = fileResource.getWriter();
        
        out.println("remote.host : " + host);
        out.println("graphics.fieldOfView : " + fieldOfView);
        out.println("graphics.renderDistance : " + renderDistance);
        out.println("graphics.maxFramerate : " + maxFrameRate);
        out.println("graphics.fullscreen : " + fullscreen);
        out.println("graphics.showFps : " + showFps);
        out.println("graphics.brightness : " + brightness);
        out.println("graphics.fog : " + fog);
        out.println("graphics.firstPersonModel : " + firstPersonModel);

        out.println("control.mouseSensitivity : " + mouseSensitivity);

        for(KeyMapping keyType: KeyMapping.values())
            out.println("key." + keyType.toString().toLowerCase() + " : " + keyMappings.getOrDefault(keyType, keyType.getDefault()).toString().toLowerCase());
        
        out.close();
    }
    
    public String getHost(){
        return host;
    }
    
    public void setRemoteHost(String host){
        this.host = host;
    }
    

    public Key getKey(KeyMapping keyType){
        return keyMappings.getOrDefault(keyType, keyType.getDefault());
    }

    public void setKey(KeyMapping keyType, Key key){
        keyMappings.put(keyType, key);
    }


    public int getFieldOfView(){
        return fieldOfView;
    }

    public void setFieldOfView(int fieldOfView){
        this.fieldOfView = fieldOfView;
        
        final GameCamera camera = session.getGame().getCamera();
        if(camera != null)
            camera.setFov(fieldOfView);
    }


    public int getRenderDistance(){
        return renderDistance;
    }

    public void setRenderDistance(int renderDistance){
        this.renderDistance = renderDistance;
        
        final GameCamera camera = session.getGame().getCamera();
        if(camera != null)
            camera.setDistance(renderDistance);
    }


    public int getMaxFrameRate(){
        return maxFrameRate;
    }

    public void setMaxFrameRate(int maxFrameRate){
        this.maxFrameRate = maxFrameRate;
        session.getFpsSync().setTPS(maxFrameRate);

        session.getFpsSync().enable(maxFrameRate > 0 && maxFrameRate < UNLIMITED_FPS_THRESHOLD);
        Jpize.setVsync(maxFrameRate == 0);
    }


    public boolean isFullscreen(){
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen){
        this.fullscreen = fullscreen;

        Jpize.window().setFullscreen(fullscreen);
    }


    public boolean isShowFPS(){
        return showFps;
    }

    public void setShowFPS(boolean showFps){
        this.showFps = showFps;
    }
    
    
    public float getBrightness(){
        return brightness;
    }
    
    public void setBrightness(float brightness){
        this.brightness = brightness;
    }


    public boolean isFogEnabled(){
        return fog;
    }

    public void setFog(boolean fog){
        this.fog = fog;
    }


    public float getMouseSensitivity(){
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float mouseSensitivity){
        this.mouseSensitivity = mouseSensitivity;
        session.getGame().getPlayer().getController().getRotationController().setSensitivity(mouseSensitivity);
    }
    
    
    public boolean isFirstPersonModel(){
        return firstPersonModel;
    }
    
    public void setFirstPersonModel(boolean firstPersonModel){
        this.firstPersonModel = firstPersonModel;
    }

}
