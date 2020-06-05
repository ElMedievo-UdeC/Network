package cl.bgmp.bungee.commands.privatemessage;

import cl.bgmp.bungee.BungeeMessages;
import cl.bgmp.bungee.ChatConstant;
import cl.bgmp.bungee.CommonsBungee;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PrivateMessageCommands {
  @Command(
      aliases = {"message", "msg", "pm"},
      desc = "Send a private message to another player.",
      usage = "<player> <msg>",
      min = 2)
  @CommandPermissions("commons.bungee.command.message")
  public static void message(final CommandContext args, CommandSender sender) {
    if (!(sender instanceof ProxiedPlayer)) {
      sender.sendMessage(
          BungeeMessages.colourify(ChatColor.RED, ChatConstant.NO_CONSOLE.getAsTextComponent()));
      return;
    }

    final ProxiedPlayer msgReceiver = CommonsBungee.get().getProxy().getPlayer(args.getString(0));
    if (msgReceiver == null) {
      sender.sendMessage(
          BungeeMessages.colourify(
              ChatColor.RED, ChatConstant.PLAYER_NOT_FOUND.getAsTextComponent()));
      return;
    }

    final ProxiedPlayer msgSender = (ProxiedPlayer) sender;
    final String message = args.getJoinedStrings(1);

    PrivateMessagesManager.sendMsg(msgSender, msgReceiver, message);
  }

  @Command(
      aliases = {"reply", "r"},
      desc = "Reply to the last message received.",
      usage = "<msg>",
      min = 1)
  @CommandPermissions("commons.bungee.command.reply")
  public static void reply(final CommandContext args, CommandSender sender) {
    if (!(sender instanceof ProxiedPlayer)) {
      sender.sendMessage(
          BungeeMessages.colourify(ChatColor.RED, ChatConstant.NO_CONSOLE.getAsTextComponent()));
      return;
    }

    PrivateMessagesManager.sendReply((ProxiedPlayer) sender, args.getJoinedStrings(0));
  }
}