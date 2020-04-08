package net.md_5.bungee.connection;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnection.KeepAliveData;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.SettingsChangedEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

public class UpstreamBridge extends PacketHandler
{


    private final ProxyServer bungee;
    private final UserConnection con;

    private long lastTabCompletion = -1; //BotFilter

    public UpstreamBridge(ProxyServer bungee, UserConnection con)
    {
        this.bungee = bungee;
        this.con = con;

        BungeeCord.getInstance().addConnection( con );
        con.getTabListHandler().onConnect();
        con.unsafe().sendPacket( BungeeCord.getInstance().registerChannels( con.getPendingConnection().getVersion() ) );

        //BotFilter start
        if ( con.isCallSettingsEvent() )
        {
            SettingsChangedEvent settingsEvent = new SettingsChangedEvent( con );
            bungee.getPluginManager().callEvent( settingsEvent );
            con.setCallSettingsEvent( false );
        }
        if ( !con.getDelayedPluginMessages().isEmpty() )
        {
            con.setDelayedPluginMessages( clearPluginMessages( con.getDelayedPluginMessages() ) );
        }
        //BotFilter end
    }

    //BotFilter start
    private List<PluginMessage> clearPluginMessages(List<PluginMessage> delayedPluginMessages)
    {
        List<PluginMessage> cleared = new ArrayList<>();
        for ( PluginMessage message : delayedPluginMessages )
        {
            try
            {
                this.handle( message );
                cleared.add( message );
            } catch ( Throwable t )
            {
                if ( !( t instanceof CancelSendSignal ) )
                {
                    throw new RuntimeException( t.getMessage(), t );
                }
            }
        }
        delayedPluginMessages.clear();
        return cleared;
    }
    //BotFilter end

    @Override
    public void exception(Throwable t) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        // We lost connection to the client
        PlayerDisconnectEvent event = new PlayerDisconnectEvent( con );
        bungee.getPluginManager().callEvent( event );
        con.getTabListHandler().onDisconnect();
        BungeeCord.getInstance().removeConnection( con );

