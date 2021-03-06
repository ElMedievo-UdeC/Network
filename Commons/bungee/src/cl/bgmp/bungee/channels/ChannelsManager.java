package cl.bgmp.bungee.channels;

import cl.bgmp.bungee.ChatConstant;
import cl.bgmp.bungee.CommonsBungee;
import cl.bgmp.bungee.ComponentWrapper;
import cl.bgmp.minecraft.util.commands.CommandContext;
import java.util.HashSet;
import java.util.Set;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Represents a manager for all {@link Channel}s, handling the players using them, commands, etc.
 */
public final class ChannelsManager implements Listener {
  private CommonsBungee commonsBungee;
  private Set<Channel> channels = new HashSet<>();

  public ChannelsManager(CommonsBungee commonsBungee) {
    this.commonsBungee = commonsBungee;
    this.commonsBungee.registerEvent(this);
  }

  public void registerChannel(Channel channel) {
    this.channels.add(channel);
  }

  /**
   * Switch the chat {@link Channel} of a player to another
   *
   * @param player The player whose chat {@link Channel} will be changed
   * @param to The {@link Channel} to which the player will be changed to
   */
  public void switchChannelFor(ProxiedPlayer player, Channel to) {
    if (getChannelOf(player) != null) getChannelOf(player).disableFor(player);
    to.enableFor(player);
  }

  /**
   * Checks for the chat {@link Channel} a player is in
   *
   * @param player Player in question
   * @return The chat {@link Channel} the provided player is using, or null of not using any.
   */
  public Channel getChannelOf(ProxiedPlayer player) {
    return channels.stream()
        .filter(channel -> channel.isEnabledFor(player))
        .findFirst()
        .orElse(null);
  }

  public Channel getChannelByName(ChannelName name) {
    return channels.stream().filter(channel -> channel.getName() == name).findFirst().orElse(null);
  }

  public void evalChannelCommand(
      CommandContext args, CommandSender sender, Channel channel, Channel global) {

    final ProxiedPlayer player = (ProxiedPlayer) sender;
    if (args.argsLength() == 0) {
      if (channel.isEnabledFor(player)) {
        player.sendMessage(
            new ComponentWrapper(ChatConstant.CHAT_ALREADY_SET_TO.getAsString())
                .color(ChatColor.WHITE)
                .append(channel.getName().getLiteral())
                .color(ChatColor.GOLD)
                .build());
      } else {
        this.switchChannelFor(player, channel);
        player.sendMessage(
            new ComponentWrapper(ChatConstant.CHAT_MODE_SWITCH.getAsString())
                .color(ChatColor.WHITE)
                .append(channel.getName().getLiteral())
                .color(ChatColor.GOLD)
                .build());
      }
    } else {
      if (!(channel instanceof EveryoneChannel))
        channel.sendChannelMessage(player, args.getJoinedStrings(0));
      else {
        this.switchChannelFor(player, global);
        player.sendMessage(
            new ComponentWrapper(ChatConstant.CHAT_MODE_SWITCH.getAsString())
                .color(ChatColor.WHITE)
                .append(global.getName().getLiteral())
                .color(ChatColor.GREEN)
                .build());
      }
    }
  }

  @EventHandler
  public void onPlayerChat(ChatEvent event) {
    if (event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) return;

    final ProxiedPlayer player = (ProxiedPlayer) event.getSender();
    final Channel channel = getChannelOf(player);
    if (channel instanceof EveryoneChannel) return;

    event.setCancelled(true);

    channel.sendChannelMessage(player, event.getMessage());
  }

  @EventHandler
  public void onPlayerJoin(ServerSwitchEvent event) {
    if (this.getChannelOf(event.getPlayer()) == null)
      this.switchChannelFor(event.getPlayer(), this.getChannelByName(ChannelName.EVERYONE));
  }
}
