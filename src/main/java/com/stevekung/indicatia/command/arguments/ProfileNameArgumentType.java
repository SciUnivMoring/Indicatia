package com.stevekung.indicatia.command.arguments;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.stevekung.indicatia.config.ExtendedConfig;
import com.stevekung.stevekungslib.utils.LangUtils;
import com.stevekung.stevekungslib.utils.client.command.IClientSuggestionProvider;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.StringTextComponent;

public class ProfileNameArgumentType implements ArgumentType<String>
{
    private static final DynamicCommandExceptionType PROFILE_NOT_FOUND = new DynamicCommandExceptionType(obj -> new StringTextComponent(LangUtils.translate("commands.inprofile.not_found", obj)));
    private static final DynamicCommandExceptionType CANNOT_REMOVE_DEFAULT = new DynamicCommandExceptionType(obj -> new StringTextComponent(LangUtils.translate("commands.inprofile.cannot_remove_default")));
    private static final SimpleCommandExceptionType INVALID_ARGS = new SimpleCommandExceptionType(new StringTextComponent(LangUtils.translate("argument.id.invalid")));
    private final Mode mode;

    private ProfileNameArgumentType(Mode mode)
    {
        this.mode = mode;
    }

    public static ProfileNameArgumentType create()
    {
        return ProfileNameArgumentType.create(Mode.NONE);
    }

    public static ProfileNameArgumentType create(Mode mode)
    {
        return new ProfileNameArgumentType(mode);
    }

    public static String getProfile(CommandContext<IClientSuggestionProvider> context, String name)
    {
        return context.getArgument(name, String.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        ArrayList<File> files = new ArrayList<>(Arrays.asList(ExtendedConfig.USER_DIR.listFiles()));
        List<String> resList = files.stream().filter(file -> this.mode == Mode.REMOVE ? file.getName().endsWith(".dat") && !file.getName().equals("default.dat") : file.getName().endsWith(".dat")).map(file -> file.getName().replace(".dat", "")).collect(Collectors.toList());
        return ProfileNameArgumentType.suggestIterable(resList, builder);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException
    {
        String fileName = ProfileNameArgumentType.read(reader);
        boolean exist = false;
        boolean remove = false;

        if (ExtendedConfig.USER_DIR.exists())
        {
            for (File file : ExtendedConfig.USER_DIR.listFiles())
            {
                String name = file.getName();

                if (name.equals(fileName + ".dat") && name.endsWith(".dat"))
                {
                    exist = true;
                }
                if (this.mode == Mode.REMOVE && fileName.equals("default") && name.equals(fileName + ".dat"))
                {
                    remove = true;
                }
            }
        }

        if (remove)
        {
            throw ProfileNameArgumentType.CANNOT_REMOVE_DEFAULT.create(fileName);
        }

        if (exist)
        {
            return fileName;
        }
        else
        {
            throw ProfileNameArgumentType.PROFILE_NOT_FOUND.create(fileName);
        }
    }

    @Override
    public Collection<String> getExamples()
    {
        return Collections.singletonList("profile");
    }

    private static CompletableFuture<Suggestions> suggestIterable(Iterable<String> iterable, SuggestionsBuilder builder)
    {
        String typedString = builder.getRemaining().toLowerCase(Locale.ROOT);
        ProfileNameArgumentType.applySuggest(iterable, typedString, string1 -> string1, builder::suggest);
        return builder.buildFuture();
    }

    private static void applySuggest(Iterable<String> iterable, String typedString, Function<String, String> function, Consumer<String> consumer)
    {
        for (String name : iterable)
        {
            String name2 = function.apply(name);

            if (name2.startsWith(typedString))
            {
                consumer.accept(name);
            }
        }
    }

    private static String read(StringReader reader) throws CommandSyntaxException
    {
        int cursor = reader.getCursor();

        while (reader.canRead() && ResourceLocation.isValidPathCharacter(reader.peek()))
        {
            reader.skip();
        }

        String string = reader.getString().substring(cursor, reader.getCursor());

        try
        {
            return string;
        }
        catch (ResourceLocationException e)
        {
            reader.setCursor(cursor);
            throw ProfileNameArgumentType.INVALID_ARGS.createWithContext(reader);
        }
    }

    public enum Mode
    {
        NONE,
        REMOVE
    }
}