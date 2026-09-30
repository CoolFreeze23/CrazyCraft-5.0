package crazycraft.loader;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;

/** The setup window's look: colors from the pack's logo and title-screen buttons, and the custom-painted parts. */
final class Theme {
    static final Color BG = new Color(0x120d0a);
    static final Color PANEL = new Color(0x1b1410);
    static final Color PANEL_2 = new Color(0x241a13);
    static final Color BORDER = new Color(0x5a3d1c);
    static final Color GOLD = new Color(0xf0c010);
    static final Color GOLD_LIGHT = new Color(0xffd94a);
    static final Color ORANGE = new Color(0xea7a22);
    static final Color RED = new Color(0xd40707);
    static final Color TEXT = new Color(0xf1e7d6);
    static final Color MUTED = new Color(0xb3a393);
    static final Color DIM = new Color(0x6f6257);
    static final Color GREEN = new Color(0x74d06a);
    static final Color YELLOW = new Color(0xf5c542);
    static final Color FAIL_RED = new Color(0xef5a44);
    static final Color BLUE = new Color(0x8fd3ff);
    static final Color LOG_BG = new Color(0x0c0907);

    static final String SANS = pick("Segoe UI", "Inter", "Helvetica Neue", "Arial", Font.SANS_SERIF);
    static final String MONO = pick("Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono", Font.MONOSPACED);

    static final BufferedImage KEYART = image("keyart.jpg");
    static final BufferedImage LOGO = image("logo.png");
    static final BufferedImage BUTTON = image("button.png");
    static final BufferedImage BUTTON_HOVER = image("button_hover.png");
    static final List<BufferedImage> ICONS = Arrays.asList(image("icon16.png"), image("icon32.png"), image("icon64.png"));

    private Theme() {
    }

    static Font font(int style, float size) {
        return new Font(SANS, style, 12).deriveFont(size);
    }

