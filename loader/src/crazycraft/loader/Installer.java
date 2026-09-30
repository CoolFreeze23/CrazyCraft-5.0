package crazycraft.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Installs or updates a CrazyCraft server folder or game folder: NeoForge (servers only), then every mod and resource
 * pack from its official download, checked and (for the few that need it) fixed, then the extra files. Nothing the
 * player or owner put in the folder is deleted: jars that are no longer part of the pack are moved to mods-removed/.
 */
final class Installer {
    enum Mode { SERVER, CLIENT }

    enum Step {
        JAVA("Java 21"), NEOFORGE("NeoForge server"), MODS("Mods"), FILES("Extra files"), TIDY("Tidy up");

        final String title;

        Step(String title) {
            this.title = title;
        }
    }

    enum State { WAITING, RUNNING, DONE, WARNING, FAILED }

    enum Result { READY, INCOMPLETE, FAILED }

    interface Listener {
        void log(String line);

        void step(Step step, State state, String detail);

        void progress(long bytesDone, long bytesTotal, int filesDone, int filesTotal, String current);

        /** Called when mods have to be downloaded by hand; returns once they are in place or the user moves on. */
        void manual(List<Manifest.Mod> missing, Installer installer) throws InterruptedException;
    }

    private static final int THREADS = 6;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    final Path dir;
    final Manifest manifest;
    final Mode mode;
    private final Listener ui;
    private final Path mods;
    private final Path work;
    private final Path downloads;
    final Path manualDir;
    private final Map<String, Map<String, Object>> installed = new ConcurrentHashMap<>();
    private final String stamp = LocalDateTime.now().format(STAMP);
    private PrintWriter logFile;
    private String previousVersion;

    Installer(Path dir, Manifest manifest, Listener ui, Mode mode) {
        this.dir = dir.toAbsolutePath().normalize();
        this.manifest = manifest;
        this.ui = ui;
        this.mode = mode;
        this.mods = this.dir.resolve("mods");
        this.work = this.dir.resolve(".crazycraft");
        this.downloads = work.resolve("downloads");
        this.manualDir = this.dir.resolve("manual");
    }

    /** The mods and resource packs this folder gets: everything but client-only files on a server, and so on. */
    List<Manifest.Mod> wanted() {
        return manifest.mods().stream().filter(m -> mode == Mode.SERVER ? m.onServer() : m.onClient()).toList();
    }

    private List<Manifest.Extract> wantedExtracts() {
        return manifest.extracts().stream().filter(x -> mode == Mode.SERVER ? x.onServer() : x.onClient()).toList();
    }

    private String noun() {
        return mode == Mode.SERVER ? "mods" : "files";
    }

    private Path target(Manifest.Mod m) {
        return dir.resolve(m.dir()).resolve(m.file());
    }

    private static String key(Manifest.Mod m) {
        return m.dir() + "/" + m.file();
    }

