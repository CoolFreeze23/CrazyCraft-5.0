package crazycraft.loader;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;

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
