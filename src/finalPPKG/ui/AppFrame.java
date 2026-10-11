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
        setMinimumSize(new Dimension(980, 660));
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1240, Math.max(980, screen.width - 60)),
                Math.min(880, Math.max(660, screen.height - 60)));
        setLocationRelativeTo(null);
        BufferedImage icon = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = icon.createGraphics();
        new AppIcon(AppIcon.Kind.LEAF, 32, Theme.SUCCESS).paintIcon(this, graphics, 4, 4);
        graphics.dispose();
        setIconImage(icon);
    }

    public AppShell getShell() { return shell; }
}