    Result run() {
        try {
            Files.createDirectories(mods);
            Files.createDirectories(downloads);
            Files.createDirectories(manualDir);
            logFile = new PrintWriter(Files.newBufferedWriter(work.resolve("loader.log"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND), true);
            loadState();
        } catch (IOException e) {
            log("Can't write to " + dir + ": " + e.getMessage());
            ui.step(Step.JAVA, State.FAILED, "This folder is not writable");
            return Result.FAILED;
        }
        log("CrazyCraft " + manifest.packVersion() + (mode == Mode.SERVER ? " server" : " game") + " setup in " + dir
                + (previousVersion != null && !previousVersion.equals(manifest.packVersion())
                ? " (updating from " + previousVersion + ")" : ""));
        Result result = Result.READY;
        try {
            if (!checkJava()) {
                return Result.FAILED;
            }
            if (mode == Mode.SERVER && !installNeoForge()) {
                return Result.FAILED;
            }
            Result m = installMods();
            if (m == Result.FAILED) {
                return Result.FAILED;
            }
            result = m;
            extractFiles();
            tidy();
            saveState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("Stopped.");
            return Result.FAILED;
        } catch (Exception e) {
            log("Something went wrong: " + e);
            return Result.FAILED;
        } finally {
            try {
                saveState();
            } catch (IOException ignored) {
                // the next run checks every file again
            }
        }
        log(result == Result.READY ? "All set: " + wanted().size() + " " + noun() + " ready."
                : "Setup finished, but some mods are still missing.");
        return result;
    }

    /** A quick check, without downloading or writing anything: is any file missing or out of date? */
    boolean needsWork() {
        loadState();
        try {
            for (Manifest.Mod m : wanted()) {
                if (!isGood(m, target(m))) {
                    return true;
                }
            }
            for (Manifest.Extract x : wantedExtracts()) {
                Path t = dir.resolve(x.to());
                if (!Files.isRegularFile(t) || !Hashes.sha256(Files.readAllBytes(t)).equals(x.sha256())) {
                    return true;
                }
            }
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    // ---- Java ----

    private boolean checkJava() {
        ui.step(Step.JAVA, State.RUNNING, "");
        Runtime.Version v = Runtime.version();
        String vendor = System.getProperty("java.vendor", "");
        if (v.feature() < 21) {
            ui.step(Step.JAVA, State.FAILED, "Java " + v.feature() + " is too old; Minecraft 1.21.1 needs 21");
            return false;
        }
        ui.step(Step.JAVA, State.DONE, "Java " + v.feature() + "." + v.interim() + "." + v.update()
                + (vendor.isEmpty() ? "" : " (" + vendor + ")"));
        return true;
    }

    // ---- NeoForge ----

    private boolean installNeoForge() throws InterruptedException {
        Manifest.NeoForge neo = manifest.neoforge();
        Path lib = dir.resolve("libraries/net/neoforged/neoforge/" + neo.version());
        if (Files.isRegularFile(lib.resolve("win_args.txt")) && Files.isRegularFile(lib.resolve("unix_args.txt"))) {
            ui.step(Step.NEOFORGE, State.DONE, "NeoForge " + neo.version() + " is installed");
            return true;
        }
        ui.step(Step.NEOFORGE, State.RUNNING, "Downloading NeoForge " + neo.version());
        Path installer = work.resolve("neoforge-" + neo.version() + "-installer.jar");
        try {
            boolean have = Files.isRegularFile(installer) && Hashes.of(installer, "SHA-1").equalsIgnoreCase(neo.sha1());
            if (!have) {
                Download.fetch(neo.installer(), installer, 0, neo.sha1(), "SHA-1", d -> { });
            }
            ui.step(Step.NEOFORGE, State.RUNNING, "Installing NeoForge " + neo.version() + " (takes a minute)");
            ProcessBuilder pb = new ProcessBuilder(javaExe(), "-jar", installer.toString(), "--install-server")
                    .directory(dir.toFile()).redirectErrorStream(true);
            Process p = pb.start();
            int libs = 0;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    logFile.println("  [neoforge] " + line);
                    if (line.startsWith("Downloading library") || line.contains("Downloading")) {
                        libs++;
                        if (libs % 10 == 0) {
                            ui.step(Step.NEOFORGE, State.RUNNING, "Installing NeoForge (" + libs + " libraries)");
                        }
                    }
                }
            }
            int code = p.waitFor();
            Path installerLog = dir.resolve(installer.getFileName() + ".log");
            if (Files.isRegularFile(installerLog)) {
                Files.move(installerLog, work.resolve(installerLog.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
            if (code != 0 || !Files.isRegularFile(lib.resolve("unix_args.txt"))) {
                ui.step(Step.NEOFORGE, State.FAILED, "The NeoForge installer stopped (code " + code + "); see .crazycraft/loader.log");
                return false;
            }
        } catch (IOException e) {
            ui.step(Step.NEOFORGE, State.FAILED, "Could not install NeoForge: " + e.getMessage());
            log("NeoForge: " + e);
            return false;
        }
        ui.step(Step.NEOFORGE, State.DONE, "NeoForge " + neo.version() + " installed");
        log("NeoForge " + neo.version() + " installed.");
        return true;
    }

    static String javaExe() {
        return ProcessHandle.current().info().command().orElse("java");
    }

    // ---- mods and resource packs ----

    private Result installMods() throws InterruptedException, IOException {
        List<Manifest.Mod> wanted = wanted();
        ui.step(Step.MODS, State.RUNNING, "Checking " + wanted.size() + " " + noun());
        List<Manifest.Mod> need = new ArrayList<>();
        int checked = 0;
        for (Manifest.Mod m : wanted) {
            if (!isGood(m, target(m))) {
                need.add(m);
            }
            if (++checked % 10 == 0) {
                ui.step(Step.MODS, State.RUNNING, "Checking " + noun() + " (" + checked + " of " + wanted.size() + ")");
            }
        }
        List<Manifest.Mod> auto = need.stream().filter(m -> !m.manual()).toList();
        List<Manifest.Mod> byHand = new ArrayList<>(need.stream().filter(Manifest.Mod::manual).toList());
        if (need.isEmpty()) {
            ui.step(Step.MODS, State.DONE, wanted.size() + " " + noun() + ", all up to date");
            log("All " + wanted.size() + " " + noun() + " are already in place.");
            return Result.READY;
        }
        log((wanted.size() - need.size()) + " " + noun() + " already in place, " + auto.size() + " to download"
                + (byHand.isEmpty() ? "" : ", " + byHand.size() + " by hand"));

        long total = auto.stream().mapToLong(Manifest.Mod::size).sum();
        AtomicLong done = new AtomicLong();
        AtomicInteger files = new AtomicInteger();
        List<String> failed = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS, r -> {
            Thread t = new Thread(r, "download");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> jobs = new ArrayList<>();
            for (Manifest.Mod m : auto) {
                jobs.add(pool.submit(() -> {
                    ui.progress(done.get(), total, files.get(), auto.size(), m.file());
                    try {
                        fetchMod(m, d -> ui.progress(done.addAndGet(d), total, files.get(), auto.size(), m.file()));
                        int n = files.incrementAndGet();
                        ui.progress(done.get(), total, n, auto.size(), m.file());
                        ui.step(Step.MODS, State.RUNNING, "Downloaded " + n + " of " + auto.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        synchronized (failed) {
                            failed.add(m.file());
                        }
                        log("Could not get " + m.file() + " from " + m.source().label() + ": " + e.getMessage());
                    }
                    return null;
                }));
            }
            for (Future<?> f : jobs) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    log("Download job failed: " + e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
        if (!failed.isEmpty()) {
            ui.step(Step.MODS, State.FAILED, (failed.size() == 1 ? "1 file" : failed.size() + " files")
                    + " could not be downloaded; run setup again to retry");
            return Result.FAILED;
        }

        if (!byHand.isEmpty()) {
            byHand = scanManual(byHand);
            if (!byHand.isEmpty()) {
                ui.step(Step.MODS, State.WARNING, byHand.size() == 1 ? "1 mod needs a download by hand"
                        : byHand.size() + " mods need a download by hand");
                ui.manual(byHand, this);
                byHand = scanManual(byHand);
            }
            if (!byHand.isEmpty()) {
                ui.step(Step.MODS, State.WARNING, "Missing: " + String.join(", ",
                        byHand.stream().map(Manifest.Mod::name).toList()));
                return Result.INCOMPLETE;
            }
        }
        ui.step(Step.MODS, State.DONE, wanted.size() + " " + noun() + " ready (" + need.size() + " new)");
        return Result.READY;
    }

    private void fetchMod(Manifest.Mod m, Download.Progress progress) throws IOException, InterruptedException {
        Path target = target(m);
        if (m.patch() == null) {
            Path tmp = downloads.resolve(m.file() + ".part");
            Download.fetch(m.source().url(), tmp, m.size(), m.sha512(), "SHA-512", progress);
            place(tmp, target);
            record(m, target);
            log("Got " + m.file() + " from " + m.source().label());
            return;
        }
        Path orig = downloads.resolve(m.patch().originalFile().replaceAll("[\\\\/:*?\"<>|\\[\\] ]", "_") + ".orig");
        Download.fetch(m.source().url(), orig, m.size(), m.sha512(), "SHA-512", progress);
        Path fixed = downloads.resolve(m.file() + ".fixed");
        Patcher.apply(orig, m.patch(), fixed);
        Files.deleteIfExists(orig);
        place(fixed, target);
        record(m, target);
        log("Got " + m.patch().originalFile() + " from " + m.source().label() + " and applied the pack's fix: " + m.file());
    }

    /** Moves a finished file into place, first moving any old file of that name to mods-removed/. */
    private void place(Path from, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        if (Files.exists(target)) {
            moveAside(target);
        }
        Files.move(from, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void moveAside(Path file) throws IOException {
        Path dest = dir.resolve("mods-removed").resolve(stamp);
        Files.createDirectories(dest);
        Files.move(file, dest.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String identity(Manifest.Mod m) {
        return m.patch() == null ? "sha512:" + m.sha512() : "fixed:" + m.patch().result();
    }

    boolean isGood(Manifest.Mod m, Path f) throws IOException {
        if (!Files.isRegularFile(f)) {
            return false;
        }
        long size = Files.size(f);
        long modified = Files.getLastModifiedTime(f).toMillis();
        Map<String, Object> rec = installed.get(key(m));
        if (rec != null && identity(m).equals(rec.get("id")) && Json.num(rec, "size") == size
                && Json.num(rec, "modified") == modified) {
            return true;
        }
        boolean ok;
        try {
            ok = m.patch() == null
                    ? size == m.size() && Hashes.of(f, "SHA-512").equalsIgnoreCase(m.sha512())
                    : Hashes.contentHash(Hashes.files(f)).equals(m.patch().result());
        } catch (IOException e) {
            ok = false;
        }
        if (ok) {
            record(m, f);
        }
        return ok;
    }

    private void record(Manifest.Mod m, Path f) throws IOException {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("id", identity(m));
        rec.put("size", Files.size(f));
        rec.put("modified", Files.getLastModifiedTime(f).toMillis());
        installed.put(key(m), rec);
    }

    /** Looks for hand-downloaded mods in manual/ and the Downloads folder, and installs the ones it finds. */
    synchronized List<Manifest.Mod> scanManual(List<Manifest.Mod> missing) throws IOException {
        List<Manifest.Mod> still = new ArrayList<>();
        for (Manifest.Mod m : missing) {
            if (isGood(m, target(m))) {
                continue;
            }
            Path found = null;
            for (Path place : manualPlaces()) {
                found = findBySize(place, m);
                if (found != null) {
                    break;
                }
            }
            if (found != null) {
                installManual(m, found);
            } else {
                still.add(m);
            }
        }
        return still;
    }

    /** Installs a file the user picked or dropped, if it is the right one; returns the mod it matched, or null. */
    synchronized Manifest.Mod offer(Path file, List<Manifest.Mod> missing) throws IOException {
        long size = Files.size(file);
        for (Manifest.Mod m : missing) {
            if (m.size() == size && Hashes.of(file, "SHA-512").equalsIgnoreCase(m.sha512())) {
                installManual(m, file);
                return m;
            }
        }
        return null;
    }

    private void installManual(Manifest.Mod m, Path found) throws IOException {
        Path tmp = downloads.resolve(m.file() + ".part");
        Files.copy(found, tmp, StandardCopyOption.REPLACE_EXISTING);
        place(tmp, target(m));
        record(m, target(m));
        log("Found " + m.name() + " at " + found + " and copied it into " + m.dir() + "/");
    }

    List<Path> manualPlaces() {
        List<Path> out = new ArrayList<>();
        out.add(manualDir);
        String home = System.getProperty("user.home");
        if (home != null) {
            out.add(Path.of(home, "Downloads"));
        }
        String profile = System.getenv("USERPROFILE");
        if (profile != null && !Path.of(profile, "Downloads").equals(out.get(out.size() - 1))) {
            out.add(Path.of(profile, "Downloads"));
        }
        return out;
    }

    private Path findBySize(Path place, Manifest.Mod m) throws IOException {
        if (!Files.isDirectory(place)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(place)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p) && Files.size(p) == m.size()
                        && Hashes.of(p, "SHA-512").equalsIgnoreCase(m.sha512())) {
                    return p;
                }
            }
        }
        return null;
    }

    // ---- extra files ----

    private void extractFiles() {
        List<Manifest.Extract> list = wantedExtracts();
        if (list.isEmpty()) {
            ui.step(Step.FILES, State.DONE, "Nothing to add");
            return;
        }
        ui.step(Step.FILES, State.RUNNING, "Adding " + list.size() + (list.size() == 1 ? " file" : " files"));
        int ok = 0;
        for (Manifest.Extract x : list) {
            Path target = dir.resolve(x.to());
            try {
                if (Files.isRegularFile(target) && Hashes.sha256(Files.readAllBytes(target)).equals(x.sha256())) {
                    ok++;
                    continue;
                }
                Path jar = mods.resolve(x.from());
                if (!Files.isRegularFile(jar)) {
                    log("Skipped " + x.to() + ": " + x.from() + " is missing");
                    continue;
                }
                byte[] data;
                try (ZipFile z = new ZipFile(jar.toFile())) {
                    ZipEntry e = z.getEntry(x.entry());
                    if (e == null) {
                        throw new IOException(x.entry() + " is not in " + x.from());
                    }
                    try (InputStream in = z.getInputStream(e)) {
                        data = in.readAllBytes();
                    }
                }
                if (!Hashes.sha256(data).equals(x.sha256())) {
                    throw new IOException(x.entry() + " in " + x.from() + " is not the expected file");
                }
                Files.createDirectories(target.getParent());
                Files.write(target, data);
                log("Added " + x.to() + " (" + x.why() + ")");
                ok++;
            } catch (IOException e) {
                log("Could not add " + x.to() + ": " + e.getMessage());
            }
        }
        ui.step(Step.FILES, ok == list.size() ? State.DONE : State.WARNING, ok + " of " + list.size() + " in place");
    }

    // ---- tidy ----

    /**
     * Servers: jars in mods/ that aren't part of the pack (and not listed in user-mods.txt) move to mods-removed/, so
     * old versions can't clash with new ones after an update. Game folders: only jars this setup installed before and
     * the pack no longer has move; mods a player added are left alone.
     */
    private void tidy() throws IOException {
        ui.step(Step.TIDY, State.RUNNING, "");
        Set<String> keep = new HashSet<>();
        wanted().forEach(m -> keep.add(key(m)));
        List<String> moved = new ArrayList<>();
        if (mode == Mode.SERVER) {
            Set<String> own = userMods();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(mods, "*.jar")) {
                for (Path p : ds) {
                    String name = p.getFileName().toString();
                    if (!keep.contains("mods/" + name) && !own.contains(name)) {
                        moveAside(p);
                        moved.add(name);
                    }
                }
            }
        } else {
            for (String k : new ArrayList<>(installed.keySet())) {
                if (k.startsWith("mods/") && !keep.contains(k)) {
                    Path p = dir.resolve(k);
                    if (Files.isRegularFile(p)) {
                        moveAside(p);
                        moved.add(p.getFileName().toString());
                    }
                }
            }
        }
        installed.keySet().removeIf(k -> !keep.contains(k));
        if (moved.isEmpty()) {
            ui.step(Step.TIDY, State.DONE, "Nothing to tidy");
        } else {
            ui.step(Step.TIDY, State.DONE, (moved.size() == 1 ? "1 old jar" : moved.size() + " old jars")
                    + " moved to mods-removed/");
            log("Moved " + moved.size() + (moved.size() == 1 ? " jar that is" : " jars that are")
                    + " not part of CrazyCraft " + manifest.packVersion() + " to mods-removed/" + stamp + ": "
                    + String.join(", ", moved)
                    + (mode == Mode.SERVER ? ". To keep a mod of your own, list its file name in user-mods.txt." : "."));
        }
    }

    private Set<String> userMods() throws IOException {
        Set<String> out = new HashSet<>();
        Path f = dir.resolve("user-mods.txt");
        if (Files.isRegularFile(f)) {
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String s = line.strip();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    // ---- state ----

    private void loadState() {
        Path f = work.resolve("state.json");
        if (!Files.isRegularFile(f)) {
            return;
        }
        try {
            Map<String, Object> root = Json.obj(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
            previousVersion = Json.str(root, "pack");
            Map<String, Object> inst = Json.obj(root.get("installed"));
            if (inst != null) {
                inst.forEach((k, v) -> installed.put(k, Json.obj(v)));
            }
        } catch (Exception e) {
            // a damaged state file only means every file gets checked again
        }
    }

    private void saveState() throws IOException {
        if (!Files.isDirectory(work)) {
            return;
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("pack", manifest.packVersion());
        root.put("installed", new java.util.TreeMap<>(installed));
        Files.writeString(work.resolve("state.json"), Json.write(root), StandardCharsets.UTF_8);
    }

    void log(String line) {
        if (logFile != null) {
            logFile.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0, 8) + " " + line);
        }
        ui.log(line);
    }

    // ---- EULA and memory, used by the server front ends ----

    static boolean eulaAccepted(Path dir) {
        try {
            return Files.readString(dir.resolve("eula.txt")).lines().anyMatch(l -> l.strip().equalsIgnoreCase("eula=true"));
        } catch (IOException e) {
            return false;
        }
    }

    static void acceptEula(Path dir) throws IOException {
        Files.writeString(dir.resolve("eula.txt"), "# Accepted in the CrazyCraft server setup "
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " (https://aka.ms/MinecraftEULA)\neula=true\n");
    }

    /** The -Xmx value in user_jvm_args.txt, in GB, or 0 when it isn't set. */
    static int memoryGb(Path dir) {
        try {
            for (String l : Files.readAllLines(dir.resolve("user_jvm_args.txt"))) {
                String s = l.strip();
                if (s.startsWith("-Xmx")) {
                    String v = s.substring(4).toUpperCase();
                    long n = Long.parseLong(v.replaceAll("[^0-9]", ""));
                    return (int) (v.endsWith("M") ? n / 1024 : v.endsWith("K") ? n / (1024 * 1024) : n);
                }
            }
        } catch (IOException | NumberFormatException e) {
            // fall through
        }
        return 0;
    }

    static void setMemoryGb(Path dir, int gb) throws IOException {
        Path f = dir.resolve("user_jvm_args.txt");
        List<String> lines = Files.isRegularFile(f) ? new ArrayList<>(Files.readAllLines(f)) : new ArrayList<>();
        boolean xmx = false;
        boolean xms = false;
        int min = Math.max(2, Math.min(4, gb / 2));
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i).strip();
            if (s.startsWith("-Xmx")) {
                lines.set(i, "-Xmx" + gb + "G");
                xmx = true;
            } else if (s.startsWith("-Xms")) {
                lines.set(i, "-Xms" + min + "G");
                xms = true;
            }
        }
        if (!xms) {
            lines.add("-Xms" + min + "G");
        }
        if (!xmx) {
            lines.add("-Xmx" + gb + "G");
        }
        Files.write(f, lines, StandardCharsets.UTF_8);
    }
}
