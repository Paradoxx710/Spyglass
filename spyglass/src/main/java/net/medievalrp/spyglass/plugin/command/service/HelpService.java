package net.medievalrp.spyglass.plugin.command.service;

import net.kyori.adventure.text.Component;
import net.medievalrp.spyglass.plugin.command.render.Messages;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class HelpService {

    private static final int PAGE_SIZE = 8;

    public void send(CommandSender sender) {
        send(sender, 1);
    }

    public void send(CommandSender sender, int page) {
        Messages messages = Messages.getInstance();
        java.util.List<String> entries = messages.rawList("help.commands");
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clamped = Math.min(Math.max(page, 1), pages);
        Component header = messages.format("help.header", sender,
                "%page%", clamped, "%pages%", pages);
        if (clamped > 1) {
            header = header.append(messages.format("help.nav-separator", sender))
                    .append(navButton(messages, sender, "←", clamped - 1));
        }
        if (clamped < pages) {
            header = header.append(messages.format("help.nav-separator", sender))
                    .append(navButton(messages, sender, "→", clamped + 1));
        }
        sender.sendMessage(header);
        sender.sendMessage(messages.get("help.subtitle", sender));
        int from = (clamped - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, entries.size());
        for (int index = from; index < to; index++) {
            String name = entries.get(index);
            if (messages.hasListTemplate("help.commands")) {
                String key = "help.entries." + name;
                sender.sendMessage(messages.format("help.entry", sender,
                        "%name%", name,
                        "%usage%", messages.raw(key + ".usage"),
                        "%description%", messages.raw(key + ".description")));
            } else {
                sender.sendMessage(messages.get("help.commands." + name, sender));
            }
        }
    }

    private static Component navButton(Messages messages, CommandSender sender,
                                       String arrow, int targetPage) {
        Component label = messages.format("help.nav", sender, "%arrow%", arrow);
        return label
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                        "/spyglass help " + targetPage))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        messages.format("help.nav-hover", sender, "%page%", targetPage)));
    }
}
