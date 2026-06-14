package ruby.systems.commands;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import ruby.RubyClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CommandSuggestions {
    private CommandSuggestions() {}

    public static boolean isCommandInput(String text) {
        String prefix = RubyClient.chatPrefix.value();
        return !prefix.isEmpty() && text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static Suggestions wrap(String text, int cursor) {
        String prefix = RubyClient.chatPrefix.value();
        if (prefix.isEmpty() || cursor < prefix.length())
            return new Suggestions(StringRange.between(cursor, cursor), List.of());
        if (!text.regionMatches(true, 0, prefix, 0, prefix.length()))
            return new Suggestions(StringRange.between(cursor, cursor), List.of());

        int tokenStart = tokenStart(text, cursor, prefix.length());
        String partial = text.substring(tokenStart, cursor);
        StringRange range = StringRange.between(tokenStart, cursor);

        String afterPrefix = text.substring(prefix.length(), cursor);
        if (afterPrefix.startsWith(" ")) afterPrefix = afterPrefix.substring(1);

        int firstSpace = afterPrefix.indexOf(' ');
        List<Suggestion> suggestions = new ArrayList<>();

        if (firstSpace == -1) {
            for (Command command : Commands.getCommands()) {
                String name = command.name();
                if (partial.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT)))
                    suggestions.add(new Suggestion(range, name));
            }
        } else {
            String commandToken = afterPrefix.substring(0, firstSpace);
            Command command = Commands.getByName(commandToken);
            if (command == null) return new Suggestions(range, List.of());

            String argsPart = afterPrefix.substring(firstSpace + 1);
            int argIndex = argsPart.isEmpty() ? 0 : argsPart.split("\\s+", -1).length - 1;
            if (!partial.isEmpty() && argIndex > 0 && !argsPart.endsWith(" "))
                argIndex--;

            String[] args = argsPart.isEmpty() ? new String[0] : argsPart.split("\\s+");
            for (String value : command.suggest(args, Math.max(0, argIndex))) {
                if (partial.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT)))
                    suggestions.add(new Suggestion(range, value));
            }
        }

        suggestions.sort(Comparator.comparing(s -> s.getText().toLowerCase(Locale.ROOT)));
        if (suggestions.isEmpty()) return new Suggestions(range, List.of());
        return new Suggestions(range, suggestions);
    }

    private static int tokenStart(String text, int cursor, int prefixLength) {
        int start = cursor;
        int min = prefixLength;

        while (start > min && Character.isWhitespace(text.charAt(start - 1)))
            start--;

        while (start > min && !Character.isWhitespace(text.charAt(start - 1)))
            start--;

        return start;
    }
}
