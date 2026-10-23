package crazycraft.loader;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import javax.swing.*;

/** Launcher selection and release installation, shared by new installs and updates. */
final class ClientSetup {
    enum Launcher { PRISM, MINECRAFT }
    record Instance(String name, Path root, Path game, String profile) {
        public String toString() { return name + " — " + game; }
    }
    record Release(String tag, Path contents, Manifest manifest) {}
    static final String REPO = "CoolFreeze23/CrazyCraft-5.0";

    static Path defaultRoot(Launcher launcher) {
        Path home = Path.of(System.getProperty("user.home"));
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return Path.of(System.getenv().getOrDefault("APPDATA", home.toString()))
                .resolve(launcher == Launcher.PRISM ? "PrismLauncher" : ".minecraft");
        if (os.contains("mac")) return home.resolve("Library/Application Support")
                .resolve(launcher == Launcher.PRISM ? "PrismLauncher" : "minecraft");
        return launcher == Launcher.PRISM ? home.resolve(".local/share/PrismLauncher") : home.resolve(".minecraft");
    }

    static Path instancesDir(Path root) throws IOException {
        Path cfg = root.resolve("prismlauncher.cfg");
        if (Files.isRegularFile(cfg)) for (String line : Files.readAllLines(cfg)) {
            if (line.startsWith("InstanceDir=")) {
                String value = line.substring(12).strip();
                if (value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
                if (!value.isBlank()) return root.resolve(value).toAbsolutePath().normalize();
            }
        }
        return root.resolve("instances");
    }

    static Path profilesFile(Path root) {
        Path store = root.resolve("launcher_profiles_microsoft_store.json");
        return Files.exists(store) ? store : root.resolve("launcher_profiles.json");
    }

    static Map<String, Object> readObject(Path path) throws IOException {
        return Json.obj(Json.parse(Files.readString(path)));
    }

    static boolean isPack(Path game) {
        try {
            return REPO.equals(Manifest.parse(Files.readString(game.resolve("crazycraft-manifest.json"))).repo());
        } catch (Exception e) {
            if (!Files.isRegularFile(game.resolve("resourcepacks/CrazyCraft5-ptBR.zip")) || !Files.isDirectory(game.resolve("mods"))) return false;
            try (var jars = Files.list(game.resolve("mods"))) {
                return jars.anyMatch(p -> p.getFileName().toString().startsWith("orespawn_integrations-") && p.toString().endsWith(".jar"));
            } catch (IOException ignored) { return false; }
        }
    }

    static List<Instance> discover(Launcher launcher, Path root) throws IOException {
        List<Instance> found = new ArrayList<>();
        if (launcher == Launcher.PRISM) {
            Path base = instancesDir(root);
            if (!Files.isDirectory(base)) return found;
            try (var dirs = Files.list(base)) {
                for (Path p : dirs.filter(Files::isDirectory).sorted().toList()) {
                    Path game = Files.isDirectory(p.resolve("minecraft")) ? p.resolve("minecraft") : p.resolve(".minecraft");
                    if (!isPack(game)) continue;
                    String name = p.getFileName().toString();
                    for (String line : Files.exists(p.resolve("instance.cfg")) ? Files.readAllLines(p.resolve("instance.cfg")) : List.<String>of())
                        if (line.startsWith("name=")) name = line.substring(5);
                    found.add(new Instance(name, p, game, null));
                }
            }
        } else if (Files.isRegularFile(profilesFile(root))) {
            Map<String, Object> profiles = Json.obj(readObject(profilesFile(root)).get("profiles"));
            if (profiles != null) for (var entry : profiles.entrySet()) {
                Map<String, Object> p = Json.obj(entry.getValue());
                String gameDir = Json.str(p, "gameDir");
                if (gameDir == null) continue;
                Path game = root.resolve(gameDir).toAbsolutePath().normalize();
                if (isPack(game)) found.add(new Instance(Json.str(p, "name"), root, game, entry.getKey()));
            }
        }
        if (launcher == Launcher.MINECRAFT && Files.isDirectory(root.resolve("crazycraft-instances"))) {
            try (var dirs = Files.list(root.resolve("crazycraft-instances"))) {
                for (Path game : dirs.filter(Files::isDirectory).toList()) {
                    if (isPack(game) && found.stream().noneMatch(i -> i.game().equals(game)))
                        found.add(new Instance(game.getFileName().toString(), root, game,
                                "crazycraft-" + UUID.nameUUIDFromBytes(game.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                }
            }
        }
        return found;
    }

    static Release latest(Consumer<String> status) throws Exception {
        status.accept("Checking the latest CrazyCraft release…");
        Map<String, Object> release = Json.obj(Json.parse(Download.text("https://api.github.com/repos/" + REPO + "/releases/latest")));
        Map<String, Object> asset = Json.arr(release.get("assets")).stream().map(Json::obj)
                .filter(a -> "CrazyCraft5-Client.zip".equals(a.get("name"))).findFirst()
                .orElseThrow(() -> new IOException("The latest release has no client installer files."));
        Path stage = Files.createTempDirectory("crazycraft-client-");
        Path zip = stage.resolve("client.zip");
        String digest = Json.str(asset, "digest");
        status.accept("Downloading " + release.get("tag_name") + " setup files…");
        Download.fetch(Json.str(asset, "browser_download_url"), zip, Json.num(asset, "size"),
                digest != null && digest.startsWith("sha256:") ? digest.substring(7) : null, "SHA-256", n -> {});
        Path contents = stage.resolve("contents");
        unzip(zip, contents);
        Manifest manifest = Manifest.parse(Files.readString(contents.resolve(".minecraft/crazycraft-manifest.json")));
        if (!REPO.equals(manifest.repo()) || !Json.str(release, "tag_name").equals("v" + manifest.packVersion()))
            throw new IOException("Release and client manifest do not match.");
        return new Release(Json.str(release, "tag_name"), contents, manifest);
    }

    static void unzip(Path zip, Path dest) throws IOException {
        try (ZipFile z = new ZipFile(zip.toFile())) {
            var entries = z.entries();
            long bytes = 0;
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path out = safe(dest, entry.getName());
                if (entry.isDirectory()) { Files.createDirectories(out); continue; }
                if (entry.getSize() < 0 || (bytes += entry.getSize()) > 256L * 1024 * 1024)
                    throw new IOException("Client setup archive is too large.");
                Files.createDirectories(out.getParent());
                try (var in = z.getInputStream(entry)) { Files.copy(in, out); }
            }
        }
    }

    static Path safe(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path result = base.resolve(relative).normalize();
        if (!result.startsWith(base)) throw new IOException("File escapes installation folder: " + relative);
        for (Path p = result; p != null; p = p.getParent())
            if (Files.isSymbolicLink(p)) throw new IOException("Linked installation paths are not supported: " + p);
        return result;
    }

    static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), ".crazycraft-", ".tmp");
        Files.writeString(tmp, text);
        try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    static void backup(Path file, Path to) throws IOException {
        if (Files.isRegularFile(file)) {
            Files.createDirectories(to.getParent());
            Files.copy(file, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Instance newInstance(Launcher launcher, Path root, String name) throws IOException {
        if (!name.matches("[\\p{L}\\p{N} _-]{1,64}") || name.isBlank())
            throw new IOException("Use 1–64 letters, numbers, spaces, underscores or hyphens for the name.");
        Path folder = safe(launcher == Launcher.PRISM ? instancesDir(root) : root.resolve("crazycraft-instances"), name.strip());
        if (Files.exists(folder)) throw new IOException("That folder already exists. Choose an existing instance or another name.");
        return launcher == Launcher.PRISM ? new Instance(name, folder, folder.resolve(".minecraft"), null)
                : new Instance(name, root, folder, "crazycraft-" + UUID.randomUUID());
    }

    static Path apply(Release release, Launcher launcher, Instance instance, boolean update) throws IOException {
        Path game = safe(instance.game(), "");
        if (update && !isPack(game)) throw new IOException("This is not a recognized CrazyCraft installation.");
        Path history = safe(game, ".crazycraft/backups/" + Instant.now().toEpochMilli());
        Files.createDirectories(history);
        // Import ownership from older Prism imports so obsolete pack jars can be retired without touching user mods.
        Path oldManifest = game.resolve("crazycraft-manifest.json");
        if (update && !Files.exists(oldManifest)) {
            try (var files = Files.list(game.resolve("mods"))) {
                write(safe(game, ".crazycraft/legacy-mods.json"), Json.write(files.filter(p -> p.toString().endsWith(".jar"))
                        .map(p -> p.getFileName().toString()).toList()));
            }
        }
        if (Files.isRegularFile(oldManifest)) {
            Manifest old = Manifest.parse(Files.readString(oldManifest));
            Path statePath = safe(game, ".crazycraft/state.json");
            backup(statePath, history.resolve("state.json"));
            Map<String, Object> state = Files.exists(statePath) ? readObject(statePath) : new LinkedHashMap<>();
            Map<String, Object> owned = state.containsKey("installed") ? Json.obj(state.get("installed")) : new LinkedHashMap<>();
            for (Manifest.Mod mod : old.mods()) if (mod.onClient()) owned.putIfAbsent(mod.dir() + "/" + mod.file(), Map.of());
            state.put("installed", owned);
            state.putIfAbsent("pack", old.packVersion());
            write(statePath, Json.write(state));
        }
        Path payload = release.contents().resolve(".minecraft");
        Set<String> allowed = Set.of("config", "defaultconfigs", "resourcepacks", "mcheli", "moonlight-global-datapacks", "patchouli_books",
                "emi.json", "patchouli_data.json", "icon.png", "options.txt", "crazycraft-loader.jar", "crazycraft-manifest.json");
        try (var files = Files.walk(payload)) {
            for (Path src : files.filter(Files::isRegularFile).toList()) {
                Path rel = payload.relativize(src);
                if (!allowed.contains(rel.getName(0).toString())) throw new IOException("Unexpected client file: " + rel);
                Path target = safe(game, rel.toString());
                if (update && Set.of("options.txt", "emi.json", "patchouli_data.json").contains(rel.toString()) && Files.exists(target)) continue;
                backup(target, history.resolve("game").resolve(rel));
                Files.createDirectories(target.getParent());
                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (launcher == Launcher.PRISM) {
            Path cfg = safe(instance.root(), "instance.cfg");
            Path components = safe(instance.root(), "mmc-pack.json");
            backup(cfg, history.resolve("instance.cfg"));
            backup(components, history.resolve("mmc-pack.json"));
            if (!Files.exists(cfg)) write(cfg, Files.readString(release.contents().resolve("instance.cfg")).replace("name=CrazyCraft 5.0", "name=" + instance.name()));
            // Preserve other component metadata, but replace the Minecraft and NeoForge pins from the release.
            Map<String, Object> desired = readObject(release.contents().resolve("mmc-pack.json"));
            if (update && Files.exists(components)) {
                Map<String, Object> existing = readObject(components);
                List<Object> merged = new ArrayList<>(Json.arr(existing.get("components")));
                for (Object component : Json.arr(desired.get("components"))) {
                    Object uid = Json.obj(component).get("uid");
                    merged.removeIf(c -> Objects.equals(Json.obj(c).get("uid"), uid));
                    merged.add(component);
                }
                existing.put("components", merged);
                desired = existing;
            }
            write(components, Json.write(desired));
        }
        return history;
    }

    static void installNative(Release release, Path root, Instance instance, Consumer<String> status) throws Exception {
        Path profiles = profilesFile(root);
        if (!Files.isRegularFile(profiles)) throw new IOException("Open the Minecraft Launcher and sign in once, then close it and retry.");
        Path history = instance.game().resolve(".crazycraft/launcher-backups/" + Instant.now().toEpochMilli());
        backup(profiles, history.resolve(profiles.getFileName()));
        Manifest.NeoForge neo = release.manifest().neoforge();
        Path installer = release.contents().getParent().resolve("neoforge-installer.jar");
        status.accept("Installing NeoForge " + neo.version() + " for the Minecraft Launcher…");
        Download.fetch(neo.installer(), installer, 0, neo.sha1(), "SHA-1", n -> {});
        Path log = history.resolve("neoforge.log");
        // The NeoForge installer also edits its generic launcher profile. Restore that file before adding ours.
        Map<Path, byte[]> original = new LinkedHashMap<>();
        for (String name : List.of("launcher_profiles.json", "launcher_profiles_microsoft_store.json")) {
            Path file = root.resolve(name);
            original.put(file, Files.exists(file) ? Files.readAllBytes(file) : null);
        }
        try {
            Process process = new ProcessBuilder(Installer.javaExe(), "-jar", installer.toString(), "--installClient", root.toString())
                    .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (process.waitFor() != 0) throw new IOException("NeoForge installation failed. Details: " + log);
        } finally {
            for (var entry : original.entrySet()) {
                if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
                else Files.write(entry.getKey(), entry.getValue());
            }
        }
        String version = "neoforge-" + neo.version();
        if (!Files.isRegularFile(root.resolve("versions").resolve(version).resolve(version + ".json")))
            throw new IOException("NeoForge did not create its launcher version. See " + log);
        registerNative(root, instance, version);
    }

    static void registerNative(Path root, Instance instance, String version) throws IOException {
        Path file = profilesFile(root);
        Map<String, Object> data = readObject(file);
        Map<String, Object> profiles = Json.obj(data.computeIfAbsent("profiles", k -> new LinkedHashMap<>()));
        Map<String, Object> p = profiles.containsKey(instance.profile()) ? Json.obj(profiles.get(instance.profile())) : new LinkedHashMap<>();
        p.put("name", instance.name());
        p.put("type", "custom");
        p.put("lastVersionId", version);
        p.put("gameDir", instance.game().toAbsolutePath().toString());
        p.putIfAbsent("javaArgs", "-Xms2G -Xmx8G -XX:+UseG1GC");
        p.putIfAbsent("created", Instant.now().toString());
        profiles.put(instance.profile(), p);
        write(file, Json.write(data));
    }

    static void requireClosed() throws IOException {
        for (ProcessHandle process : ProcessHandle.allProcesses().toList()) {
            if (process.pid() == ProcessHandle.current().pid()) continue;
            String cmd = process.info().command().orElse("").toLowerCase(Locale.ROOT);
            String args = String.join(" ", process.info().arguments().orElse(new String[0]));
            if (cmd.endsWith("prismlauncher.exe") || cmd.endsWith("/prismlauncher") || cmd.endsWith("minecraftlauncher.exe")
                    || args.contains("net.minecraft.client.main.Main") || args.contains("org.prismlauncher.EntryPoint") || args.contains("forgeclient"))
                throw new IOException("Close Minecraft and its launcher before installing or updating, then retry.");
        }
    }

    static void open() {
        open(null);
    }

    static void open(Path screenshot) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CrazyCraft Setup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIconImages(Theme.ICONS.stream().filter(Objects::nonNull).toList());
            JPanel form = new JPanel(new GridLayout(0, 1, 6, 8));
            form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
            JComboBox<String> launcher = new JComboBox<>(new String[]{"Prism Launcher", "Minecraft Launcher"});
            JTextField root = new JTextField(defaultRoot(Launcher.PRISM).toString());
            JComboBox<Object> target = new JComboBox<>();
            JTextField name = new JTextField("CrazyCraft 5.0");
            JLabel status = new JLabel("Close Minecraft and its launcher before continuing.");
            JButton scan = new Theme.TextureButton("Find existing installations", false);
            JButton browse = new Theme.TextureButton("Browse…", false);
            JButton go = new Theme.TextureButton("Install / update to latest release", true);
            JButton server = new Theme.TextureButton("Set up a server instead", false);
            Runnable refresh = () -> {
                target.removeAllItems(); target.addItem("Create a new installation");
                try {
                    for (Instance i : discover(Launcher.values()[launcher.getSelectedIndex()], Path.of(root.getText()))) target.addItem(i);
                    status.setText((target.getItemCount() - 1) + " CrazyCraft installation(s) found.");
                } catch (Exception e) { status.setText("Could not read folder: " + e.getMessage()); }
            };
            scan.addActionListener(e -> refresh.run());
            launcher.addActionListener(e -> { root.setText(defaultRoot(Launcher.values()[launcher.getSelectedIndex()]).toString()); refresh.run(); });
            target.addActionListener(e -> name.setEnabled(!(target.getSelectedItem() instanceof Instance)));
            browse.addActionListener(e -> {
                JFileChooser picker = new JFileChooser(root.getText()); picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (picker.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) { root.setText(picker.getSelectedFile().toString()); refresh.run(); }
            });
            go.addActionListener(e -> {
                Launcher chosen = Launcher.values()[launcher.getSelectedIndex()];
                Path base;
                Instance instance;
                boolean update = target.getSelectedItem() instanceof Instance;
                try {
                    requireClosed();
                    base = Path.of(root.getText()).toAbsolutePath().normalize();
                    if (chosen == Launcher.PRISM && !Files.isRegularFile(base.resolve("prismlauncher.cfg")))
                        throw new IOException("Choose the Prism data folder containing prismlauncher.cfg. Open Prism once first.");
                    if (chosen == Launcher.MINECRAFT && !Files.isRegularFile(profilesFile(base)))
                        throw new IOException("Choose your Minecraft folder after signing into the launcher once.");
                    instance = update ? (Instance) target.getSelectedItem() : newInstance(chosen, base, name.getText());
                } catch (Exception ex) { JOptionPane.showMessageDialog(frame, ex.getMessage(), "Setup", JOptionPane.ERROR_MESSAGE); return; }
                for (var component : form.getComponents()) component.setEnabled(false);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                Consumer<String> progress = text -> SwingUtilities.invokeLater(() -> status.setText(text));
                new SwingWorker<Release, Void>() {
                    protected Release doInBackground() throws Exception {
                        Release release = latest(progress);
                        requireClosed();
                        progress.accept("Preparing " + instance.name() + " for " + release.tag() + "…");
                        apply(release, chosen, instance, update);
                        if (chosen == Launcher.MINECRAFT) installNative(release, base, instance, progress);
                        return release;
                    }
                    protected void done() {
                        try {
                            Release release = get();
                            Gui.openClientSetup(release.manifest(), instance.game(), "Open " + (chosen == Launcher.PRISM ? "Prism" : "Minecraft Launcher") + " and select “" + instance.name() + "”.");
                            frame.dispose();
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                            JOptionPane.showMessageDialog(frame, cause.getMessage(), "Setup could not finish", JOptionPane.ERROR_MESSAGE);
                            for (var component : form.getComponents()) component.setEnabled(true);
                            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            refresh.run();
                        }
                    }
                }.execute();
            });
            server.addActionListener(e -> {
                try { Gui.open(Main.loadManifest(null, Main.defaultDir()), Main.defaultDir(), Installer.Mode.SERVER); frame.dispose(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(frame, ex.getMessage()); }
            });
            form.add(new JLabel("CrazyCraft — install or update your game"));
            form.add(launcher); form.add(new JLabel("Launcher data folder (portable installs: Browse)")); form.add(root); form.add(browse);
            form.add(scan); form.add(target); form.add(new JLabel("New installation name")); form.add(name);
            form.add(new JLabel("Updates keep saves and player settings; replaced files are backed up."));
            form.add(status); form.add(go); form.add(server);
            form.setBackground(Theme.BG);
            for (var component : form.getComponents()) {
                component.setFont(Theme.font(java.awt.Font.PLAIN, 14));
                if (component instanceof JLabel) component.setForeground(Theme.TEXT);
            }
            form.getComponent(0).setFont(Theme.font(java.awt.Font.BOLD, 22));
            form.getComponent(0).setForeground(Theme.GOLD_LIGHT);
            frame.add(form, BorderLayout.CENTER); frame.setSize(760, 660); frame.setLocationRelativeTo(null);
            refresh.run();
            if (screenshot == null) frame.setVisible(true);
            else {
                form.setSize(760, 660); layout(form);
                var image = new java.awt.image.BufferedImage(760, 660, java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g = image.createGraphics(); form.printAll(g); g.dispose();
                try { javax.imageio.ImageIO.write(image, "png", screenshot.toFile()); }
                catch (IOException e) { throw new RuntimeException(e); }
                frame.dispose();
            }
        });
    }

    private static void layout(java.awt.Container container) {
        container.doLayout();
        for (var child : container.getComponents()) if (child instanceof java.awt.Container nested) layout(nested);
    }
}
