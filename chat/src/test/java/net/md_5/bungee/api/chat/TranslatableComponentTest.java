package net.md_5.bungee.api.chat;

import static org.junit.jupiter.api.Assertions.*;
import net.md_5.bungee.chat.ComponentSerializer;
import org.junit.jupiter.api.Test;

public class TranslatableComponentTest
{

    @Test
    public void testMissingPlaceholdersAdded()
    {
        TranslatableComponent testComponent = new TranslatableComponent( "Test string with %s placeholders: %s", 2, "aoeu" );
        assertEquals( "Test string with 2 placeholders: aoeu", testComponent.toPlainText() );
        assertEquals( "Test string with 2 placeholders: aoeu", testComponent.toLegacyText() );
    }

    @Test
    public void testJsonSerialisation()
    {
        TranslatableComponent testComponent = new TranslatableComponent( "Test string with %s placeholder", "a" );
        String jsonString = ComponentSerializer.toString( testComponent );
        BaseComponent[] baseComponents = ComponentSerializer.parse( jsonString );

        assertEquals( "Test string with a placeholder", BaseComponent.toPlainText( baseComponents ) );
        assertEquals( "Test string with a placeholder", BaseComponent.toLegacyText( baseComponents ) );
    }
}