        if ( con.getServer() != null )
        {
            // Manually remove from everyone's tab list
            // since the packet from the server arrives
            // too late
            // TODO: This should only done with server_unique
            //       tab list (which is the only one supported
            //       currently)
            PlayerListItem packet = new PlayerListItem();
            packet.setAction( PlayerListItem.Action.REMOVE_PLAYER );
            PlayerListItem.Item item = new PlayerListItem.Item();
            item.setUuid( con.getUniqueId() );
            packet.setItems( new PlayerListItem.Item[]
            {
                item
            } );
            for ( ProxiedPlayer player : con.getServer().getInfo().getPlayers() )
            {
                player.unsafe().sendPacket( packet );
            }
            con.getServer().disconnect( "Quitting" );
        }
    }

    @Override
    public void writabilityChanged(ChannelWrapper channel) throws Exception
    {
        if ( con.getServer() != null )
        {
            con.getServer().getCh().getHandle().config().setAutoRead( channel.getHandle().isWritable() );
        }
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) throws Exception
    {
        return con.getServer() != null || packet.packet instanceof PluginMessage;
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        if ( con.getServer() != null )
        {
            con.getEntityRewrite().rewriteServerbound( packet.buf, con.getClientEntityId(), con.getServerEntityId(), con.getPendingConnection().getVersion() );
            con.getServer().getCh().write( packet );
        }
    }

    @Override
    public void handle(KeepAlive alive) throws Exception
    {
        //BotFilter start - fix possibility race condition caused by botfilter keep alive packet
        if ( con.getServer() == null )
        {
            throw CancelSendSignal.INSTANCE;
        }
        //BotFilter end

        Queue<KeepAliveData> keepAliveDataQueue = con.getServer().getKeepAlives();
        KeepAliveData keepAliveData = keepAliveDataQueue.peek();

        if ( keepAliveData != null && alive.getRandomId() == keepAliveData.getId() )
        {
            keepAliveDataQueue.remove();
            int newPing = (int) ( System.currentTimeMillis() - keepAliveData.getTime() );
            con.getTabListHandler().onPingChange( newPing );
            con.setPing( newPing );
        } else
        {
            throw CancelSendSignal.INSTANCE;
        }
    }

    @Override
    public void handle(Chat chat) throws Exception
    {
        int maxLength = ( con.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_11 ) ? 256 : 100;
        Preconditions.checkArgument( chat.getMessage().length() <= maxLength, "Chat message too long" ); // Mojang limit, check on updates

        ChatEvent chatEvent = new ChatEvent( con, con.getServer(), chat.getMessage() );
        if ( !bungee.getPluginManager().callEvent( chatEvent ).isCancelled() )
        {
            chat.setMessage( chatEvent.getMessage() );
            if ( !chatEvent.isCommand() || !bungee.getPluginManager().dispatchCommand( con, chat.getMessage().substring( 1 ) ) )
            {
                con.getServer().unsafe().sendPacket( chat );
            }
        }
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(TabCompleteRequest tabComplete) throws Exception
    {
        //BotFilter start
        long now = System.currentTimeMillis();
        if ( con.getPendingConnection().getVersion() <= ProtocolConstants.MINECRAFT_1_12_2 && lastTabCompletion > 0 && ( now - lastTabCompletion ) <= 500 )
        {
            throw CancelSendSignal.INSTANCE;
        }
        lastTabCompletion = now;
        //BotFilter end

        List<String> suggestions = new ArrayList<>();

        if ( tabComplete.getCursor().startsWith( "/" ) )
        {
            bungee.getPluginManager().dispatchCommand( con, tabComplete.getCursor().substring( 1 ), suggestions );
        }

        TabCompleteEvent tabCompleteEvent = new TabCompleteEvent( con, con.getServer(), tabComplete.getCursor(), suggestions );
        bungee.getPluginManager().callEvent( tabCompleteEvent );

        if ( tabCompleteEvent.isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        List<String> results = tabCompleteEvent.getSuggestions();
        if ( !results.isEmpty() )
        {
            // Unclear how to handle 1.13 commands at this point. Because we don't inject into the command packets we are unlikely to get this far unless
            // Bungee plugins are adding results for commands they don't own anyway
            if ( con.getPendingConnection().getVersion() < ProtocolConstants.MINECRAFT_1_13 )
            {
                con.unsafe().sendPacket( new TabCompleteResponse( results ) );
            } else
            {
                int start = tabComplete.getCursor().lastIndexOf( ' ' ) + 1;
                int end = tabComplete.getCursor().length();
                StringRange range = StringRange.between( start, end );

                List<Suggestion> brigadier = new LinkedList<>();
                for ( String s : results )
                {
                    brigadier.add( new Suggestion( range, s ) );
                }

                con.unsafe().sendPacket( new TabCompleteResponse( tabComplete.getTransactionId(), new Suggestions( range, brigadier ) ) );
            }
            throw CancelSendSignal.INSTANCE;
        }
    }

    @Override
    public void handle(ClientSettings settings) throws Exception
    {
        con.setSettings( settings );

        SettingsChangedEvent settingsEvent = new SettingsChangedEvent( con );
        bungee.getPluginManager().callEvent( settingsEvent );
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        if ( pluginMessage.getTag().equals( "BungeeCord" ) )
        {
            throw CancelSendSignal.INSTANCE;
        }

        if ( BungeeCord.getInstance().config.isForgeSupport() )
        {
            // Hack around Forge race conditions
            if ( pluginMessage.getTag().equals( "FML" ) && pluginMessage.getStream().readUnsignedByte() == 1 )
            {
                throw CancelSendSignal.INSTANCE;
            }

            // We handle forge handshake messages if forge support is enabled.
            if ( pluginMessage.getTag().equals( ForgeConstants.FML_HANDSHAKE_TAG ) )
            {
                // Let our forge client handler deal with this packet.
                con.getForgeClientHandler().handle( pluginMessage );
                throw CancelSendSignal.INSTANCE;
            }

            if ( con.getServer() != null && !con.getServer().isForgeServer() && pluginMessage.getData().length > Short.MAX_VALUE )
            {
                // Drop the packet if the server is not a Forge server and the message was > 32kiB (as suggested by @jk-5)
                // Do this AFTER the mod list, so we get that even if the intial server isn't modded.
                throw CancelSendSignal.INSTANCE;
            }
        }

        PluginMessageEvent event = new PluginMessageEvent( con, con.getServer(), pluginMessage.getTag(), pluginMessage.getData().clone() );
        if ( bungee.getPluginManager().callEvent( event ).isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        // TODO: Unregister as well?
        if ( PluginMessage.SHOULD_RELAY.apply( pluginMessage ) )
        {
            con.getPendingConnection().getRelayMessages().add( pluginMessage );
        }
    }

    @Override
    public String toString()
    {
        return "[" + con.getName() + "] -> UpstreamBridge";
    }
}
