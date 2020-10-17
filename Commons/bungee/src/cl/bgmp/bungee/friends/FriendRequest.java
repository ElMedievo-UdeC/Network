package cl.bgmp.bungee.friends;

import cl.bgmp.bungee.APIBungee;
import cl.bgmp.bungee.CommonsBungee;
import cl.bgmp.bungee.users.user.LinkedUser;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class FriendRequest {
  private final Duration expiration = Duration.ofMinutes(2);

  private final CommonsBungee commonsBungee;
  private final APIBungee api;
  private final LinkedUser sender;
  private final LinkedUser receiver;
  private boolean expired = false;

  public FriendRequest(
      CommonsBungee commonsBungee, APIBungee api, LinkedUser sender, LinkedUser receiver) {
    this.commonsBungee = commonsBungee;
    this.sender = sender;
    this.receiver = receiver;
    this.api = api;

    this.commonsBungee
        .getProxy()
        .getScheduler()
        .schedule(this.commonsBungee, this::expire, expiration.getSeconds(), TimeUnit.SECONDS);
  }

  public LinkedUser getSender() {
    return sender;
  }

  public LinkedUser getReceiver() {
    return receiver;
  }

  public boolean isExpired() {
    return expired;
  }

  public void expire() {
    this.expired = true;

    final ProxiedPlayer senderPlayer =
        this.commonsBungee.getProxy().getPlayer(this.sender.getUUID());
    final ProxiedPlayer receiverPlayer =
        this.commonsBungee.getProxy().getPlayer(this.receiver.getUUID());
    if (senderPlayer == null || receiverPlayer == null) return;

    senderPlayer.sendMessage(
        ChatColor.RED
            + "Your friend request to "
            + ChatColor.AQUA
            + receiverPlayer.getName()
            + ChatColor.RED
            + " has expired.");
    receiverPlayer.sendMessage(
        ChatColor.RED
            + "Your friend request from "
            + ChatColor.AQUA
            + senderPlayer.getName()
            + ChatColor.RED
            + " has expired.");
  }

  public void accept() {
    this.expired = true;

    sender.addFriend(receiver);
    receiver.addFriend(sender);
  }

  public void deny() {
    this.expired = true;
  }
}