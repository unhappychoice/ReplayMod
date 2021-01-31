package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import net.minecraft.resources.ResourcePack;
import net.minecraft.resources.ResourcePackFileNotFoundException;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 * Also duplicates `replaymod.input.*` bindings to `key.replaymod.*` as convention on Fabric.
 */
public class LangResourcePack extends ResourcePack {
    private static final Gson GSON = new Gson();
    public static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).lang$");

    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + ReplayMod.MOD_ID + ".%s";

    private final Path basePath;

    public LangResourcePack() {
        super(new File(NAME));

        this.basePath = null; // stub
    }

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.lang", matcher.group(1), matcher.group(2).toUpperCase());
    }

    private Path baseLangPath() {
        return basePath.resolve("assets").resolve(ReplayMod.MOD_ID).resolve("lang");
    }

    private Path langPath(String path) {
        String langName = langName(path);
        if (langName == null) return null;
        Path basePath = baseLangPath();
        return basePath.resolve(langName);
    }

    private String convertValue(String value) {
        return value;
    }

    @Override
    protected InputStream getInputStream(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return new ByteArrayInputStream("{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 4}}".getBytes(StandardCharsets.UTF_8));
        }

        Path langPath = langPath(path);
        if (langPath == null) throw new ResourcePackFileNotFoundException(this.file, path);

        List<String> langFile;
        try (InputStream in = Files.newInputStream(langPath)) {
            langFile = IOUtils.readLines(in, StandardCharsets.UTF_8);
        }

        Map<String, String> properties = new HashMap<>();
        for (String line : langFile) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int i = line.indexOf('=');
            String key = line.substring(0, i);
            String value = line.substring(i + 1);
            value = convertValue(value);
            if (key.startsWith(LEGACY_KEY_PREFIX)) {
                // Duplicating instead of just remapping as some other part of the UI may still rely on the old key
                properties.put(key, value);
                key = String.format(FABRIC_KEY_FORMAT, key.substring(LEGACY_KEY_PREFIX.length()));
            }
            properties.put(key, value);
        }

        return new ByteArrayInputStream(GSON.toJson(properties).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean resourceExists(String path) {
        Path langPath = langPath(path);
        return langPath != null && Files.exists(langPath);
    }


    @Override
    public Collection<ResourceLocation> getAllResourceLocations(
            ResourcePackType resourcePackType,
            String namespace,
            String path,
            int maxDepth,
            Predicate<String> filter
    ) {
        if (resourcePackType == ResourcePackType.CLIENT_RESOURCES && "lang".equals(path)) {
            Path base = baseLangPath();
            try {
                return Files.walk(base, 1)
                        .skip(1)
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName).map(Path::toString)
                        .map(LANG_FILE_NAME_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> String.format("%s_%s.json", matcher.group(1), matcher.group(1)))
                        .filter(filter::test)
                        .map(name -> new ResourceLocation(ReplayMod.MOD_ID, "lang/" + name))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getResourceNamespaces(ResourcePackType resourcePackType) {
        if (resourcePackType == ResourcePackType.CLIENT_RESOURCES) {
            return Collections.singleton("replaymod");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {
    }

    // Not needed on fabric, using MixinModResourcePackUtil instead.
}
