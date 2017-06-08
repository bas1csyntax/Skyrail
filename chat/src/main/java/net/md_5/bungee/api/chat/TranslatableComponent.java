package net.md_5.bungee.api.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Accessors(chain = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TranslatableComponent extends BaseComponent
{

    private final ResourceBundle locales = ResourceBundle.getBundle( "mojang-translations/en_US" );
    private final Pattern format = Pattern.compile( "%(?:(\\d+)\\$)?([A-Za-z%]|$)" );

    /**
     * The key into the Minecraft locale files to use for the translation. The
     * text depends on the client's locale setting. The console is always en_US
     */
    private String translate;
    /**
     * The components to substitute into the translation
     */
    private List<BaseComponent> with;

    /**
     * Creates a translatable component from the original to clone it.
     *
     * @param original the original for the new translatable component.
     */
    public TranslatableComponent(TranslatableComponent original)
    {
        super( original );
        setTranslate( original.getTranslate() );

        if ( original.getWith() != null )
        {
            List<BaseComponent> temp = new ArrayList<BaseComponent>();
            for ( BaseComponent baseComponent : original.getWith() )
            {
                temp.add( baseComponent.duplicate() );
            }
            setWith( temp );
        }
    }

    /**
     * Creates a translatable component with the passed substitutions
     *
     * @see #translate
     * @see #setWith(java.util.List)
     * @param translate the translation key
     * @param with the {@link java.lang.String}s and
     * {@link net.md_5.bungee.api.chat.BaseComponent}s to use into the
     * translation
     */
    public TranslatableComponent(String translate, Object... with)
    {
        setTranslate( translate );
        List<BaseComponent> temp = new ArrayList<BaseComponent>();
        for ( Object w : with )
        {
            if ( w instanceof String )
            {
                temp.add( new TextComponent( (String) w ) );
            } else
            {
                temp.add( (BaseComponent) w );
            }
        }
        setWith( temp );
    }

    /**
     * Creates a duplicate of this TranslatableComponent.
     *
     * @return the duplicate of this TranslatableComponent.
     */
    @Override
    public TranslatableComponent duplicate()
    {
        return new TranslatableComponent( this );
    }

    @Override
    public TranslatableComponent setColor(ChatColor color) {
        super.setColor( color );
        return this;
    }

    @Override
    public TranslatableComponent setBold(Boolean bold) {
        super.setBold( bold );
        return this;
    }

    @Override
    public TranslatableComponent setItalic(Boolean italic) {
        super.setItalic( italic );
        return this;
    }

    @Override
    public TranslatableComponent setUnderlined(Boolean underlined) {
        super.setUnderlined( underlined );
        return this;
    }

    @Override
    public TranslatableComponent setStrikethrough(Boolean strikethrough) {
        super.setStrikethrough( strikethrough );
        return this;
    }

    @Override
    public TranslatableComponent setObfuscated(Boolean obfuscated) {
        super.setObfuscated( obfuscated );
        return this;
    }

    @Override
    public TranslatableComponent setInsertion(String insertion) {
        super.setInsertion( insertion );
        return this;
    }

    @Override
    public TranslatableComponent setExtra(List<BaseComponent> components)
    {
        super.setExtra( components );
        return this;
    }

    @Override
    public TranslatableComponent addExtra(String text)
    {
        super.addExtra( text );
        return this;
    }

    @Override
    public TranslatableComponent addExtra(BaseComponent component)
    {
        super.addExtra( component );
        return this;
    }

    @Override
    public TranslatableComponent setClickEvent(ClickEvent clickEvent) {
        super.setClickEvent( clickEvent );
        return this;
    }

    @Override
    public TranslatableComponent setHoverEvent(HoverEvent hoverEvent) {
        super.setHoverEvent( hoverEvent );
        return this;
    }

    /**
     * Sets the translation substitutions to be used in this component. Removes
     * any previously set substitutions
     *
     * @param components the components to substitute
     *
     * @return this TranslatableComponent
     */
    public TranslatableComponent setWith(List<BaseComponent> components)
    {
        for ( BaseComponent component : components )
        {
            component.parent = this;
        }
        with = components;
        return this;
    }

    /**
     * Adds a text substitution to the component. The text will inherit this
     * component's formatting
     *
     * @param text the text to substitute
     *
     * @return this TranslatableComponent
     */
    public TranslatableComponent addWith(String text)
    {
        return addWith( new TextComponent( text ) );
    }

    /**
     * Adds a component substitution to the component. The text will inherit
     * this component's formatting
     *
     * @param component the component to substitute
     *
     * @return this TranslatableComponent
     */
    public TranslatableComponent addWith(BaseComponent component)
    {
        if ( with == null )
        {
            with = new ArrayList<BaseComponent>();
        }
        component.parent = this;
        with.add( component );
        return this;
    }

    @Override
    protected void toPlainText(StringBuilder builder)
    {
        String trans;
        try
        {
            trans = locales.getString( translate );
        } catch ( MissingResourceException ex )
        {
            trans = translate;
        }

        Matcher matcher = format.matcher( trans );
        int position = 0;
        int i = 0;
        while ( matcher.find( position ) )
        {
            int pos = matcher.start();
            if ( pos != position )
            {
                builder.append( trans.substring( position, pos ) );
            }
            position = matcher.end();

            String formatCode = matcher.group( 2 );
            switch ( formatCode.charAt( 0 ) )
            {
                case 's':
                case 'd':
                    String withIndex = matcher.group( 1 );
                    with.get( withIndex != null ? Integer.parseInt( withIndex ) - 1 : i++ ).toPlainText( builder );
                    break;
                case '%':
                    builder.append( '%' );
                    break;
            }
        }
        if ( trans.length() != position )
        {
            builder.append( trans.substring( position, trans.length() ) );
        }

        super.toPlainText( builder );
    }

    @Override
    protected void toLegacyText(StringBuilder builder)
    {
        String trans;
        try
        {
            trans = locales.getString( translate );
        } catch ( MissingResourceException e )
        {
            trans = translate;
        }

        Matcher matcher = format.matcher( trans );
        int position = 0;
        int i = 0;
        while ( matcher.find( position ) )
        {
            int pos = matcher.start();
            if ( pos != position )
            {
                addFormat( builder );
                builder.append( trans.substring( position, pos ) );
            }
            position = matcher.end();

            String formatCode = matcher.group( 2 );
            switch ( formatCode.charAt( 0 ) )
            {
                case 's':
                case 'd':
                    String withIndex = matcher.group( 1 );
                    with.get( withIndex != null ? Integer.parseInt( withIndex ) - 1 : i++ ).toLegacyText( builder );
                    break;
                case '%':
                    addFormat( builder );
                    builder.append( '%' );
                    break;
            }
        }
        if ( trans.length() != position )
        {
            addFormat( builder );
            builder.append( trans.substring( position, trans.length() ) );
        }
        super.toLegacyText( builder );
    }

    private void addFormat(StringBuilder builder)
    {
        builder.append( getColor() );
        if ( isBold() )
        {
            builder.append( ChatColor.BOLD );
        }
        if ( isItalic() )
        {
            builder.append( ChatColor.ITALIC );
        }
        if ( isUnderlined() )
        {
            builder.append( ChatColor.UNDERLINE );
        }
        if ( isStrikethrough() )
        {
            builder.append( ChatColor.STRIKETHROUGH );
        }
        if ( isObfuscated() )
        {
            builder.append( ChatColor.MAGIC );
        }
    }
}
