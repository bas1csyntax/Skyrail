package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.util.Deserializable;
import net.md_5.bungee.protocol.util.Either;
import net.md_5.bungee.protocol.util.NoOrigDeserializable;
import se.llbit.nbt.SpecificTag;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Subtitle extends DefinedPacket
{

    private Deserializable<Either<String, SpecificTag>, BaseComponent> textRaw;

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        textRaw = readBaseComponent( buf, protocolVersion );
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeBaseComponent( textRaw, buf, protocolVersion );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

    public Subtitle(BaseComponent text)
    {
        setText( text );
    }

    public BaseComponent getText()
    {
        if ( textRaw == null )
        {
            return null;
        }
        return textRaw.get();
    }

    public void setText(BaseComponent text)
    {
        if ( text == null )
        {
            this.textRaw = null;
            return;
        }
        this.textRaw = new NoOrigDeserializable<>( text );
    }
}
