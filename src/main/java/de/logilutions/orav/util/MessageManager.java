package de.logilutions.orav.util;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The MessageManager is created to unify the Chat Messages on the Server <br>
 * <br>
 * Example:
 * <br>
 * <code>
 * {@link MessageManager} manager = new {@link #MessageManager()}.{@link #setPrefix(String)}.{@link #setEnabledPrefix(boolean)}.{@link #setBaseColor(ChatColor)}.{@link #setHighlightColor(ChatColor)}.{@link #setErrorColor(ChatColor)};<br>
 * manager.{@link #broadcast(Collection, String)}; <br>
 * </code>
 * <br>
 * <i>Note: Setter do not need to be in a particular order.</i><br>
 * <br>
 */
public class MessageManager {
    private static final String BUNGEE_PLUGIN_CLASS = "net.md_5.bungee.api.plugin.Plugin";
    private static final String BUKKIT_PLUGIN_CLASS = "org.bukkit.plugin.java.JavaPlugin";

    /**
     * The Prefix shown before the Message <br>
     * Default Prefix is : §8[§aMPP§8] <br>
     */
    @Getter
    private String prefix;
    /**
     * The Default Color the Message is sent with <br>
     * You can use '%bc%' in your message to get the Base Color <br>
     * Default Color: {@link ChatColor#GRAY} <br>
     */
    @Getter
    private ChatColor baseColor;
    /**
     * The Color with which you can Highlight some Text <br>
     * You can use '%hc%' in your message to get the Highlight Color <br>
     * Default Color: {@link ChatColor#YELLOW} <br>
     */
    @Getter
    private ChatColor highlightColor;

    /**
     * The Color with which you can sent Error Messages <br>
     * You can use '%ec%' in your message to get the Highlight Color <br>
     * Default Color: {@link ChatColor#RED} <br>
     */
    @Getter
    private ChatColor errorColor;

    /**
     * The Color with which you can sent the upper part of a title <br>
     * Default Color: {@link ChatColor#GREEN} <br>
     */
    @Getter
    private ChatColor upperTitleColor;

    /**
     * The Color with which you can sent the lower part of a title <br>
     * Default Color: {@link ChatColor#GRAY} <br>
     */
    @Getter
    private ChatColor lowerTitleColor;

    /**
     * The ticks it takes the title to fade in <br>
     * Default duration: 5 <br>
     */
    @Getter
    private int titleFadeInLength;

    /**
     * The ticks it takes the title to fade out <br>
     * Default duration: 5 <br>
     */
    @Getter
    private int titleFadeOutLength;

    /**
     * The ticks the title stays <br>
     * Default duration: 40 <br>
     */
    @Getter
    private int titleStayLength;

    /**
     * Determines if the Prefix is printed or not <br>
     * Default Value: true <br>
     */
    private boolean enablePrefix;

    /**
     * Default Prefix: "§8[§aMPP§8]" <br>
     * Default EnabledPrefix: true <br>
     * Default BaseColor: {@link ChatColor#GRAY} <br>
     * Default HighlightColor: {@link ChatColor#YELLOW} <br>
     * Default ErrorColor: {@link ChatColor#RED} <br>
     * Default UpperTitleColor: {@link ChatColor#GREEN} <br>
     * Default LowerTitleColor: {@link ChatColor#GRAY} <br>
     * Default TitleFadeInLength: 5 <br>
     * Default TitleFadeOutLength: 5} <br>
     * Default TitleStayLength: 40 <br>
     */
    public MessageManager() {
        this.setPrefix("§8[§5ORAV7§8]§7");
        this.setEnabledPrefix(true);
        this.setBaseColor(ChatColor.GRAY);
        this.setHighlightColor(ChatColor.YELLOW);
        this.setErrorColor(ChatColor.RED);
        this.setUpperTitleColor(ChatColor.GREEN);
        this.setLowerTitleColor(ChatColor.GRAY);
        this.setTitleFadeInLength(5);
        this.setTitleFadeOutLength(5);
        this.setTitleStayLength(40);
    }


    /**
     * @param player  The Person or Console the message is sent to
     * @param message The Message you send to the Player
     */
    public void sendMessage(Player player, String message) {
        sendMessage(player, this.enablePrefix, new TextComponent(message));
    }

    /**
     * @param player       The Person or Console the message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param message      The Message you send to the Player
     */
    public void sendMessage(Player player, boolean enablePrefix, String message) {
        sendMessage(player, enablePrefix, new TextComponent(message));
    }

    /**
     * @param player     The Person or Console the message is sent to
     * @param component  {@link net.md_5.bungee.api.chat.BaseComponent} Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void sendMessage(Player player, BaseComponent component, BaseComponent... components) {
        sendMessage(player, this.enablePrefix, component, components);
    }

    /**
     * @param player       The Person or Console the message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param component    {@link net.md_5.bungee.api.chat.BaseComponent} Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components   Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void sendMessage(Player player, boolean enablePrefix, BaseComponent component, BaseComponent... components) {
        TextComponent sendComponent = buildComponent(enablePrefix, component);
        for (BaseComponent baseComponent : components) {
            sendComponent.addExtra(buildComponent(false, baseComponent));
        }

        player.spigot().sendMessage(sendComponent);
    }

    /**
     * @param player      The Person or Console the message is sent to
     * @param warningText The Warning text, e.g. "%bc%§Do you really want to reset the world?"
     * @param confirmText The Confirmation Text e.g. "%ec%reset"
     *                    [] will be automatically added!
     * @param hoverText   The Hovertext e.g. "%bc%Click to reset your World"
     * @param command     The command which will be performed
     */
    public void sendConfirmMessage(Player player, String warningText, String confirmText, String hoverText, String command) {
        sendConfirmMessage(player, enablePrefix, warningText, confirmText, hoverText, command);
    }

    /**
     * @param player       The Person or Console the message is sent to
     * @param enablePrefix Enabling Prefix (Only for the warning text!)
     * @param warningText  The Warning text, e.g. "%bc%§Do you really want to reset the world?"
     * @param confirmText  The Confirmation Text e.g. "%ec%reset"
     *                     [] will be automatically added!
     * @param hoverText    The Hovertext e.g. "§5Click to reset your World"
     * @param command      The command which will be performed
     */
    public void sendConfirmMessage(Player player, boolean enablePrefix, String warningText, String confirmText, String hoverText
            , String command) {

        TextComponent sendComponent = buildComponent(enablePrefix, new TextComponent(warningText));
        if (!confirmText.contains("[") && !confirmText.contains("]")) {
            confirmText = createConfirmText(confirmText);
        }
        TextComponent confirmComponent = buildComponent(false, createExecuteMessage(confirmText, hoverText, command));

        player.spigot().sendMessage(sendComponent);
        player.spigot().sendMessage(confirmComponent);
    }

    private String createConfirmText(String confirmText) {
        String confirmationText = "";
        if (confirmText.startsWith("%")) {
            confirmationText += confirmText.substring(0, 4) + createConfirmText(confirmText.substring(4));
        } else if (confirmText.startsWith("§")) {
            confirmationText += confirmText.substring(0, 2) + createConfirmText(confirmText.substring(2));
        } else {
            confirmationText += "[" + confirmText + "]";
        }
        return confirmationText;
    }


    /**
     * @param player           The Person or Console the message is sent to
     * @param text             Text before Command
     * @param textAfterCommand Text After Command
     * @param command          The Command
     */
    public void sendCommandMessage(Player player, boolean enablePrefix, String text, String command, String textAfterCommand) {
        sendCommandMessage(player, enablePrefix, text, command, textAfterCommand, "Klicke um %hc%" + command + " %bc%auszuführen!");
    }

    /**
     * @param player           The Person or Console the message is sent to
     * @param text             Text before Command
     * @param textAfterCommand Text After Command
     * @param command          The Command
     */
    @Deprecated
    public void sendCommandMessage(Player player, String text, String textAfterCommand, String command) {
        sendCommandMessage(player, text, command, textAfterCommand, "Klicke um %hc%" + command + " %bc%auszuführen!");
    }

    /**
     * @param player           The Person or Console the message is sent to
     * @param text             Text before Command
     * @param textAfterCommand Text After Command
     * @param hoverText        The hover Text
     * @param command          The Command
     */
    public void sendCommandMessage(Player player, String text, String command, String textAfterCommand, String hoverText) {
        sendCommandMessage(player, true, text, command, textAfterCommand, hoverText);
    }

    /**
     * @param player           The Person or Console the message is sent to
     * @param enablePrefix     Enables Prefix or not
     * @param text             Text before Command
     * @param textAfterCommand Text After Command
     * @param hoverText        The hover Text
     * @param command          The Command
     */
    public void sendCommandMessage(Player player, boolean enablePrefix, String text, String command, String textAfterCommand, String hoverText) {
        TextComponent before = new TextComponent(text);
        BaseComponent commandComponent = createExecuteMessage("%hc%" + command + "%bc%", hoverText, command);
        TextComponent after = new TextComponent(textAfterCommand);
        sendMessage(player, enablePrefix, before, commandComponent, after);
    }

    /**
     * @param players A Collection of Player the Message is sent to
     * @param message The Message you sent to the Players
     */
    public void broadcast(Collection<? extends Player> players, String message) {
        broadcast(players, this.enablePrefix, null, new TextComponent(message));
    }

    /**
     * @param players      A Collection of Player the Message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param message      The Message you sent to the Players
     */
    public void broadcast(Collection<? extends Player> players, boolean enablePrefix, String message) {
        broadcast(players, enablePrefix, null, new TextComponent(message));
    }

    /**
     * @param players    A Collection of Player the Message is sent to
     * @param component  The Message {@link net.md_5.bungee.api.chat.BaseComponent} sent to the Player Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void broadcast(Collection<? extends Player> players, BaseComponent component, BaseComponent... components) {
        for (BaseComponent baseComponent : components) {
            component.addExtra(baseComponent);
        }
        broadcast(players, this.enablePrefix, null, component);
    }

    /**
     * @param players      A Collection of Player the Message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param component    The Message {@link net.md_5.bungee.api.chat.BaseComponent} sent to the Player Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components   Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void broadcast(Collection<? extends Player> players, boolean enablePrefix, BaseComponent component, BaseComponent... components) {
        for (BaseComponent baseComponent : components) {
            component.addExtra(baseComponent);
        }
        broadcast(players, enablePrefix, null, component);
    }

    /**
     * @param players    A Collection of Player the Message is sent to
     * @param message    The Message Broadcasted to all Player which have the needed Permission
     * @param permission The Permission an Player needs to see this Message
     */
    public void broadcast(Collection<? extends Player> players, String message, String permission) {
        broadcast(players, this.enablePrefix, permission, new TextComponent(message));
    }

    /**
     * @param players      A Collection of Player the Message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param permission   The Permission an Player needs to see this Message
     * @param message      The Message Broadcasted to all Player which have the needed Permission
     */
    public void broadcast(Collection<? extends Player> players, boolean enablePrefix, String permission, String message) {
        broadcast(players, enablePrefix, permission, new TextComponent(message));
    }

    /**
     * @param players    A Collection of Player the Message is sent to
     * @param permission The Permission an Player needs to see this Message
     * @param component  The Message {@link net.md_5.bungee.api.chat.BaseComponent} sent to the Player Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void broadcast(Collection<? extends Player> players, String permission, BaseComponent component, BaseComponent... components) {
        for (BaseComponent baseComponent : components) {
            component.addExtra(baseComponent);
        }
        broadcast(players, this.enablePrefix, permission, component);
    }

    /**
     * @param players      A Collection of Player the Message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param permission   The Permission an Player needs to see this Message
     * @param component    The Message {@link net.md_5.bungee.api.chat.BaseComponent} sent to the Player Normally an {@link net.md_5.bungee.api.chat.TextComponent}
     * @param components   Additional {@link net.md_5.bungee.api.chat.BaseComponent} which are append with {@link net.md_5.bungee.api.chat.TextComponent#addExtra(BaseComponent)}
     */
    public void broadcast(Collection<? extends Player> players, boolean enablePrefix, String permission, BaseComponent component, BaseComponent... components) {
        for (BaseComponent baseComponent : components) {
            component.addExtra(baseComponent);
        }
        broadcast(players, enablePrefix, permission, component);
    }

    /**
     * @param players      A Collection of Player the Message is sent to
     * @param enablePrefix Determine if the Prefix should be Shown
     * @param permission   The Permission an Player needs to see this Message
     * @param message      No further description provided
     */
    public void broadcast(Collection<? extends Player> players, boolean enablePrefix, String permission, BaseComponent message) {
        TextComponent component = buildComponent(enablePrefix, message);

        players.forEach(player -> {
            if (permission != null && !player.hasPermission(permission)) {
                return;
            }
            player.spigot().sendMessage(component);
        });
    }

    /**
     * creates a BaseComponent which executes a command when clicking in it.
     * Also shows a message when hovering over it.
     *
     * @param content   the message
     * @param hoverText the message shown when hovering over it
     * @param command   the command which will be executed
     * @return the created BaseComponent
     */
    public BaseComponent createExecuteMessage(String content, String hoverText, String command) {
        return buildCommandMessage(content, hoverText, command, HoverEvent.Action.SHOW_TEXT, ClickEvent.Action.RUN_COMMAND);
    }

    /**
     * creates a BaseComponent which executes a command when clicking in it.
     * Also shows a message when hovering over it.
     *
     * @param content   the message
     * @param hoverText the message shown when hovering over it
     * @param url       the url which will be opened
     * @return the created BaseComponent
     */
    public BaseComponent createLinkMessage(String content, String hoverText, String url) {
        return createLinkMessage(false, content, hoverText, url);
    }

    /**
     * creates a BaseComponent which executes a command when clicking in it.
     * Also shows a message when hovering over it.
     *
     * @param shortURL  if true remove https and show only top-level-domain (e.g. https://myplayplanet.net/rules will be shortened to myplayplanet.net)
     * @param content   the message
     * @param hoverText the message shown when hovering over it
     * @param url       the url which will be opened
     * @return the created BaseComponent
     */
    public BaseComponent createLinkMessage(boolean shortURL, String content, String hoverText, String url) {
        if (shortURL) {
            content = url.replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "")
                    .split("/")[0] + " ";
        }
        return buildCommandMessage(content, hoverText, url, HoverEvent.Action.SHOW_TEXT, ClickEvent.Action.OPEN_URL);
    }

    /**
     * creates a BaseComponent which suggests a command when clicking in it.
     * Also shows a message when hovering over it.
     *
     * @param content   the message
     * @param hoverText the message shown when hovering over it
     * @param command   the command which will be suggested
     * @return the created BaseComponent
     */
    public BaseComponent createSuggestMessage(String content, String hoverText, String command) {
        return buildCommandMessage(content, hoverText, command, HoverEvent.Action.SHOW_TEXT, ClickEvent.Action.SUGGEST_COMMAND);
    }

    /**
     * @param content       the message
     * @param hoverText     the message shown when hovering over it
     * @param command       the command which will be suggested
     * @param hoverAction   the Action the message should be shown with
     * @param commandAction the Action to execute the Command
     * @return the created BaseComponent
     */
    private BaseComponent buildCommandMessage(String content, String hoverText, String command, HoverEvent.Action hoverAction, ClickEvent.Action commandAction) {
        BaseComponent message = new TextComponent(content);
//        message.setExtra(Arrays.asList(TextComponent.fromLegacyText(content)));
        message.setHoverEvent(new HoverEvent(hoverAction, TextComponent.fromLegacyText(hoverText)));
        message.setClickEvent(new ClickEvent(commandAction, command));
        return message;
    }

    /**
     * @param prefix  The Prefix set before the Message
     * @param message The Message you sent to the Player
     * @return {@link net.md_5.bungee.api.chat.TextComponent} which is replace with the {@link #baseColor} and {@link #highlightColor}
     */
    private TextComponent buildComponent(boolean prefix, BaseComponent message) {
        TextComponent component = new TextComponent();
        if (prefix) {
            component.addExtra(this.prefix + " ");
        }
        if (message.getExtra() == null || message.getExtra().isEmpty()) {
            component.setColor(this.baseColor);
            String text = formatText(message.toPlainText());
            if (text.contains("http")) {
                Pattern pattern = Pattern.compile("\\b(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher matcher = pattern.matcher(text);
                int previousEnd = 0;
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    String beforeLink = text.substring(previousEnd, start);
                    String link = text.substring(start, end);
                    component.addExtra(beforeLink);
                    component.addExtra(createLinkMessage(ChatColor.YELLOW + link, ChatColor.YELLOW + link, link));
                    previousEnd = end;
                }
                component.addExtra(text.substring(previousEnd));
            } else {
                TextComponent txt = new TextComponent(text);
                txt.setClickEvent(message.getClickEvent());
                if (message.getHoverEvent() != null) {
                    txt.setHoverEvent(buildHoverEvent(message.getHoverEvent()));
                }
                component.addExtra(txt);
            }

            /* format Hover Event Text */


        } else {

            /* format extras */

            if (message instanceof TextComponent) {
                component.addExtra(buildComponent(false, new TextComponent(((TextComponent) message).getText())));
            }
            for (BaseComponent component1 : message.getExtra()) {
                component.addExtra(buildComponent(false, component1));
            }

            /* set Events */

            if (message.getHoverEvent() != null) {
                component.setHoverEvent(buildHoverEvent(message.getHoverEvent()));
            }
            if (message.getClickEvent() != null) {
                component.setClickEvent(message.getClickEvent());
            }
        }

        return component;
    }

//    public BaseComponent

    private HoverEvent buildHoverEvent(HoverEvent hoverEvent) {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.color(this.baseColor);
        for (BaseComponent baseComponent : hoverEvent.getValue()) {
            componentBuilder.append(formatText(baseComponent.toPlainText()));
        }
        HoverEvent newHoverEvent = new HoverEvent(hoverEvent.getAction(), componentBuilder.create());
        return newHoverEvent;
    }

    /**
     * Format a text
     *
     * @param text the text to format
     * @return the formatted tecxt
     */
    public String formatText(String text) {
        return text.replace("%bc%", "" + this.getBaseColor())
                .replace("%hc%", "" + this.getHighlightColor())
                .replace("%ec%", "" + this.getErrorColor());
    }


    /**
     * Default {@link #prefix}
     *
     * @param prefix The new Prefix
     * @return No further description provided
     */
    public MessageManager setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Default {@link #baseColor}
     *
     * @param color The new Base Color
     * @return No further description provided
     */
    public MessageManager setBaseColor(ChatColor color) {
        this.baseColor = color;
        return this;
    }

    /**
     * Default {@link #highlightColor}
     *
     * @param color The new Highlight Color
     * @return No further description provided
     */
    public MessageManager setHighlightColor(ChatColor color) {
        this.highlightColor = color;
        return this;
    }

    /**
     * Default {@link #errorColor}
     *
     * @param color No further description provided
     * @return No further description provided
     */
    public MessageManager setErrorColor(ChatColor color) {
        this.errorColor = color;
        return this;
    }

    /**
     * Default {@link #upperTitleColor}
     *
     * @param color No further description provided
     * @return No further description provided
     */
    public MessageManager setUpperTitleColor(ChatColor color) {
        this.upperTitleColor = color;
        return this;
    }

    /**
     * Default {@link #lowerTitleColor}
     *
     * @param color No further description provided
     * @return No further description provided
     */
    public MessageManager setLowerTitleColor(ChatColor color) {
        this.lowerTitleColor = color;
        return this;
    }

    /**
     * Default {@link #titleFadeInLength}
     *
     * @param duration No further description provided
     * @return No further description provided
     */
    public MessageManager setTitleFadeInLength(int duration) {
        this.titleFadeInLength = duration;
        return this;
    }

    /**
     * Default {@link #titleFadeOutLength}
     *
     * @param duration No further description provided
     * @return No further description provided
     */
    public MessageManager setTitleFadeOutLength(int duration) {
        this.titleFadeOutLength = duration;
        return this;
    }

    /**
     * Default {@link #titleStayLength}
     *
     * @param duration No further description provided
     * @return No further description provided
     */
    public MessageManager setTitleStayLength(int duration) {
        this.titleStayLength = duration;
        return this;
    }

    /**
     * Default {@link #enablePrefix}
     *
     * @param enabled If prefix should be printed
     * @return No further description provided
     */
    public MessageManager setEnabledPrefix(boolean enabled) {
        this.enablePrefix = enabled;
        return this;
    }

    /**
     * @param player    The Person or Console the message is sent to
     * @param upperText The upper text of the title
     * @param lowerText The lower text of the title
     * @param fadeIn    The amount of ticks the Title needs to fade in
     * @param fadeOut   The amount of ticks the Title needs to fade out
     * @param stay      The amount of ticks the Title stays
     */
    public void sendTitle(Player player, String upperText, String lowerText, int fadeIn, int fadeOut, int stay) {
        player.sendTitle(this.upperTitleColor + upperText, this.lowerTitleColor + lowerText, fadeIn, stay, fadeOut);
    }

    /**
     * @param player    The Person or Console the message is sent to
     * @param upperText The upper text of the title
     * @param lowerText The lower text of the title
     */
    public void sendTitle(Player player, String upperText, String lowerText) {
        player.sendTitle(this.upperTitleColor + upperText, this.lowerTitleColor + lowerText, this.titleFadeInLength, this.titleStayLength, this.titleFadeOutLength);
    }
}
