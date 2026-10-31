package crazycraft.loader;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A game folder's update retires what the pack replaces: a jar matching the manifest's retire patterns (the
 * CrazyCraft Controls prototype, now inside OreSpawn Integrations) and a second copy of a pack mod under another file
 * name. The pack's own copy and the player's unrelated mods stay.
 */
public final class RetireTest {
    public static void main(String[] args) throws Exception {
        Path game = Files.createTempDirectory("crazycraft-retire-");
        Path mods = Files.createDirectories(game.resolve("mods"));
        Path packControlify = jar(mods.resolve("controlify-3.0.1+lts+1.21.1-neoforge.jar"), "controlify");
        jar(mods.resolve("controlify-handheld-copy.jar"), "controlify");
        jar(mods.resolve("cccontrols-0.1.0-proto.jar"), "cccontrols");
        jar(mods.resolve("my-own-mod.jar"), "my_own_mod");
        Manifest manifest = new Manifest("Test", "2.0.0", ClientSetup.REPO, "1.21.1",
                new Manifest.NeoForge("21.1.252", null, null),
                List.of(new Manifest.Mod(packControlify.getFileName().toString(), "mods", "Controlify", "3.0.1", "client",
                        Files.size(packControlify), Hashes.of(packControlify, "SHA-512"),
                        new Manifest.Source("manual", null, null, null, null), "LGPL-3.0", null, List.of())),
                List.of(), List.of("cccontrols-*-proto.jar"));
        Installer.Result result = new Installer(game, manifest, ConsoleUi.QUIET, Installer.Mode.CLIENT).run();
        check(result == Installer.Result.READY, "update completes (" + result + ")");
        check(Files.exists(packControlify), "the pack's Controlify stays");
        check(Files.exists(mods.resolve("my-own-mod.jar")), "a player's own mod stays");
        check(!Files.exists(mods.resolve("cccontrols-0.1.0-proto.jar")), "the prototype is retired");
        check(!Files.exists(mods.resolve("controlify-handheld-copy.jar")), "the duplicate Controlify is retired");
        try (var removed = Files.walk(game.resolve("mods-removed"))) {
            List<String> names = removed.map(p -> p.getFileName().toString()).toList();
            check(names.contains("cccontrols-0.1.0-proto.jar") && names.contains("controlify-handheld-copy.jar"),
                    "both are kept in mods-removed/, not deleted");
        }
        System.out.println("Retire checks passed: the prototype and a duplicate pack mod move aside, the rest stays.");
    }

    private static Path jar(Path path, String modId) throws Exception {
        try (OutputStream out = Files.newOutputStream(path); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zip.write(("modLoader=\"javafml\"\n[[mods]]\nmodId=\"" + modId + "\"\nversion=\"1\"\n")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return path;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
