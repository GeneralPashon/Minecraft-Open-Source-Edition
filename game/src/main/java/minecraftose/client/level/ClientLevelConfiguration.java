package minecraftose.client.level;

import minecraftose.main.level.LevelConfiguration;

public class ClientLevelConfiguration extends LevelConfiguration {
    
    public void load(String name){
        super.load(name);
    }

    public void setName(String name){
        this.name = name;
    }
    
}