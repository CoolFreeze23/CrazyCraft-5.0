package crazycraft.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

/** The text front end, used by the start scripts and on servers without a screen. */
final class ConsoleUi implements Installer.Listener {
    private final boolean interactive = System.console() != null;
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private long lastLine;
    private int lastPercent = -1;
    private boolean midLine;

    /** For quick checks that report nothing. */
    static final Installer.Listener QUIET = new Installer.Listener() {
        @Override
        public void log(String line) {
        }

        @Override
        public void step(Installer.Step step, Installer.State state, String detail) {
        }

        @Override
        public void progress(long bytesDone, long bytesTotal, int filesDone, int filesTotal, String current) {
        }

        @Override
        public void manual(List<Manifest.Mod> missing, Installer installer) {
        }
    };

    static void banner(Manifest m, Installer.Mode mode) {
        System.out.println();
        System.out.println("  ==============================================================");
        System.out.println("    " + m.packName() + (mode == Installer.Mode.CLIENT ? I18n.t(" game") : I18n.t(" server")) + I18n.t(" setup  -  pack ")
                + m.packVersion() + "  -  Minecraft " + m.minecraft());
        System.out.println(I18n.t("    Every mod is downloaded from its official page (Modrinth, CurseForge"));
        System.out.println(I18n.t("    or the author's GitHub) and checked before it is used."));
        System.out.println("  ==============================================================");
        System.out.println();
    }

    @Override
    public synchronized void log(String line) {
        endLine();
        System.out.println("  " + line);
    }

    @Override
    public synchronized void step(Installer.Step step, Installer.State state, String detail) {
        if (state == Installer.State.RUNNING && detail.isEmpty()) {
            return;
        }
        if (state == Installer.State.RUNNING && System.currentTimeMillis() - lastLine < 1500 && step == Installer.Step.MODS) {
            return;
        }
        lastLine = System.currentTimeMillis();
        endLine();
        if (step == Installer.Step.NEOFORGE && state == Installer.State.WAITING) {
            return;
        }
        String tag = switch (state) {
            case DONE -> "[ OK ]";
            case RUNNING -> "[ .. ]";
            case WARNING -> "[WARN]";
            case FAILED -> "[FAIL]";
            case WAITING -> "[    ]";
        };
        System.out.println("  " + tag + " " + pad(I18n.t(step.title), 16) + detail);
    }

    @Override
    public synchronized void progress(long bytesDone, long bytesTotal, int filesDone, int filesTotal, String current) {
        int pct = bytesTotal > 0 ? (int) (bytesDone * 100 / bytesTotal) : 100;
        if (interactive) {
            String bar = "#".repeat(pct / 4) + "-".repeat(25 - pct / 4);
            String line = String.format(Locale.ROOT, "\r  [%s] %3d%%  %d/%d mods  %s / %s  ", bar, pct, filesDone,
                    filesTotal, mb(bytesDone), mb(bytesTotal));
            System.out.print(line);
            midLine = true;
        } else if (pct / 10 != lastPercent / 10) {
            lastPercent = pct;
            System.out.println(String.format(Locale.ROOT, "  ...%3d%%  %d/%d mods  %s / %s", pct, filesDone, filesTotal,
                    mb(bytesDone), mb(bytesTotal)));
        }
    }

    @Override
    public void manual(List<Manifest.Mod> missing, Installer installer) throws InterruptedException {
        List<Manifest.Mod> left = missing;
        while (!left.isEmpty()) {
            synchronized (this) {
                endLine();
                System.out.println();
                System.out.println(I18n.t("  These mods have to be downloaded by hand, because their authors only allow"));
                System.out.println(I18n.t("  downloads from CurseForge itself:"));
                for (Manifest.Mod m : left) {
                    System.out.println();
                    System.out.println("    " + m.name() + " (" + m.file() + ")");
                    System.out.println("    " + (m.source().download() != null ? m.source().download() : m.source().page()));
                }
                System.out.println();
                System.out.println(I18n.t("  Save the file into ") + installer.manualDir + I18n.t(" (or your Downloads folder)."));
            }
            if (!interactive) {
                System.out.println(I18n.t("  Then run the setup again."));
                return;
            }
            System.out.print(I18n.t("  Press Enter to look again, or type skip to go on without it: "));
            String answer;
            try {
                answer = in.readLine();
            } catch (IOException e) {
                return;
            }
            if (answer == null || (answer.strip().equalsIgnoreCase("skip") || answer.strip().equalsIgnoreCase("pular"))) {
                return;
            }
            try {
                left = installer.scanManual(left);
            } catch (IOException e) {
                System.out.println(I18n.t("  Could not read the folder: ") + e.getMessage());
            }
        }
    }

    /** Asks about the EULA if it hasn't been accepted; returns whether it is accepted now. */
    boolean eula(java.nio.file.Path dir, boolean preAccepted) throws IOException {
        if (Installer.eulaAccepted(dir)) {
            return true;
        }
        if (preAccepted) {
            Installer.acceptEula(dir);
            System.out.println(I18n.t("  EULA accepted (https://aka.ms/MinecraftEULA)."));
            return true;
        }
        if (!interactive) {
            System.out.println(I18n.t("  Before the server starts, accept the Minecraft EULA (https://aka.ms/MinecraftEULA):"));
            System.out.println(I18n.t("  set eula=true in eula.txt, or run the setup with --accept-eula."));
            return false;
        }
        System.out.println();
        System.out.print(I18n.t("  Do you accept the Minecraft EULA (https://aka.ms/MinecraftEULA)? [y/N]: "));
        String a = in.readLine();
        if (a != null && (a.strip().equalsIgnoreCase("y") || a.strip().equalsIgnoreCase("yes") || (I18n.portuguese() && (a.strip().equalsIgnoreCase("s") || a.strip().equalsIgnoreCase("sim"))))) {
            Installer.acceptEula(dir);
            return true;
        }
        System.out.println(I18n.t("  Not accepted; the server won't start until eula=true is set in eula.txt."));
        return false;
    }

    private void endLine() {
        if (midLine) {
            System.out.println();
            midLine = false;
        }
    }

    private static String pad(String s, int n) {
        return s.length() >= n ? s + " " : s + " ".repeat(n - s.length());
    }

    static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.0f MB", bytes / 1048576.0);
    }
}