    static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, 12).deriveFont(size);
    }

    private static String pick(String... names) {
        List<String> have = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : names) {
            if (have.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }

    private static BufferedImage image(String name) {
        try (InputStream in = Theme.class.getResourceAsStream("/crazycraft/loader/" + name)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g2;
    }

    /** Draws text with a little letter spacing, the way the pack's title screen labels look. */
    static void spaced(Graphics2D g, String s, int x, int y, float spacing) {
        FontMetrics fm = g.getFontMetrics();
        float cx = x;
        for (char c : s.toCharArray()) {
            g.drawString(String.valueOf(c), cx, y);
            cx += fm.charWidth(c) + spacing;
        }
    }

    static int spacedWidth(FontMetrics fm, String s, float spacing) {
        return (int) (fm.stringWidth(s) + spacing * Math.max(0, s.length() - 1));
    }

    static String ellipsize(FontMetrics fm, String s, int width) {
        if (fm.stringWidth(s) <= width) {
            return s;
        }
        String dots = "...";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n) + dots) > width) {
            n--;
        }
        return s.substring(0, n) + dots;
    }

    /** The key art with the logo over it, as on the pack's title screen. */
    static final class Header extends JComponent {
        private final String subtitle;

        Header(String subtitle) {
            this.subtitle = subtitle;
            setPreferredSize(new Dimension(980, 188));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(BG);
            g2.fillRect(0, 0, w, h);
            if (KEYART != null) {
                int srcH = (int) (KEYART.getWidth() * (double) h / w);
                int srcY = Math.max(0, Math.min(KEYART.getHeight() - srcH, (int) (KEYART.getHeight() * 0.33)));
                g2.drawImage(KEYART, 0, 0, w, h, 0, srcY, KEYART.getWidth(), srcY + srcH, null);
            }
            g2.setPaint(new GradientPaint(0, 0, new Color(18, 13, 10, 90), 0, h, new Color(18, 13, 10, 250)));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new GradientPaint(0, h - 3, new Color(240, 192, 16, 0), 0, h, new Color(240, 192, 16, 90)));
            g2.fillRect(0, h - 3, w, 3);
            int logoH = 104;
            if (LOGO != null) {
                int logoW = LOGO.getWidth() * logoH / LOGO.getHeight();
                g2.drawImage(LOGO, (w - logoW) / 2, 18, logoW, logoH, null);
            }
            g2.setFont(font(Font.BOLD, 12f));
            int sw = spacedWidth(g2.getFontMetrics(), subtitle, 2.2f);
            int sy = 18 + logoH + 30;
            g2.setColor(new Color(0, 0, 0, 160));
            spaced(g2, subtitle, (w - sw) / 2 + 1, sy + 1, 2.2f);
            g2.setColor(GOLD_LIGHT);
            spaced(g2, subtitle, (w - sw) / 2, sy, 2.2f);
            g2.dispose();
        }
    }

    /** A button drawn with the pack's title-screen button texture: gold edges, red end caps. */
    static final class TextureButton extends JButton {
        private boolean hover;
        private final boolean primary;

        TextureButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(font(Font.BOLD, primary ? 15f : 13.5f));
            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(Math.max(primary ? 190 : 120, fm.stringWidth(getText()) + 44), 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            Composite old = g2.getComposite();
            if (!isEnabled()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            }
            BufferedImage tex = hover && isEnabled() ? BUTTON_HOVER : BUTTON;
            if (tex != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int cap = 6;
                int th = tex.getHeight();
                int tw = tex.getWidth();
                g2.drawImage(tex, 0, 0, cap, h, 0, 0, cap, th, null);
                g2.drawImage(tex, cap, 0, w - cap, h, cap, 0, tw - cap, th, null);
                g2.drawImage(tex, w - cap, 0, w, h, tw - cap, 0, tw, th, null);
            } else {
                g2.setColor(PANEL_2);
                g2.fillRect(0, 0, w, h);
                g2.setColor(GOLD);
                g2.drawRect(0, 0, w - 1, h - 1);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            int tx = (w - fm.stringWidth(t)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(t, tx + 1, ty + 1);
            g2.setColor(primary && isEnabled() ? GOLD_LIGHT : TEXT);
            g2.drawString(t, tx, ty);
            g2.setComposite(old);
            g2.dispose();
        }
    }

    /** The progress bar: the logo's gold-to-red gradient with a moving shine while it works. */
    static final class GlowBar extends JComponent {
        private double value;
        private boolean active;

        GlowBar() {
            setPreferredSize(new Dimension(400, 22));
        }

        void set(double v, boolean active) {
            this.value = Math.max(0, Math.min(1, v));
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D track = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h, h);
            g2.setColor(LOG_BG);
            g2.fill(track);
            int fw = (int) Math.round((w - 4) * value);
            if (fw > 0) {
                RoundRectangle2D fill = new RoundRectangle2D.Double(2, 2, Math.max(fw, h - 4), h - 4, h - 4, h - 4);
                g2.setPaint(new LinearGradientPaint(2, 0, Math.max(3, w - 2), 0, new float[]{0f, 0.55f, 1f},
                        new Color[]{GOLD_LIGHT, ORANGE, new Color(0xc8321c)}));
                g2.fill(fill);
                g2.setPaint(new GradientPaint(0, 2, new Color(255, 255, 255, 70), 0, h / 2f, new Color(255, 255, 255, 0)));
                g2.fill(fill);
                if (active) {
                    double phase = (System.currentTimeMillis() % 1800) / 1800.0;
                    int sx = (int) (-60 + phase * (fw + 120));
                    g2.setClip(fill);
                    g2.setPaint(new GradientPaint(sx, 0, new Color(255, 255, 255, 0), sx + 30, 0, new Color(255, 255, 255, 90),
                            true));
                    g2.fillRect(sx, 0, 60, h);
                    g2.setClip(null);
                }
            }
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(track);
            g2.dispose();
        }
    }

    /** The list of setup steps on the left, each with its state and a short detail. */
    static final class StepList extends JComponent {
        private final Map<Installer.Step, Installer.State> states = new EnumMap<>(Installer.Step.class);
        private final Map<Installer.Step, String> details = new EnumMap<>(Installer.Step.class);
        private final List<Installer.Step> shown;

        StepList(List<Installer.Step> shown) {
            this.shown = shown;
            for (Installer.Step s : Installer.Step.values()) {
                states.put(s, Installer.State.WAITING);
                details.put(s, "");
            }
            setPreferredSize(new Dimension(262, 330));
        }

        void set(Installer.Step s, Installer.State st, String detail) {
            states.put(s, st);
            if (detail != null && !detail.isEmpty()) {
                details.put(s, detail);
            }
            repaint();
        }

        void reset() {
            for (Installer.Step s : Installer.Step.values()) {
                states.put(s, Installer.State.WAITING);
                details.put(s, "");
            }
            repaint();
        }

        boolean running() {
            return states.containsValue(Installer.State.RUNNING);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(PANEL);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(BORDER);
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            g2.setFont(font(Font.BOLD, 11f));
            g2.setColor(DIM);
            spaced(g2, "SETUP STEPS", 22, 30, 1.8f);
            int y = 48;
            for (Installer.Step s : shown) {
                Installer.State st = states.get(s);
                icon(g2, st, 22, y + 3);
                g2.setFont(font(Font.BOLD, 14.5f));
                g2.setColor(st == Installer.State.WAITING ? DIM : TEXT);
                g2.drawString(s.title, 56, y + 16);
                g2.setFont(font(Font.PLAIN, 12f));
                g2.setColor(st == Installer.State.FAILED ? FAIL_RED : st == Installer.State.WARNING ? YELLOW : MUTED);
                String d = details.get(s);
                if (d.isEmpty() && st == Installer.State.WAITING) {
                    d = "Waiting";
                }
                g2.drawString(ellipsize(g2.getFontMetrics(), d, getWidth() - 70), 56, y + 34);
                y += 56;
            }
            g2.dispose();
        }

        static void icon(Graphics2D g2, Installer.State st, int x, int y) {
            int d = 22;
            Ellipse2D c = new Ellipse2D.Double(x, y, d, d);
            switch (st) {
                case DONE -> {
                    g2.setColor(GREEN);
                    g2.fill(c);
                    g2.setColor(new Color(0x10240e));
                    g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D p = new Path2D.Double();
                    p.moveTo(x + 6, y + 11.5);
                    p.lineTo(x + 9.8, y + 15.2);
                    p.lineTo(x + 16.5, y + 7.5);
                    g2.draw(p);
                }
                case RUNNING -> {
                    g2.setColor(new Color(240, 192, 16, 50));
                    g2.fill(c);
                    double a = (System.currentTimeMillis() % 1000) / 1000.0 * 360;
                    g2.setColor(GOLD);
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Arc2D.Double(x + 2, y + 2, d - 4, d - 4, -a, 260, Arc2D.OPEN));
                }
                case WARNING -> {
                    g2.setColor(YELLOW);
                    Path2D t = new Path2D.Double();
                    t.moveTo(x + d / 2.0, y + 1);
                    t.lineTo(x + d, y + d - 1);
                    t.lineTo(x, y + d - 1);
                    t.closePath();
                    g2.fill(t);
                    g2.setColor(new Color(0x2a2000));
                    g2.setFont(font(Font.BOLD, 13f));
                    g2.drawString("!", x + d / 2 - 2, y + d - 4);
                }
                case FAILED -> {
                    g2.setColor(FAIL_RED);
                    g2.fill(c);
                    g2.setColor(new Color(0x2a0a06));
                    g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 7, y + 7, x + 15, y + 15);
                    g2.drawLine(x + 15, y + 7, x + 7, y + 15);
                }
                default -> {
                    g2.setColor(DIM);
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(new Ellipse2D.Double(x + 1, y + 1, d - 2, d - 2));
                }
            }
        }
    }

    /** The checkbox mark: a dark rounded box with a gold edge and a gold tick. */
    static final class CheckIcon implements javax.swing.Icon {
        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            javax.swing.AbstractButton b = (javax.swing.AbstractButton) c;
            Graphics2D g2 = smooth(g);
            RoundRectangle2D r = new RoundRectangle2D.Double(x + 0.5, y + 0.5, 17, 17, 6, 6);
            g2.setColor(LOG_BG);
            g2.fill(r);
            g2.setColor(b.getModel().isRollover() ? GOLD_LIGHT : GOLD);
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(r);
            if (b.isSelected()) {
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(x + 4.5, y + 9.5);
                p.lineTo(x + 8, y + 13);
                p.lineTo(x + 14, y + 5.5);
                g2.setColor(GOLD_LIGHT);
                g2.draw(p);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 19;
        }

        @Override
        public int getIconHeight() {
            return 19;
        }
    }

    /** A slider with a gold-filled track and a round gold knob. */
    static final class SliderLook extends javax.swing.plaf.basic.BasicSliderUI {
        SliderLook(javax.swing.JSlider s) {
            super(s);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = smooth(g);
            int cy = trackRect.y + trackRect.height / 2;
            int x0 = trackRect.x;
            int x1 = trackRect.x + trackRect.width;
            RoundRectangle2D track = new RoundRectangle2D.Double(x0, cy - 3, x1 - x0, 6, 6, 6);
            g2.setColor(LOG_BG);
            g2.fill(track);
            g2.setColor(BORDER);
            g2.draw(track);
            int kx = thumbRect.x + thumbRect.width / 2;
            g2.setPaint(new GradientPaint(x0, 0, GOLD_LIGHT, x1, 0, ORANGE));
            g2.fill(new RoundRectangle2D.Double(x0 + 1, cy - 2, Math.max(0, kx - x0 - 1), 4, 4, 4));
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = smooth(g);
            int d = 16;
            int x = thumbRect.x + (thumbRect.width - d) / 2;
            int y = thumbRect.y + (thumbRect.height - d) / 2;
            g2.setColor(GOLD_LIGHT);
            g2.fill(new Ellipse2D.Double(x, y, d, d));
            g2.setColor(new Color(0x6b4a0a));
            g2.draw(new Ellipse2D.Double(x, y, d, d));
            g2.dispose();
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(18, 18);
        }

        @Override
        public void paintFocus(Graphics g) {
            // no focus ring
        }
    }

    /** A thin dark scrollbar with a brown thumb. */
    static final class ScrollLook extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            trackColor = LOG_BG;
            thumbColor = BORDER;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(isDragging ? GOLD : BORDER);
            g2.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 6, 6));
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
            g.setColor(LOG_BG);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    /** A rounded panel with the pack's brown border, used for the cards. */
    static final class Card extends javax.swing.JPanel {
        private final Color edge;

        Card(Color edge) {
            this.edge = edge;
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(PANEL_2);
            g2.fill(r);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(r);
            g2.dispose();
        }
    }
}
