package crazycraft.loader;

/**
 * The update banner's version order. An alpha install sees no banner for an older full release (v1.4.2 under
 * v2.0.0-alpha.1), and sees one for the next alpha and for the full release it leads up to.
 */
public final class UpdateBannerTest {
    public static void main(String[] args) {
        check(Gui.newer("1.4.3", "1.4.2"), "1.4.3 after 1.4.2");
        check(!Gui.newer("1.4.2", "1.4.2"), "the same version is not newer");
        check(Gui.newer("1.10.0", "1.9.9"), "parts compare by value");
        check(!Gui.newer("1.4.2", "2.0.0-alpha.1"), "an alpha install gets no banner for v1.4.2");
        check(Gui.newer("2.0.0", "2.0.0-alpha.1"), "2.0.0 after its alpha");
        check(!Gui.newer("2.0.0-alpha.1", "2.0.0"), "an alpha is not newer than its full release");
        check(Gui.newer("2.0.0-alpha.2", "2.0.0-alpha.1"), "alpha.2 after alpha.1");
        check(Gui.newer("2.0.0-beta.1", "2.0.0-alpha.3"), "beta after alpha");
        check(Gui.newer("2.0.0-alpha.1", "1.4.2"), "a v1.4.2 install would order the alpha after it");
        System.out.println("Update banner checks passed: full releases after their alphas, no banner for older ones.");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
