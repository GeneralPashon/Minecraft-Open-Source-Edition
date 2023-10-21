package minecraftose.main.command;

import minecraftose.main.command.argument.CommandArg;
import minecraftose.main.command.source.CommandSource;
import minecraftose.server.Server;

import java.util.List;

public class CommandContext{
    
    private final Server server;
    private final CommandSource source;
    private final String command;
    private final List<CommandArg> arguments;
    
    public CommandContext(Server server, CommandSource source, String command, List<CommandArg> arguments){
        this.server = server;
        this.source = source;
        this.command = command;
        this.arguments = arguments;
    }
    
    public Server getServer(){
        return server;
    }
    
    public CommandSource getSource(){
        return source;
    }
    
    public String getCommand(){
        return command;
    }
    
    public CommandArg getArg(int index){
        return arguments.get(index);
    }
    
}
