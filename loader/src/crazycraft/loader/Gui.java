package crazycraft.loader;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** The setup window. */
final class Gui implements Installer.Listener {
    private static final String[] FUN = {
            "Feeding the Girlfriend", "Sharpening Big Bertha", "Waking up Mobzilla", "Counting rubies",
            "Teaching the rats some manners", "Growing King trees, one per trunk", "Polishing the Royal Guardian armor",
            "Asking the Kraken nicely", "Filling the uranium belts", "Herding Brutalflies", "Stacking crystal apples",
            "Warming up the Nightmare Rookery", "Oiling the chainsaw", "Hiding the Emperor Scorpion"};

    private final Manifest manifest;
    private Path dir;
    final JPanel root = new JPanel(new BorderLayout());
    private JFrame frame;

    private final Theme.StepList steps = new Theme.StepList();
    private final Theme.GlowBar bar = new Theme.GlowBar();
    private final JLabel headline = label("", Font.BOLD, 21f, Theme.TEXT);
    private final JLabel stats = label(" ", Font.PLAIN, 13f, Theme.MUTED);
    private final JLabel current = label(" ", Font.PLAIN, 12.5f, Theme.DIM);
    private final JLabel fun = label(" ", Font.ITALIC, 13f, new Color(0xd9b76a));
    private final JLabel banner = label(" ", Font.BOLD, 12.5f, Theme.BLUE);
    private final JTextArea log = new JTextArea();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Theme.TextureButton primary = new Theme.TextureButton("Install server", true);
    private final Theme.TextureButton credits = new Theme.TextureButton("Mods & credits", false);
    private final Theme.TextureButton openFolder = new Theme.TextureButton("Open folder", false);
    private final JLabel folderLabel = label("", Font.PLAIN, 12f, Theme.MUTED);
    private final JLabel changeFolder = link("Change");
    private final JSlider memory = new JSlider(4, 16, 8);
    private final JLabel memoryValue = label("8 GB", Font.BOLD, 13f, Theme.GOLD_LIGHT);
    private final JCheckBox eulaStart = checkbox("I accept the Minecraft EULA");
    private final JCheckBox eulaDone = checkbox("I accept the Minecraft EULA");
    private final JPanel manualRows = new JPanel();
    private final JLabel manualTitle = label("", Font.BOLD, 17f, Theme.YELLOW);
    private final JLabel doneTitle = label("", Font.BOLD, 19f, Theme.GREEN);
    private final JLabel doneText = label(" ", Font.PLAIN, 13f, Theme.MUTED);
    private final JLabel problemText = paragraph("");
    private final JLabel problemTitle = label("", Font.BOLD, 18f, Theme.FAIL_RED);
    private final ExecutorService background = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "setup-background");
        t.setDaemon(true);
        return t;
    });

    private Timer anim;
    private int funIndex = (int) (System.nanoTime() % FUN.length);
    private long funSince;
    private volatile Installer installer;
    private volatile boolean working;
    private boolean ready;
    private List<Manifest.Mod> manualMissing = new ArrayList<>();
    private CountDownLatch manualLatch;
    private Timer manualPoll;
    private long lastBytes;
    private long lastTime;
    private double speed;

    Gui(Manifest manifest, Path dir) {
        this.manifest = manifest;
        this.dir = dir;
        build();
        refreshIdle();
    }

    // ---- entry points ----

    static void open(Manifest manifest, Path dir) {
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui(manifest, dir);
            JFrame f = new JFrame(manifest.packName() + " Server Setup");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(gui.root);
            f.setIconImages(Theme.ICONS.stream().filter(i -> i != null).toList());
            f.setSize(1000, 790);
            f.setMinimumSize(new Dimension(940, 760));
            f.setLocationRelativeTo(null);
            gui.frame = f;
            gui.startAnimation();
            gui.checkForUpdate();
            f.setVisible(true);
        });
    }

    /** Renders the window into a PNG without ever showing it, for the docs and for checking the layout. */
    static void screenshot(Manifest manifest, Path dir, String demo, Path out) throws Exception {
        BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            Gui gui = new Gui(manifest, dir);
            gui.demo(demo);
            img[0] = gui.render(1000, 790);
        });
        ImageIO.write(img[0], "png", out.toFile());
    }

    BufferedImage render(int w, int h) {
        root.setSize(w, h);
        invalidateTree(root);
        layoutTree(root);
        layoutTree(root);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        root.paint(g);
        g.dispose();
        return img;
    }

    private static void invalidateTree(Component c) {
        c.invalidate();
        if (c instanceof Container ct) {
            for (Component k : ct.getComponents()) {
                invalidateTree(k);
            }
        }
    }

    private static void layoutTree(Component c) {
        if (c instanceof Container ct) {
            ct.doLayout();
            for (Component k : ct.getComponents()) {
                layoutTree(k);
            }
        }
    }

    // ---- layout ----

    private void build() {
        root.setBackground(Theme.BG);
        root.add(new Theme.Header(("SERVER SETUP  ·  PACK " + manifest.packVersion() + "  ·  MINECRAFT "
                + manifest.minecraft()).toUpperCase(Locale.ROOT)), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.add(steps, BorderLayout.WEST);

        JPanel main = new JPanel();
        main.setBackground(Theme.BG);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(16, 26, 12, 26));
        banner.setVisible(false);
        banner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        for (JComponent c : new JComponent[]{banner, headline, bar, stats, current, fun}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setMinimumSize(new Dimension(100, 22));
        main.add(banner);
        main.add(headline);
        main.add(Box.createVerticalStrut(10));
        main.add(bar);
        main.add(Box.createVerticalStrut(8));
        main.add(stats);
        main.add(Box.createVerticalStrut(2));
        main.add(current);
        main.add(Box.createVerticalStrut(2));
        main.add(fun);
        main.add(Box.createVerticalStrut(12));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.add(startCard(), "start");
        cards.add(workingCard(), "working");
        cards.add(manualCard(), "manual");
        cards.add(doneCard(), "done");
        cards.add(problemCard(), "problem");
        main.add(cards);
        body.add(main, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Theme.BG);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(Theme.mono(11.5f));
        log.setBackground(Theme.LOG_BG);
        log.setForeground(Theme.MUTED);
        log.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setPreferredSize(new Dimension(900, 112));
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        scroll.getViewport().setBackground(Theme.LOG_BG);
        scroll.getVerticalScrollBar().setUI(new Theme.ScrollLook());
        scroll.getHorizontalScrollBar().setUI(new Theme.ScrollLook());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        log.setText("Setup log: every download, check and fix shows up here and in .crazycraft/loader.log.");
        bottom.add(scroll, BorderLayout.NORTH);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 18));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 11));
        left.setOpaque(false);
        left.add(folderLabel);
        left.add(changeFolder);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(credits);
        right.add(openFolder);
        right.add(primary);
        footer.add(left, BorderLayout.CENTER);
        footer.add(right, BorderLayout.EAST);
        bottom.add(footer, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        primary.addActionListener(e -> onPrimary());
        credits.addActionListener(e -> showCredits());
        openFolder.addActionListener(e -> browse(dir.toUri()));
        changeFolder.addMouseListener(click(e -> chooseFolder()));
        banner.addMouseListener(click(e -> browse(URI.create("https://github.com/" + manifest.repo() + "/releases/latest"))));
        memory.addChangeListener(e -> memoryValue.setText(memory.getValue() + " GB"));
        eulaStart.addActionListener(e -> eulaDone.setSelected(eulaStart.isSelected()));
        eulaDone.addActionListener(e -> {
            eulaStart.setSelected(eulaDone.isSelected());
            refreshPrimary();
        });
        root.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport s) {
                return installer != null && !manualMissing.isEmpty() && s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport s) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    offerFiles(files);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private JComponent startCard() {
        Theme.Card c = new Theme.Card(Theme.BORDER);
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        List<Manifest.Mod> server = manifest.serverMods();
        long patched = server.stream().filter(m -> m.patch() != null).count();
        long byHand = server.stream().filter(Manifest.Mod::manual).count();
        long bytes = server.stream().mapToLong(Manifest.Mod::size).sum();
        c.add(label("Everything this server needs, straight from the source", Font.BOLD, 16f, Theme.TEXT), g);
        g.gridy++;
        g.insets = new Insets(8, 0, 0, 0);
        c.add(label(server.size() + " mods  ·  " + patched + " fixed for the pack  ·  " + byHand + " by hand  ·  about "
                + ConsoleUi.mb(bytes), Font.BOLD, 13f, Theme.GOLD_LIGHT), g);
        g.gridy++;
        c.add(paragraph("Each mod is downloaded from its official page (Modrinth, CurseForge or the author's GitHub) and "
                + "checked before it's used, so the pack never hands out anyone else's files. Worlds and settings in "
                + "this folder are left alone."), g);
        g.gridy++;
        g.insets = new Insets(14, 0, 0, 0);
        JPanel mem = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        mem.setOpaque(false);
        JLabel ml = label("Server memory", Font.BOLD, 13f, Theme.TEXT);
        ml.setPreferredSize(new Dimension(130, 24));
        mem.add(ml);
        memory.setOpaque(false);
        memory.setUI(new Theme.SliderLook(memory));
        memory.setPreferredSize(new Dimension(300, 24));
        memory.setMajorTickSpacing(2);
        memory.setSnapToTicks(false);
        memory.setForeground(Theme.MUTED);
        mem.add(memory);
        mem.add(Box.createHorizontalStrut(12));
        mem.add(memoryValue);
        c.add(mem, g);
        g.gridy++;
        g.insets = new Insets(8, 0, 0, 0);
        c.add(eulaRow(eulaStart), g);
        topAlign(c, g);
        return c;
    }

    private JComponent workingCard() {
        Theme.Card c = new Theme.Card(Theme.BORDER);
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        c.add(label("Hang tight", Font.BOLD, 16f, Theme.TEXT), g);
        g.gridy++;
        g.insets = new Insets(6, 0, 0, 0);
        c.add(paragraph("Setup retries a download that stalls and checks every file before it goes into mods/. "
                + "Jars that are no longer part of the pack are moved to mods-removed/, never deleted."), g);
        topAlign(c, g);
        return c;
    }

    private JComponent manualCard() {
        Theme.Card c = new Theme.Card(new Color(0x8a6d1c));
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        c.add(manualTitle, g);
        g.gridy++;
        g.insets = new Insets(6, 0, 0, 0);
        c.add(paragraph("Its author only allows downloads from CurseForge itself. Open the page, download the file, and "
                + "setup picks it up from your Downloads folder by itself. You can also drop the file on this window."), g);
        g.gridy++;
        g.insets = new Insets(10, 0, 0, 0);
        manualRows.setOpaque(false);
        manualRows.setLayout(new BoxLayout(manualRows, BoxLayout.Y_AXIS));
        c.add(manualRows, g);
        g.gridy++;
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.setOpaque(false);
        JLabel choose = link("Choose the file...");
        choose.addMouseListener(click(e -> chooseManualFile()));
        JLabel skip = link("Skip for now");
        skip.addMouseListener(click(e -> finishManual()));
        links.add(choose);
        links.add(Box.createHorizontalStrut(24));
        links.add(skip);
        c.add(links, g);
        topAlign(c, g);
        return c;
    }

    private JComponent doneCard() {
        Theme.Card c = new Theme.Card(new Color(0x3f6b2a));
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        c.add(doneTitle, g);
        g.gridy++;
        g.insets = new Insets(6, 0, 0, 0);
        c.add(doneText, g);
        g.gridy++;
        c.add(paragraph("Players join with the " + manifest.packName() + " v" + manifest.packVersion()
                + " client. The server's own window has the console; type stop there to shut it down."), g);
        g.gridy++;
        g.insets = new Insets(10, 0, 0, 0);
        c.add(eulaRow(eulaDone), g);
        topAlign(c, g);
        return c;
    }

    private JComponent problemCard() {
        Theme.Card c = new Theme.Card(new Color(0x7a2a1c));
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        c.add(problemTitle, g);
        g.gridy++;
        g.insets = new Insets(6, 0, 0, 0);
        c.add(problemText, g);
        topAlign(c, g);
        return c;
    }

    private JComponent eulaRow(JCheckBox box) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(box);
        p.add(Box.createHorizontalStrut(10));
        JLabel read = link("Read it");
        read.addMouseListener(click(e -> browse(URI.create("https://aka.ms/MinecraftEULA"))));
        p.add(read);
        return p;
    }

    /** Pushes a card's rows to the top. */
    private static void topAlign(JComponent card, GridBagConstraints g) {
        g.gridy++;
        g.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        card.add(filler, g);
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    // ---- state ----

    private void refreshIdle() {
        folderLabel.setText("Server folder:  " + Theme.ellipsize(folderLabel.getFontMetrics(folderLabel.getFont()),
                dir.toString(), 360));
        int mem = Installer.memoryGb(dir);
        long max = Math.max(4, Math.min(32, physicalGb() - 2));
        memory.setMaximum((int) max);
        memory.setValue(mem > 0 ? Math.min(mem, (int) max) : (int) Math.min(8, max));
        memoryValue.setText(memory.getValue() + " GB");
        boolean eula = Installer.eulaAccepted(dir);
        eulaStart.setSelected(eula);
        eulaDone.setSelected(eula);
        String installedVersion = installedVersion();
        if (installedVersion == null) {
            headline.setText("Ready to set up your server");
            stats.setText("NeoForge " + manifest.neoforge().version() + " and " + manifest.serverMods().size()
                    + " mods will be installed in this folder.");
            primary.setText("Install server");
        } else if (!installedVersion.equals(manifest.packVersion())) {
            headline.setText("Update from v" + installedVersion + " to v" + manifest.packVersion());
            stats.setText("Only the mods that changed are downloaded.");
            primary.setText("Update to v" + manifest.packVersion());
        } else {
            headline.setText("v" + manifest.packVersion() + " is installed here");
            stats.setText("Check every mod and fetch anything missing, or start the server.");
            primary.setText("Check files");
        }
        current.setText(" ");
        fun.setText(" ");
        bar.set(0, false);
        show("start");
    }

    private String installedVersion() {
        try {
            Path f = dir.resolve(".crazycraft/state.json");
            if (Files.isRegularFile(f)) {
                return Json.str(Json.obj(Json.parse(Files.readString(f, StandardCharsets.UTF_8))), "pack");
            }
        } catch (Exception ignored) {
            // treat as a fresh folder
        }
        return null;
    }

    private void refreshPrimary() {
        if (ready) {
            primary.setText("Start server");
            primary.setEnabled(eulaDone.isSelected());
        }
    }

    private void onPrimary() {
        if (working) {
            return;
        }
        if (ready) {
            startServer();
            return;
        }
        try {
            Installer.setMemoryGb(dir, memory.getValue());
            if (eulaStart.isSelected()) {
                Installer.acceptEula(dir);
            }
        } catch (IOException e) {
            appendLog("Can't write to " + dir + ": " + e.getMessage());
            return;
        }
        working = true;
        ready = false;
        steps.reset();
        primary.setText("Working...");
        primary.setEnabled(false);
        changeFolder.setVisible(false);
        headline.setText("Setting up");
        show("working");
        speed = 0;
        lastTime = System.currentTimeMillis();
        lastBytes = 0;
        Thread t = new Thread(() -> {
            Installer ins = new Installer(dir, manifest, this);
            installer = ins;
            Installer.Result r = ins.run();
            SwingUtilities.invokeLater(() -> finished(r));
        }, "setup");
        t.setDaemon(true);
        t.start();
    }

    private void finished(Installer.Result r) {
        working = false;
        installer = null;
        changeFolder.setVisible(true);
        primary.setEnabled(true);
        current.setText(" ");
        fun.setText(" ");
        switch (r) {
            case READY -> {
                ready = true;
                bar.set(1, false);
                headline.setText("Your server is ready");
                stats.setText(manifest.serverMods().size() + " mods in place  ·  NeoForge " + manifest.neoforge().version());
                doneTitle.setText("Ready to play!");
                doneText.setText(manifest.packName() + " v" + manifest.packVersion() + "  ·  Minecraft " + manifest.minecraft()
                        + "  ·  " + manifest.serverMods().size() + " mods");
                eulaDone.setSelected(Installer.eulaAccepted(dir) || eulaStart.isSelected());
                if (eulaDone.isSelected() && !Installer.eulaAccepted(dir)) {
                    try {
                        Installer.acceptEula(dir);
                    } catch (IOException ignored) {
                        // asked again at start
                    }
                }
                show("done");
                refreshPrimary();
            }
            case INCOMPLETE -> {
                headline.setText("Almost there");
                problemTitle.setText("A mod still has to be downloaded by hand");
                problemTitle.setForeground(Theme.YELLOW);
                setParagraph(problemText, "The server can't start without it. Download it (see the log), then press Check files.", 470);
                show("problem");
                primary.setText("Check files");
            }
            default -> {
                headline.setText("Setup couldn't finish");
                problemTitle.setText("Something got in the way");
                problemTitle.setForeground(Theme.FAIL_RED);
                setParagraph(problemText, "Check your internet connection and press Try again; files that are "
                        + "already here are kept. The details are in the log below and in .crazycraft/loader.log.", 470);
                show("problem");
                primary.setText("Try again");
            }
        }
    }

    // ---- Installer.Listener (called from the setup threads) ----

    @Override
    public void log(String line) {
        SwingUtilities.invokeLater(() -> appendLog(line));
    }

    private void appendLog(String line) {
        log.append((log.getDocument().getLength() == 0 ? "" : "\n") + line);
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public void step(Installer.Step step, Installer.State state, String detail) {
        SwingUtilities.invokeLater(() -> {
            steps.set(step, state, detail);
            if (state == Installer.State.RUNNING) {
                headline.setText(switch (step) {
                    case JAVA -> "Checking Java";
                    case NEOFORGE -> "Installing NeoForge";
                    case MODS -> "Getting the mods";
                    case FILES -> "Adding extra files";
                    case TIDY -> "Tidying up";
                });
                if (step != Installer.Step.MODS) {
                    stats.setText(detail.isEmpty() ? " " : detail);
                }
            }
        });
    }

    @Override
    public void progress(long bytesDone, long bytesTotal, int filesDone, int filesTotal, String file) {
        SwingUtilities.invokeLater(() -> {
            long now = System.currentTimeMillis();
            if (now - lastTime > 700) {
                double inst = (bytesDone - lastBytes) * 1000.0 / (now - lastTime);
                speed = speed == 0 ? inst : speed * 0.7 + inst * 0.3;
                lastBytes = bytesDone;
                lastTime = now;
            }
            bar.set(bytesTotal > 0 ? bytesDone / (double) bytesTotal : 1, true);
            stats.setText(filesDone + " of " + filesTotal + " mods  ·  " + ConsoleUi.mb(bytesDone) + " of "
                    + ConsoleUi.mb(bytesTotal) + (speed > 0 ? String.format(Locale.ROOT, "  ·  %.1f MB/s", speed / 1048576) : ""));
            current.setText(file);
        });
    }

    @Override
    public void manual(List<Manifest.Mod> missing, Installer ins) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> showManual(missing, latch));
        latch.await();
    }

    private void showManual(List<Manifest.Mod> missing, CountDownLatch latch) {
        manualMissing = new ArrayList<>(missing);
        manualLatch = latch;
        headline.setText(missing.size() == 1 ? "One mod needs a download by hand" : missing.size() + " mods need a download by hand");
        manualTitle.setText(missing.size() == 1 ? missing.get(0).name() + " needs a quick download by hand"
                : missing.size() + " mods need a quick download by hand");
        manualRows.removeAll();
        for (Manifest.Mod m : missing) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
            JPanel names = new JPanel(new GridBagLayout());
            names.setOpaque(false);
            GridBagConstraints g = gbc();
            names.add(label(m.name(), Font.BOLD, 14f, Theme.TEXT), g);
            g.gridy++;
            names.add(label(m.file() + "  ·  " + ConsoleUi.mb(m.size()), Font.PLAIN, 12f, Theme.MUTED), g);
            row.add(names, BorderLayout.CENTER);
            Theme.TextureButton open = new Theme.TextureButton("Open download page", false);
            open.addActionListener(e -> browse(URI.create(m.source().download() != null ? m.source().download() : m.source().page())));
            row.add(open, BorderLayout.EAST);
            manualRows.add(row);
        }
        JLabel waiting = label("Watching " + String.join(" and ", placesText()) + " for the file...", Font.ITALIC, 12.5f,
                new Color(0xd9b76a));
        waiting.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));
        waiting.setAlignmentX(Component.LEFT_ALIGNMENT);
        manualRows.add(waiting);
        show("manual");
        if (manualPoll != null) {
            manualPoll.stop();
        }
        manualPoll = new Timer(2000, e -> pollManual());
        manualPoll.start();
    }

    private List<String> placesText() {
        List<String> out = new ArrayList<>();
        out.add("manual/");
        out.add("your Downloads folder");
        return out;
    }

    private void pollManual() {
        Installer ins = installer;
        if (ins == null || manualMissing.isEmpty()) {
            return;
        }
        List<Manifest.Mod> snapshot = List.copyOf(manualMissing);
        background.submit(() -> {
            try {
                List<Manifest.Mod> left = ins.scanManual(snapshot);
                SwingUtilities.invokeLater(() -> {
                    manualMissing = new ArrayList<>(left);
                    if (left.isEmpty()) {
                        finishManual();
                    }
                });
            } catch (IOException e) {
                log("Could not look for the file: " + e.getMessage());
            }
        });
    }

    private void offerFiles(List<File> files) {
        Installer ins = installer;
        if (ins == null) {
            return;
        }
        List<Manifest.Mod> snapshot = List.copyOf(manualMissing);
        background.submit(() -> {
            for (File f : files) {
                try {
                    Manifest.Mod m = ins.offer(f.toPath(), snapshot);
                    if (m == null) {
                        log(f.getName() + " is not the file setup is waiting for.");
                    }
                } catch (IOException e) {
                    log("Could not read " + f + ": " + e.getMessage());
                }
            }
            SwingUtilities.invokeLater(this::pollManual);
        });
    }

    private void chooseManualFile() {
        JFileChooser fc = new JFileChooser(Path.of(System.getProperty("user.home"), "Downloads").toFile());
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            offerFiles(List.of(fc.getSelectedFiles()));
        }
    }

    private void finishManual() {
        if (manualPoll != null) {
            manualPoll.stop();
        }
        manualMissing = new ArrayList<>();
        show("working");
        if (manualLatch != null) {
            manualLatch.countDown();
            manualLatch = null;
        }
    }

    // ---- actions ----

    private void startServer() {
        if (!eulaDone.isSelected()) {
            return;
        }
        try {
            Installer.acceptEula(dir);
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            if (windows && Files.isRegularFile(dir.resolve("startserver.bat"))) {
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", manifest.packName() + " Server",
                        "cmd.exe", "/k", "startserver.bat").directory(dir.toFile());
                pb.environment().put("CRAZYCRAFT_SKIP_SETUP", "true");
                pb.start();
                headline.setText("The server is starting in its own window");
                stats.setText("It takes a minute or two. You can close setup now.");
                appendLog("Started startserver.bat in a new window.");
            } else {
                headline.setText("Start the server from a terminal");
                stats.setText("Run ./startserver.sh in " + dir);
            }
        } catch (IOException e) {
            appendLog("Could not start the server: " + e.getMessage());
        }
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser(dir.toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            dir = fc.getSelectedFile().toPath();
            ready = false;
            refreshIdle();
        }
    }

    private void showCredits() {
        String[] cols = {"Mod", "Version", "Downloaded from", "Note"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        List<Manifest.Mod> list = new ArrayList<>(manifest.serverMods());
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        for (Manifest.Mod m : list) {
            String note = m.patch() != null ? "Fixed for the pack: " + m.patch().why()
                    : m.manual() ? m.source().note() : "";
            model.addRow(new Object[]{m.name(), m.version(), m.source().label(), note});
        }
        JTable table = new JTable(model);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setGridColor(Theme.PANEL_2);
        table.setSelectionBackground(new Color(0x4a3418));
        table.setSelectionForeground(Theme.GOLD_LIGHT);
        table.setRowHeight(24);
        table.setFont(Theme.font(Font.PLAIN, 12.5f));
        table.getTableHeader().setBackground(Theme.PANEL_2);
        table.getTableHeader().setForeground(Theme.GOLD_LIGHT);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12.5f));
        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.setDefaultRenderer(Object.class, r);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    Manifest.Mod m = list.get(table.convertRowIndexToModel(table.getSelectedRow()));
                    browse(URI.create(m.source().page()));
                }
            }
        });
        JDialog d = new JDialog(frame, "Mods & credits", true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel intro = paragraph(manifest.packName() + " doesn't host other people's mods. Each one is downloaded "
                + "from its official page, which is also where its license and support live. Double-click a mod to open "
                + "its page. Thanks to every author in this list.");
        setParagraph(intro, manifest.packName() + " doesn't host other people's mods. Each one is downloaded from its "
                + "official page, which is also where its license and support live. Double-click a mod to open its page. "
                + "Thanks to every author in this list.", 860);
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(intro, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.PANEL);
        sp.getVerticalScrollBar().setUI(new Theme.ScrollLook());
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        p.add(sp, BorderLayout.CENTER);
        d.setContentPane(p);
        d.setSize(960, 600);
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }

    private void checkForUpdate() {
        background.submit(() -> {
            try {
                String body = Download.text("https://api.github.com/repos/" + manifest.repo() + "/releases/latest");
                String tag = Json.str(Json.obj(Json.parse(body)), "tag_name");
                if (tag != null && newer(tag.replaceFirst("^v", ""), manifest.packVersion())) {
                    SwingUtilities.invokeLater(() -> {
                        banner.setText(manifest.packName() + " " + tag + " is out. Click here for the new server files.");
                        banner.setVisible(true);
                    });
                }
            } catch (Exception ignored) {
                // offline or rate limited: no banner
            }
        });
    }

    static boolean newer(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0;
            int q = i < y.length ? num(y[i]) : 0;
            if (p != q) {
                return p > q;
            }
        }
        return false;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void startAnimation() {
        anim = new Timer(40, e -> {
            if (working) {
                steps.repaint();
                bar.repaint();
                if (System.currentTimeMillis() - funSince > 2600) {
                    funSince = System.currentTimeMillis();
                    funIndex = (funIndex + 1) % FUN.length;
                    fun.setText(FUN[funIndex] + "...");
                }
            }
        });
        anim.start();
    }

    private void browse(URI uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (IOException | UnsupportedOperationException e) {
            appendLog("Open this link in your browser: " + uri);
        }
    }

    private static long physicalGb() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalMemorySize() / (1024L * 1024 * 1024);
        } catch (Throwable t) {
            return 16;
        }
    }

    // ---- small widgets ----

    static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    static JLabel link(String text) {
        JLabel l = label("<html><u>" + text + "</u></html>", Font.BOLD, 12.5f, Theme.BLUE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    static JLabel paragraph(String text) {
        JLabel l = new JLabel();
        l.setFont(Theme.font(Font.PLAIN, 13f));
        l.setForeground(Theme.MUTED);
        setParagraph(l, text, 470);
        return l;
    }

    /** Fixed-width HTML wraps the same way in the window and in an off-screen render. */
    static void setParagraph(JLabel l, String text, int width) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        l.setText("<html><div style='width:" + width + "px'>" + safe + "</div></html>");
    }

    static JCheckBox checkbox(String text) {
        JCheckBox b = new JCheckBox(text);
        b.setOpaque(false);
        b.setFont(Theme.font(Font.BOLD, 13f));
        b.setForeground(Theme.TEXT);
        b.setFocusPainted(false);
        b.setIcon(new Theme.CheckIcon());
        b.setSelectedIcon(new Theme.CheckIcon());
        b.setRolloverEnabled(true);
        b.setIconTextGap(8);
        return b;
    }

    static MouseAdapter click(Consumer<MouseEvent> action) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.accept(e);
            }
        };
    }

    // ---- self test: a real install driven through this window, rendered off screen ----

    private volatile String shown = "start";

    private void show(String card) {
        shown = card;
        cardLayout.show(cards, card);
    }

    /**
     * Runs a real setup through the window without showing it and saves what it looks like along the way. When the
     * by-hand card comes up, the given file is copied into manual/, as a player would save it there.
     */
    static int selfTest(Manifest manifest, Path dir, Path out, Path manualFile) throws Exception {
        Files.createDirectories(out);
        Gui[] holder = new Gui[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new Gui(manifest, dir);
            holder[0].eulaStart.setSelected(true);
            holder[0].onPrimary();
        });
        Gui gui = holder[0];
        long start = System.currentTimeMillis();
        long nextShot = 0;
        int shot = 0;
        boolean manualSeen = false;
        while (System.currentTimeMillis() - start < 45 * 60_000L) {
            Thread.sleep(1000);
            String card = gui.shown;
            boolean done = !gui.working && ("done".equals(card) || "problem".equals(card));
            if ("manual".equals(card) && !manualSeen) {
                manualSeen = true;
                save(gui, out.resolve("live_manual.png"));
                Thread.sleep(3000);
                if (manualFile != null) {
                    Files.copy(manualFile, gui.dir.resolve("manual").resolve(manualFile.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("selftest: copied " + manualFile.getFileName() + " into manual/");
                }
            }
            if (System.currentTimeMillis() - start >= nextShot && !done) {
                save(gui, out.resolve(String.format(Locale.ROOT, "live_%02d.png", shot++)));
                nextShot += 15_000;
            }
            if (done) {
                Thread.sleep(500);
                save(gui, out.resolve("live_final.png"));
                System.out.println("selftest: finished on the " + card + " card, ready=" + gui.ready
                        + ", manual card seen=" + manualSeen);
                return "done".equals(card) && gui.ready ? 0 : 1;
            }
        }
        System.out.println("selftest: timed out on the " + gui.shown + " card");
        return 2;
    }

    private static void save(Gui gui, Path file) throws Exception {
        BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> img[0] = gui.render(1000, 790));
        ImageIO.write(img[0], "png", file.toFile());
    }

    // ---- demo states for screenshots ----

    private void demo(String state) {
        switch (state) {
            case "running" -> {
                steps.set(Installer.Step.JAVA, Installer.State.DONE, "Java 21.0.7 (Microsoft)");
                steps.set(Installer.Step.NEOFORGE, Installer.State.DONE, "NeoForge " + manifest.neoforge().version() + " installed");
                steps.set(Installer.Step.MODS, Installer.State.RUNNING, "Downloaded 86 of 163");
                headline.setText("Getting the mods");
                bar.set(0.53, true);
                stats.setText("86 of 163 mods  ·  412 MB of 782 MB  ·  9.6 MB/s");
                current.setText("twilightforest-1.21.1-4.8.3345-universal.jar");
                fun.setText("Waking up Mobzilla...");
                primary.setText("Working...");
                primary.setEnabled(false);
                changeFolder.setVisible(false);
                show("working");
                for (String l : new String[]{"CrazyCraft " + manifest.packVersion() + " server setup in " + dir,
                        "NeoForge " + manifest.neoforge().version() + " installed.",
                        "0 mods already in place, 162 to download, 1 by hand",
                        "Got jei-1.21.1-neoforge-19.44.0.399.jar from Modrinth",
                        "Got [1.21.1] SecurityCraft v1.10.1.jar from Modrinth and applied the pack's fix: SecurityCraft-1.21.1-v1.10.1.jar",
                        "Got ProjectE-1.21.1-PE1.1.0.jar from CurseForge",
                        "Got orespawn-1.21.1-2.0.0-beta.12.jar from GitHub"}) {
                    appendLog(l);
                }
            }
            case "manual" -> {
                steps.set(Installer.Step.JAVA, Installer.State.DONE, "Java 21.0.7 (Microsoft)");
                steps.set(Installer.Step.NEOFORGE, Installer.State.DONE, "NeoForge " + manifest.neoforge().version() + " installed");
                steps.set(Installer.Step.MODS, Installer.State.WARNING, "1 mod needs a download by hand");
                bar.set(1, false);
                stats.setText("162 of 162 mods  ·  706 MB of 706 MB");
                primary.setText("Working...");
                primary.setEnabled(false);
                changeFolder.setVisible(false);
                appendLog("Got twilightforest-1.21.1-4.8.3345-universal.jar from CurseForge");
                appendLog("Got orespawn_integrations-0.9.0.jar from GitHub");
                showManual(manifest.serverMods().stream().filter(Manifest.Mod::manual).toList(), new CountDownLatch(1));
                if (manualPoll != null) {
                    manualPoll.stop();
                }
            }
            case "done" -> {
                for (Installer.Step s : Installer.Step.values()) {
                    steps.set(s, Installer.State.DONE, switch (s) {
                        case JAVA -> "Java 21.0.7 (Microsoft)";
                        case NEOFORGE -> "NeoForge " + manifest.neoforge().version() + " installed";
                        case MODS -> manifest.serverMods().size() + " mods ready (" + manifest.serverMods().size() + " new)";
                        case FILES -> "1 of 1 in place";
                        case TIDY -> "Nothing to tidy";
                    });
                }
                appendLog("Found MCHeli at " + System.getProperty("user.home") + File.separator + "Downloads"
                        + File.separator + "MCHeli-1.21.1-1.3.0.jar and copied it into mods/");
                appendLog("Added mcheli/orespawn_uranium/models/planes/a-10_du.mqo (The uranium A-10 uses MCHeli's own A-10 model)");
                appendLog("All set: " + manifest.serverMods().size() + " mods ready.");
                eulaStart.setSelected(true);
                finished(Installer.Result.READY);
            }
            default -> {
            }
        }
    }

    Map<String, Object> debugState() {
        return Map.of("working", working, "ready", ready);
    }
}
