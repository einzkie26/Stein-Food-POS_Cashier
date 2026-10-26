package finalPPKG.ui;

import finalPPKG.app.AppConfig;
import finalPPKG.app.OrderActions;
import finalPPKG.service.MenuCatalog;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.theme.Theme;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

/** The window only hosts the workspace; no business logic lives here. */
public final class AppFrame extends JFrame {
    private final AppShell shell;

    public AppFrame(OrderActions actions, MenuCatalog menu, AppConfig config) {
        super("Stein Food Hub · Order desk");
        shell = new AppShell(actions, menu, config);
        setContentPane(shell);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(980, screen.width), Math.min(660, screen.height)));
        setSize(Math.min(1280, screen.width), Math.min(880, screen.height));
        setLocationRelativeTo(null);
        BufferedImage icon = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = icon.createGraphics();
        new AppIcon(AppIcon.Kind.BURGER, 32, Theme.PRIMARY).paintIcon(this, graphics, 4, 4);
        graphics.dispose();
        setIconImage(icon);
    }

    public AppShell getShell() { return shell; }
}
