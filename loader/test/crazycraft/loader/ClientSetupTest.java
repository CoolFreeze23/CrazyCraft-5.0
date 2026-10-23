package crazycraft.loader;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Filesystem regression tests use disposable launcher folders, never a player's installation. */
public final class ClientSetupTest {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void put(Path root, String relative, String text) throws Exception {
        ClientSetup.write(root.resolve(relative), text);
    }
    static void jar(Path path, String text) throws Exception {
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zip.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
        }
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("crazycraft-setup-tests-");
        Path payload = root.resolve("payload");
        String manifest = """
                {"format":1,"pack":{"name":"CrazyCraft","version":"1.4.0","repo":"CoolFreeze23/CrazyCraft-5.0","minecraft":"1.21.1"},
                 "neoforge":{"version":"21.1.252","installer":"https://example.invalid/","sha1":"test"},"mods":[],"extract":[]}
                """;
        put(payload, ".minecraft/crazycraft-manifest.json", manifest);
        put(payload, ".minecraft/options.txt", "default options");
        put(payload, ".minecraft/config/pack.txt", "new config");
        put(payload, "instance.cfg", "[General]\nname=CrazyCraft 5.0\nMaxMemAlloc=8192\n");
        put(payload, "mmc-pack.json", "{\"formatVersion\":1,\"components\":[{\"uid\":\"net.neoforged\",\"version\":\"21.1.252\"}]}");
        var release = new ClientSetup.Release("v1.4.0", payload, Manifest.parse(manifest));
        Path prism = root.resolve("Prism portable");
        put(prism, "prismlauncher.cfg", "InstanceDir=custom instances\n");
        var fresh = ClientSetup.newInstance(ClientSetup.Launcher.PRISM, prism, "CrazyCraft Test");
        ClientSetup.apply(release, ClientSetup.Launcher.PRISM, fresh, false);
        check(fresh.root().startsWith(prism.resolve("custom instances")), "custom Prism directory");
        check(ClientSetup.discover(ClientSetup.Launcher.PRISM, prism).size() == 1, "detect fresh instance");
        put(fresh.game(), "saves/world/level.dat", "world");
        put(fresh.game(), "servers.dat", "servers");
        put(fresh.game(), "options.txt", "player options");
        put(fresh.game(), "config/pack.txt", "old config");
        put(fresh.game(), "mods/personal.jar", "personal");
        put(fresh.root(), "instance.cfg", "[General]\nname=My game\nMaxMemAlloc=12288\nJavaPath=custom-java\n");
        put(fresh.root(), "mmc-pack.json", "{\"components\":[{\"uid\":\"net.neoforged\",\"version\":\"21.1.248\"},{\"uid\":\"custom\",\"version\":\"1\"}]}");
        Path backup = ClientSetup.apply(release, ClientSetup.Launcher.PRISM, fresh, true);
        check(Files.readString(fresh.game().resolve("options.txt")).equals("player options"), "preserve options");
        check(Files.readString(fresh.game().resolve("saves/world/level.dat")).equals("world"), "preserve saves");
        check(Files.readString(fresh.game().resolve("servers.dat")).equals("servers"), "preserve servers");
        check(Files.readString(fresh.root().resolve("instance.cfg")).contains("12288"), "preserve launcher memory");
        check(Files.readString(backup.resolve("game/config/pack.txt")).equals("old config"), "backup replaced config");
        String components = Files.readString(fresh.root().resolve("mmc-pack.json"));
        check(components.contains("21.1.252") && !components.contains("21.1.248") && components.contains("custom"), "update component pins");
        // Simulate an old import that has a manifest but no download ownership state.
        Map<String, Object> state = ClientSetup.readObject(fresh.game().resolve(".crazycraft/state.json"));
        Json.obj(state.get("installed")).put("mods/obsolete.jar", Map.of());
        ClientSetup.write(fresh.game().resolve(".crazycraft/state.json"), Json.write(state));
        put(fresh.game(), "mods/obsolete.jar", "obsolete");
        var result = new Installer(fresh.game(), release.manifest(), ConsoleUi.QUIET, Installer.Mode.CLIENT).run();
        check(result == Installer.Result.READY, "installer completes");
        check(!Files.exists(fresh.game().resolve("mods/obsolete.jar")), "retire old pack mod");
        check(Files.exists(fresh.game().resolve("mods/personal.jar")), "retain personal mod");
        check(Files.isDirectory(fresh.game().resolve("mods-removed")), "retired mods are recoverable");
        // Migrate an older manually maintained pack, keeping unrelated mods even when they depend on a pack mod.
        Files.delete(fresh.game().resolve("crazycraft-manifest.json"));
        put(fresh.game(), "resourcepacks/CrazyCraft5-ptBR.zip", "legacy marker");
        put(fresh.game(), "mods/orespawn_integrations-old.jar", "legacy marker");
        jar(fresh.game().resolve("mods/example-old.jar"), "[[mods]] # mandatory\nmodId='example'\n");
        jar(fresh.game().resolve("mods/unrelated.jar"), "[[mods]]\nmodId='personal'\n[[dependencies.personal]]\nmodId='example'\n");
        check(ClientSetup.discover(ClientSetup.Launcher.PRISM, prism).size() == 1, "discover legacy installation");
        ClientSetup.apply(release, ClientSetup.Launcher.PRISM, fresh, true);
        Path newJar = fresh.game().resolve("mods/example-new.jar");
        jar(newJar, "[[mods]]\nmodId='example'\n");
        Manifest legacyManifest = new Manifest("Test", "1", ClientSetup.REPO, "1.21.1", release.manifest().neoforge(),
                List.of(new Manifest.Mod("example-new.jar", "mods", "Example", "2", "both", Files.size(newJar), Hashes.of(newJar, "SHA-512"),
                        new Manifest.Source("manual", null, null, null, null), "MIT", null)), List.of());
        check(new Installer(fresh.game(), legacyManifest, ConsoleUi.QUIET, Installer.Mode.CLIENT).run() == Installer.Result.READY, "legacy update completes");
        check(!Files.exists(fresh.game().resolve("mods/example-old.jar")), "legacy old version retired");
        check(Files.exists(fresh.game().resolve("mods/unrelated.jar")), "dependency IDs do not remove personal mods");
        try { ClientSetup.newInstance(ClientSetup.Launcher.PRISM, prism, "CrazyCraft Test"); throw new AssertionError("collision allowed"); }
        catch (java.io.IOException expected) { }
        try { ClientSetup.safe(root, "../escape"); throw new AssertionError("path traversal allowed"); }
        catch (java.io.IOException expected) { }
        Path malicious = root.resolve("bad.zip");
        try (var z = new ZipOutputStream(Files.newOutputStream(malicious))) {
            z.putNextEntry(new ZipEntry("../escaped.txt")); z.write(1); z.closeEntry();
        }
        try { ClientSetup.unzip(malicious, root.resolve("unpacked")); throw new AssertionError("zip traversal allowed"); }
        catch (java.io.IOException expected) { }
        for (String profilesName : List.of("launcher_profiles.json", "launcher_profiles_microsoft_store.json")) {
            Path nativeRoot = root.resolve(profilesName.replace(".json", ""));
            put(nativeRoot, profilesName, "{\"profiles\":{\"vanilla\":{\"name\":\"Vanilla\"}},\"settings\":{\"keep\":true}}");
            var instance = ClientSetup.newInstance(ClientSetup.Launcher.MINECRAFT, nativeRoot, "CrazyCraft");
            ClientSetup.apply(release, ClientSetup.Launcher.MINECRAFT, instance, false);
            check(ClientSetup.discover(ClientSetup.Launcher.MINECRAFT, nativeRoot).size() == 1, "recover interrupted native install");
            ClientSetup.registerNative(nativeRoot, instance, "neoforge-21.1.252");
            ClientSetup.registerNative(nativeRoot, instance, "neoforge-21.1.253");
            Map<String, Object> data = ClientSetup.readObject(nativeRoot.resolve(profilesName));
            Map<String, Object> profiles = Json.obj(data.get("profiles"));
            check(profiles.size() == 2 && profiles.containsKey("vanilla"), "preserve vanilla, update profile in place");
            check(data.containsKey("settings"), "preserve launcher settings");
            check(ClientSetup.discover(ClientSetup.Launcher.MINECRAFT, nativeRoot).size() == 1, "detect native profile once");
        }
        if (args.length > 0) {
            var latest = ClientSetup.latest(System.out::println);
            var instance = ClientSetup.newInstance(ClientSetup.Launcher.PRISM, prism, "Published release");
            ClientSetup.apply(latest, ClientSetup.Launcher.PRISM, instance, false);
            check(ClientSetup.isPack(instance.game()), "published client payload installs");
            check(latest.manifest().mods().size() > 100, "published manifest parsed");
            if (args[0].equals("--native")) {
                Path nativeRoot = Path.of(args[1]).toAbsolutePath();
                var before = ClientSetup.readObject(ClientSetup.profilesFile(nativeRoot));
                var nativeInstance = ClientSetup.newInstance(ClientSetup.Launcher.MINECRAFT, nativeRoot, "Setup test " + System.currentTimeMillis());
                ClientSetup.apply(latest, ClientSetup.Launcher.MINECRAFT, nativeInstance, false);
                ClientSetup.installNative(latest, nativeRoot, nativeInstance, System.out::println);
                var after = ClientSetup.readObject(ClientSetup.profilesFile(nativeRoot));
                for (var entry : Json.obj(before.get("profiles")).entrySet())
                    check(Objects.equals(entry.getValue(), Json.obj(after.get("profiles")).get(entry.getKey())), "NeoForge preserves existing launcher profile");
                check(Json.obj(after.get("profiles")).containsKey(nativeInstance.profile()), "actual native installation registers profile");
            }
        }
        System.out.println("Client setup checks passed. Fixtures: " + root);
    }
}
