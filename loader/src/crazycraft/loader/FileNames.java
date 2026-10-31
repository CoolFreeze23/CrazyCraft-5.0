package crazycraft.loader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * File names from the manifest, made safe for the system they land on. A Linux or SteamOS session started with a
 * plain C locale (Steam's Gaming Mode does this) can't write names with accents or Minecraft's § colour codes, and
 * Java then refuses the whole path. Such a file is saved under a plain ASCII version of its name instead, so one odd
 * name never stops the install. The game reads resource packs and mods by their contents, not their file names.
 */
final class FileNames {

    private FileNames() {
    }

    /** {@code dir/name}, or {@code dir/plain(name)} when this system can't represent {@code name}. */
    static Path resolve(Path dir, String name) {
        try {
            return dir.resolve(name);
        } catch (InvalidPathException e) {
            return dir.resolve(plain(name));
        }
    }

    /** True when {@code name} can be used as it is under {@code dir}. */
    static boolean representable(Path dir, String name) {
        try {
            dir.resolve(name);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Every file in {@code dir} that is {@code name}, whether it was saved under that name or under {@link #plain}.
     * A file written under its real name by a UTF-8 session can't be named from a C-locale session, so the folder's
     * entries are compared the way Java reads them here: the name's UTF-8 bytes decoded with this system's file-name
     * encoding. Unreadable folders give an empty list.
     */
    static List<Path> existing(Path dir, String name) {
        Set<String> wanted = new HashSet<>(List.of(name, plain(name), asListed(name)));
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path p : entries) {
                if (wanted.contains(p.getFileName().toString()) && Files.isRegularFile(p)) {
                    out.add(p);
                }
            }
        } catch (IOException | RuntimeException e) {
            // a missing or unreadable folder holds nothing to find
        }
        return out;
    }

    /** How a directory listing shows a file whose real (UTF-8) name is {@code name}, on this system. */
    static String asListed(String name) {
        String jnu = System.getProperty("sun.jnu.encoding");
        try {
            return jnu == null ? name : new String(name.getBytes(StandardCharsets.UTF_8), Charset.forName(jnu));
        } catch (IllegalArgumentException e) {
            return name;
        }
    }

    /**
     * The name with § colour codes removed, accents taken off their letters and anything else outside printable
     * ASCII (or unsafe in a file name) turned into an underscore. "§c1.21.2 Redstone Torch§r.zip" becomes
     * "1.21.2 Redstone Torch.zip"; "Pão de Queijo.zip" becomes "Pao de Queijo.zip".
     */
    static String plain(String name) {
        String noCodes = name.replaceAll("§.", "");
        String noAccents = Normalizer.normalize(noCodes, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder out = new StringBuilder(noAccents.length());
        for (char c : noAccents.toCharArray()) {
            out.append(c >= 0x20 && c < 0x7F && "\\/:*?\"<>|".indexOf(c) < 0 ? c : '_');
        }
        String result = out.toString().strip();
        return result.isEmpty() || result.startsWith(".") ? "file" + result : result;
    }
}
