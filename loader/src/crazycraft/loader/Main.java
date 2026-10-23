package crazycraft.loader;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CrazyCraft setup. With a screen it opens the setup window; with --nogui (or on a server without a screen) it runs in
 * the terminal, which is what the server start scripts do before every start. --client sets up a game folder instead
 * (mods and resource packs, no NeoForge); the pack's Prism instance runs it before every launch, and it only opens a
 * window when something has to be downloaded.
 *
 * Options: --client, --nogui, --dir <folder>, --manifest <file>, --accept-eula, --memory <GB>,
 * --screenshot <png> --demo <start|running|manual|done> (renders the window to an image without showing it).
 */
public final class Main {
    static final String VERSION = "1.1.1";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        I18n.initialize(args);
        int screenshotIndex = java.util.Arrays.asList(args).indexOf("--setup-screenshot");
        if (screenshotIndex >= 0) {
            ClientSetup.open(Path.of(args[screenshotIndex + 1]));
            return;
        }
        if ((args.length == 0 || java.util.Arrays.asList(args).contains("--setup")) && !GraphicsEnvironment.isHeadless()) {
            ClientSetup.open();
            return;
        }
        boolean nogui = false;
        boolean client = false;
        boolean acceptEula = "true".equalsIgnoreCase(System.getenv("CRAZYCRAFT_ACCEPT_EULA"));
        Path dir = null;
        Path manifestFile = null;
        String screenshot = null;
        Path selfTest = null;
        Path selfTestManual = null;
        String demo = "start";
        int memory = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nogui", "nogui" -> nogui = true;
                case "--client" -> client = true;
                case "--server-setup" -> { }
                case "--lang" -> i++;
                case "--accept-eula" -> acceptEula = true;
                case "--dir" -> dir = Path.of(args[++i]);
                case "--manifest" -> manifestFile = Path.of(args[++i]);
                case "--memory" -> memory = Integer.parseInt(args[++i]);
                case "--screenshot" -> screenshot = args[++i];
                case "--demo" -> demo = args[++i];
                case "--selftest-gui" -> selfTest = Path.of(args[++i]);
                case "--selftest-manual" -> selfTestManual = Path.of(args[++i]);
                case "--version" -> {
                    System.out.println("CrazyCraft loader " + VERSION);
                    return;
                }
                default -> {
                    System.err.println(I18n.t("Unknown option ") + args[i]);
                    System.exit(64);
                }
            }
        }
        if (dir == null) {
            dir = defaultDir();
        }
        Manifest manifest = loadManifest(manifestFile, dir);
        Installer.Mode mode = client ? Installer.Mode.CLIENT : Installer.Mode.SERVER;

        if (screenshot != null) {
            System.setProperty("java.awt.headless", "true");
            Gui.screenshot(manifest, dir, mode, demo, Path.of(screenshot));
            return;
        }
        if (selfTest != null) {
            System.setProperty("java.awt.headless", "true");
            System.exit(Gui.selfTest(manifest, dir, mode, selfTest, selfTestManual));
        }
        if (client && !new Installer(dir, manifest, ConsoleUi.QUIET, mode).needsWork()) {
            System.out.println("CrazyCraft " + manifest.packVersion() + I18n.t(": every mod and resource pack is in place."));
            System.exit(0);
        }
        if (!nogui && !GraphicsEnvironment.isHeadless()) {
            Gui.open(manifest, dir, mode);
            return;
        }

        ConsoleUi ui = new ConsoleUi();
        ConsoleUi.banner(manifest, mode);
        if (memory > 0 && !client) {
            Installer.setMemoryGb(dir, memory);
        }
        Installer.Result result = new Installer(dir, manifest, ui, mode).run();
        if (result != Installer.Result.READY) {
            System.out.println();
            System.out.println(result == Installer.Result.INCOMPLETE
                    ? I18n.t("  Setup is not finished: some mods still have to be downloaded by hand (see above).")
                    : I18n.t("  Setup did not finish; see the messages above and .crazycraft/loader.log."));
            System.exit(result == Installer.Result.INCOMPLETE ? 3 : 1);
        }
        if (client) {
            System.out.println();
            System.out.println(I18n.t("  Ready."));
            System.exit(0);
        }
        boolean eula = ui.eula(dir, acceptEula);
        System.out.println();
        System.out.println(eula ? I18n.t("  Ready.") : I18n.t("  Ready, once the EULA is accepted."));
        System.exit(eula ? 0 : 2);
    }

    /** The folder the jar sits in, which is the server folder when it comes from the server files. */
    static Path defaultDir() {
        try {
            Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(jar)) {
                return jar.getParent();
            }
        } catch (URISyntaxException | SecurityException e) {
            // fall back to the working directory
        }
        return Path.of("").toAbsolutePath();
    }

    static Manifest loadManifest(Path file, Path dir) throws IOException {
        if (file == null && Files.isRegularFile(dir.resolve("crazycraft-manifest.json"))) {
            file = dir.resolve("crazycraft-manifest.json");
        }
        if (file != null) {
            return Manifest.parse(Files.readString(file, StandardCharsets.UTF_8));
        }
        try (InputStream in = Main.class.getResourceAsStream("/crazycraft/loader/manifest.json")) {
            if (in == null) {
                throw new IOException(I18n.t("No crazycraft-manifest.json next to the loader"));
            }
            return Manifest.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
