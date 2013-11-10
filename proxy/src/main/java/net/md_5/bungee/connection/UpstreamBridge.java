package net.md_5.bungee.connection;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.PluginMessage;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.UserLocation;
import net.md_5.bungee.protocol.packet.PlayerLook;
import net.md_5.bungee.protocol.packet.PlayerPosition;
import net.md_5.bungee.protocol.packet.PlayerPositionAndLook;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

public class UpstreamBridge extends PacketHandler
{

    private final ProxyServer bungee;
    private final UserConnection con;

    public UpstreamBridge( ProxyServer bungee, UserConnection con )
    {
        this.bungee = bungee;
        this.con = con;

        BungeeCord.getInstance().addConnection( con );
        con.getTabList().onConnect();
        con.unsafe().sendPacket( BungeeCord.getInstance().registerChannels() );
    }

    @Override
    public void exception( Throwable t ) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected( ChannelWrapper channel ) throws Exception
    {
        // We lost connection to the client
        PlayerDisconnectEvent event = new PlayerDisconnectEvent( con );
        bungee.getPluginManager().callEvent( event );
        con.getTabList().onDisconnect();
        BungeeCord.getInstance().removeConnection( con );

        if ( con.getServer() != null )
        {
            con.getServer().disconnect( "Quitting" );
        }
    }

    @Override
    public void handle( PacketWrapper packet ) throws Exception
    {
        // EntityMap.rewrite( packet.buf, con.getClientEntityId(), con.getServerEntityId() );
        if ( con.getServer() != null )
        {
            con.getServer().getCh().write( packet );
        }
    }

    @Override
    public void handle( PlayerPosition pos ) throws Exception
    {
        UserLocation loc = con.getLocation();
        loc.setX( pos.getX() );
        loc.setY( pos.getY() );
        loc.setZ( pos.getZ() );
        con.setLocation( loc );
    }

    @Override
    public void handle( PlayerLook look ) throws Exception
    {
        UserLocation loc = con.getLocation();
        loc.setYaw( look.getYaw() );
        loc.setPitch( look.getPitch() );
        con.setLocation( loc );
    }

    @Override
    public void handle( PlayerPositionAndLook pos ) throws Exception
    {
        UserLocation loc = con.getLocation();
        loc.setX( pos.getX() );
        loc.setY( pos.getY() );
        loc.setZ( pos.getZ() );
        loc.setYaw( pos.getYaw() );
        loc.setPitch( pos.getYaw() );
        con.setLocation( loc );
    }

    @Override
    public void handle( KeepAlive alive ) throws Exception
    {
        if ( alive.getRandomId() == con.getSentPingId() )
        {
            int newPing = ( int ) ( System.currentTimeMillis() - con.getSentPingTime() );
            con.getTabList().onPingChange( newPing );
            con.setPing( newPing );
        }
    }

    @Override
    public void handle( Chat chat ) throws Exception
    {
        ChatEvent chatEvent = new ChatEvent( con, con.getServer(), chat.getMessage() );
        if ( !bungee.getPluginManager().callEvent( chatEvent ).isCancelled() )
        {
            chat.setMessage( chatEvent.getMessage() );
            if ( !chatEvent.isCommand() || !bungee.getPluginManager().dispatchCommand( con, chat.getMessage().substring( 1 ) ) )
            {
                con.getLocation().getServer().unsafe().sendPacket( chat );
            }
        }
        throw new CancelSendSignal();
    }

    @Override
    public void handle( TabCompleteRequest tabComplete ) throws Exception
    {
        if ( tabComplete.getCursor().startsWith( "/" ) )
        {
            List<String> results = new ArrayList<>();
            bungee.getPluginManager().dispatchCommand( con, tabComplete.getCursor().substring( 1 ), results );

            if ( !results.isEmpty() )
            {
                con.unsafe().sendPacket( new TabCompleteResponse( results.toArray( new String[ results.size() ] ) ) );
                throw new CancelSendSignal();
            }
        }
    }

    @Override
    public void handle( ClientSettings settings ) throws Exception
    {
        con.setSettings( settings );
    }

    @Override
    public void handle( PluginMessage pluginMessage ) throws Exception
    {
        if ( pluginMessage.getTag().equals( "BungeeCord" ) )
        {
            throw new CancelSendSignal();
        }
        // Hack around Forge race conditions
        if ( pluginMessage.getTag().equals( "FML" ) && pluginMessage.getStream().readUnsignedByte() == 1 )
        {
            throw new CancelSendSignal();
        }

        PluginMessageEvent event = new PluginMessageEvent( con, con.getServer(), pluginMessage.getTag(), pluginMessage.getData().clone() );
        if ( bungee.getPluginManager().callEvent( event ).isCancelled() )
        {
            throw new CancelSendSignal();
        }

        // TODO: Unregister as well?
        if ( pluginMessage.getTag().equals( "REGISTER" ) )
        {
            con.getPendingConnection().getRegisterMessages().add( pluginMessage );
        }
    }

    @Override
    public String toString()
    {
        return "[" + con.getName() + "] -> UpstreamBridge";
    }
}
