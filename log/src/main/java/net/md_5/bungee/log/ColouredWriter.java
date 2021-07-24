package net.md_5.bungee.log;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import jline.console.ConsoleReader;
import net.md_5.bungee.api.ChatColor;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

public class ColouredWriter extends Handler
{

    private static final String[] ANSI_TABLE = new String[67];

    static
    {
        ANSI_TABLE[ ChatColor.BLACK.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.BLACK ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_BLUE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.BLUE ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_GREEN.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.GREEN ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_AQUA.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.CYAN ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_RED.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.RED ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_PURPLE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.MAGENTA ).boldOff().toString();
        ANSI_TABLE[ ChatColor.GOLD.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.YELLOW ).boldOff().toString();
        ANSI_TABLE[ ChatColor.GRAY.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.WHITE ).boldOff().toString();
        ANSI_TABLE[ ChatColor.DARK_GRAY.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.BLACK ).boldOff().toString();
        ANSI_TABLE[ ChatColor.BLUE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.BLUE ).boldOff().toString();
        ANSI_TABLE[ ChatColor.GREEN.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.GREEN ).boldOff().toString();
        ANSI_TABLE[ ChatColor.AQUA.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.CYAN ).boldOff().toString();
        ANSI_TABLE[ ChatColor.RED.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.RED ).bold().toString();
        ANSI_TABLE[ ChatColor.LIGHT_PURPLE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.MAGENTA ).bold().toString();
        ANSI_TABLE[ ChatColor.YELLOW.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.YELLOW ).bold().toString();
        ANSI_TABLE[ ChatColor.WHITE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).fg( Ansi.Color.WHITE ).bold().toString();
        ANSI_TABLE[ ChatColor.MAGIC.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.BLINK_SLOW ).toString();
        ANSI_TABLE[ ChatColor.BOLD.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.UNDERLINE_DOUBLE ).toString();
        ANSI_TABLE[ ChatColor.STRIKETHROUGH.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.STRIKETHROUGH_ON ).toString();
        ANSI_TABLE[ ChatColor.UNDERLINE.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.UNDERLINE ).toString();
        ANSI_TABLE[ ChatColor.ITALIC.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.ITALIC ).toString();
        ANSI_TABLE[ ChatColor.RESET.toString().charAt( 1 ) - 48 ] = Ansi.ansi().a( Ansi.Attribute.RESET ).toString();
    }

    private static final String formatAnsi(String msg)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for ( int index = 0, len = msg.length(); index < len; index++ )
        {
            char currentChar = msg.charAt( index );
            if ( currentChar == '§' )
            {
                if ( index == len - 1 )
                {
                    return stringBuilder.append( '§' ).toString();
                }
                char predictedColorCode = msg.charAt( ++index );
                String ansi = ANSI_TABLE[ predictedColorCode - 48 ];
                if ( ansi != null )
                {
                    stringBuilder.append( ansi );
                } else
                {
                    stringBuilder.append( '§' ).append( predictedColorCode );
                }
            } else
            {
                stringBuilder.append( currentChar );
            }
        }

        return stringBuilder.toString();
    }
    //
    private final ConsoleReader console;

    public ColouredWriter(ConsoleReader console)
    {
        this.console = console;
    }

    public void print(String s)
    {
        try
        {
            console.print( Ansi.ansi().eraseLine( Erase.ALL ).toString() + ConsoleReader.RESET_LINE + formatAnsi( s ) + Ansi.ansi().reset().toString() );
            console.drawLine();
            console.flush();
        } catch ( IOException ex )
        {
        }
    }

    @Override
    public void publish(LogRecord record)
    {
        if ( isLoggable( record ) )
        {
            print( getFormatter().format( record ) );
        }
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close() throws SecurityException
    {
    }
}
