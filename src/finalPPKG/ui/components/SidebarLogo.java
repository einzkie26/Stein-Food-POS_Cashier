package finalPPKG.ui.components;

import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** Plays the five supplied wordmark images in order without moving the sidebar layout. */
public final class SidebarLogo extends JLabel {
    private static final int SIZE = 176;
    private static final int[] HOLD_MILLIS = {1400, 450, 350, 350, 1050};
    private final Icon[] frames = new Icon[HOLD_MILLIS.length];
    private final Timer playback;
    private int frame;

    public SidebarLogo() {
        for (int index = 0; index < frames.length; index++) {
            frames[index] = MenuIcons.sidebarFrame(index + 1, SIZE);
        }
        setName("sidebar-logo");
        setIcon(frames[0]);
        setHorizontalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Stein Food Hub logo");
        getAccessibleContext().setAccessibleDescription("Stein bowl and spoon wordmark.");
        playback = new Timer(HOLD_MILLIS[0], event -> {
            frame = (frame + 1) % frames.length;
            setIcon(frames[frame]);
            playback.setDelay(HOLD_MILLIS[frame]);
        });
        playback.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) { updatePlayback(); }
        });
    }

    private void updatePlayback() {
        if (isShowing() && !Boolean.getBoolean("stein.ui.reduceMotion")) {
            if (!playback.isRunning()) {
                playback.setInitialDelay(HOLD_MILLIS[frame]);
                playback.restart();
            }
        } else {
            playback.stop();
            frame = 0;
            setIcon(frames[0]);
        }
    }

    @Override public void addNotify() {
        super.addNotify();
        updatePlayback();
    }

    @Override public void removeNotify() {
        playback.stop();
        frame = 0;
        setIcon(frames[0]);
        super.removeNotify();
    }
}
