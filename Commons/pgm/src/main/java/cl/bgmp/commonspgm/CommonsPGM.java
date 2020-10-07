package cl.bgmp.commonspgm;

import cl.bgmp.bukkit.util.BukkitCommandsManager;
import cl.bgmp.bukkit.util.CommandsManagerRegistration;
import cl.bgmp.butils.bungee.Bungee;
import cl.bgmp.commonspgm.commands.CommonsCommand;
import cl.bgmp.commonspgm.injection.CommonsPGMModule;
import cl.bgmp.commonspgm.modules.TipsModule;
import cl.bgmp.commonspgm.modules.manager.ModuleManager;
import cl.bgmp.commonspgm.modules.manager.ModuleManagerImpl;
import cl.bgmp.commonspgm.modules.navigator.NavigatorModule;
import cl.bgmp.commonspgm.translations.AllTranslations;
import cl.bgmp.minecraft.util.commands.CommandsManager;
import cl.bgmp.minecraft.util.commands.annotations.TabCompletion;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import cl.bgmp.minecraft.util.commands.exceptions.CommandPermissionsException;
import cl.bgmp.minecraft.util.commands.exceptions.CommandUsageException;
import cl.bgmp.minecraft.util.commands.exceptions.MissingNestedCommandException;
import cl.bgmp.minecraft.util.commands.exceptions.ScopeMismatchException;
import cl.bgmp.minecraft.util.commands.exceptions.WrappedCommandException;
import cl.bgmp.minecraft.util.commands.injection.SimpleInjector;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommonsPGM extends JavaPlugin {
  private CommandsManager commandsManager = new BukkitCommandsManager();

  private Bungee bungee;
  private CommonsPGMConfig config;
  private AllTranslations translations;
  private ModuleManagerImpl moduleManager;

  public ModuleManager getModuleManager() {
    return this.moduleManager;
  }

  public Bungee getBungee() {
    return bungee;
  }

  @Inject private TipsModule tipsModule;
  @Inject private NavigatorModule navigatorModule;

  @Override
  public void onEnable() {
    this.loadConfig();
    this.translations = new AllTranslations();
    this.moduleManager = new ModuleManagerImpl();

    this.bungee = new Bungee(this);
    this.bungee.registerOutgoing();

    this.inject();

    this.moduleManager.registerModule(this.tipsModule);
    this.moduleManager.registerModule(this.navigatorModule);
    this.moduleManager.loadModules();

    this.registerCommands();
  }

  private void inject() {
    final CommonsPGMModule module =
        new CommonsPGMModule(this, this.config, this.translations, this.moduleManager);
    final Injector injector = module.createInjector();

    injector.injectMembers(this);
    injector.injectMembers(this.config);
    injector.injectMembers(this.translations);
    injector.injectMembers(this.moduleManager);
  }

  private void registerCommands() {
    this.registerCommand(CommonsCommand.class, this, this.moduleManager, this.translations);
  }

  public void registerEvent(Listener listener) {
    PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(listener, this);
  }

  public void unregisterEvent(Listener listener) {
    HandlerList.unregisterAll(listener);
  }

  private void loadConfig() {
    this.saveDefaultConfig();
    this.reloadConfig();
    if (config != null) return;

    this.getServer().getPluginManager().disablePlugin(this);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    try {
      this.config = new CommonsPGMConfig(getConfig(), getDataFolder());
    } catch (RuntimeException e) {
      e.printStackTrace();
      this.getLogger()
          .severe(
              translations.get("misc.configuration.load.failed", getServer().getConsoleSender()));
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      this.commandsManager.execute(command.getName(), args, sender, sender);
    } catch (ScopeMismatchException exception) {
      String[] scopes = exception.getScopes();
      if (!Arrays.asList(scopes).contains("player")) {
        sender.sendMessage(ChatColor.RED + translations.get("commands.no.player", sender));
      } else {
        sender.sendMessage(ChatColor.RED + translations.get("commands.no.console", sender));
      }
    } catch (CommandPermissionsException exception) {
      sender.sendMessage(ChatColor.RED + translations.get("commands.no.permission", sender));
    } catch (MissingNestedCommandException exception) {
      sender.sendMessage(
          ChatColor.RED + translations.get("commands.syntax.error", sender, exception.getUsage()));
    } catch (CommandUsageException exception) {
      sender.sendMessage(ChatColor.RED + exception.getMessage());
      sender.sendMessage(ChatColor.RED + exception.getUsage());
    } catch (WrappedCommandException exception) {
      if (exception.getCause() instanceof NumberFormatException) {
        sender.sendMessage(ChatColor.RED + translations.get("commands.number.string", sender));
      } else {
        sender.sendMessage(translations.get("commands.unknown.error", sender));
        exception.printStackTrace();
      }
    } catch (CommandException exception) {
      sender.sendMessage(ChatColor.RED + exception.getMessage());
    }
    return true;
  }

  private void registerCommand(Class<?> clazz, Object... toInject) {
    if (toInject.length > 0) this.commandsManager.setInjector(new SimpleInjector(toInject));
    else this.commandsManager.setInjector(null);
    CommandsManagerRegistration defaultRegistration =
        new CommandsManagerRegistration(this, this.commandsManager);

    final Class<?>[] subclasses = clazz.getClasses();

    if (subclasses.length == 0) defaultRegistration.register(clazz);
    else {
      TabCompleter tabCompleter = null;
      Class<?> nestNode = null;
      for (Class<?> subclass : subclasses) {
        if (subclass.isAnnotationPresent(TabCompletion.class)) {
          try {
            tabCompleter = (TabCompleter) subclass.newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
          }
        } else nestNode = subclass;
      }
      if (tabCompleter == null) defaultRegistration.register(subclasses[0]);
      else {
        CommandsManagerRegistration customRegistration =
            new CommandsManagerRegistration(this, this, tabCompleter, commandsManager);
        if (subclasses.length == 1) customRegistration.register(clazz);
        else customRegistration.register(nestNode);
      }
    }
  }
}
