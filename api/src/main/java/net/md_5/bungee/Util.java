package net.md_5.bungee;

import com.google.common.base.Joiner;
import com.google.common.primitives.UnsignedLongs;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Series of utility classes to perform various operations.
 */
public class Util
{

    public static final int DEFAULT_PORT = 25565;

    /**
     * Method to transform human readable addresses into usable address objects.
     *
     * @param hostline in the format of 'host:port'
     * @return the constructed hostname + port.
     */
    @Contract(pure = true)
    public static SocketAddress getAddr(@NotNull String hostline)
    {
        URI uri = null;
        try
        {
            uri = new URI( hostline );
        } catch ( URISyntaxException ex )
        {
        }

        if ( uri != null && "unix".equals( uri.getScheme() ) )
        {
            return new DomainSocketAddress( uri.getPath() );
        }

        if ( uri == null || uri.getHost() == null )
        {
            try
            {
                uri = new URI( "tcp://" + hostline );
            } catch ( URISyntaxException ex )
            {
                throw new IllegalArgumentException( "Bad hostline: " + hostline, ex );
            }
        }

        if ( uri.getHost() == null )
        {
            throw new IllegalArgumentException( "Invalid host/address: " + hostline );
        }

        return new InetSocketAddress( uri.getHost(), ( uri.getPort() ) == -1 ? DEFAULT_PORT : uri.getPort() );
    }

    /**
     * Formats an integer as a hex value.
     *
     * @param i the integer to format
     * @return the hex representation of the integer
     */
    @NotNull
    @Contract(pure = true)
    public static String hex(int i)
    {
        return String.format( "0x%02X", i );
    }

    /**
     * Constructs a pretty one line version of a {@link Throwable}. Useful for
     * debugging.
     *
     * @param t the {@link Throwable} to format.
     * @return a string representing information about the {@link Throwable}
     */
    @NotNull
    @Contract(pure = true)
    public static String exception(@NotNull Throwable t)
    {
        // TODO: We should use clear manually written exceptions
        StackTraceElement[] trace = t.getStackTrace();
        return t.getClass().getSimpleName() + " : " + t.getMessage()
                + ( ( trace.length > 0 ) ? " @ " + t.getStackTrace()[0].getClassName() + ":" + t.getStackTrace()[0].getLineNumber() : "" );
    }

    @NotNull
    @Contract(pure = true)
    public static String csv(@NotNull Iterable<?> objects)
    {
        return format( objects, ", " );
    }

    @NotNull
    @Contract(pure = true)
    public static String format(@NotNull Iterable<?> objects, @NotNull String separators)
    {
        return Joiner.on( separators ).join( objects );
    }

    /**
     * Converts a String to a UUID
     *
     * @param uuid The string to be converted
     * @return The result
     */
    @NotNull
    @Contract(pure = true)
    public static UUID getUUID(@NotNull String uuid)
    {
        return new UUID( UnsignedLongs.parseUnsignedLong( uuid.substring( 0, 16 ), 16 ), UnsignedLongs.parseUnsignedLong( uuid.substring( 16 ), 16 ) );
    }
}
