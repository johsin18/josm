// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Transparency;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ImageProvider.GetPaddedOptions;

import com.kitfox.svg.SVGConst;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageProvider} class.
 */
public class ImageProviderTest {
    
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().mapStyles().presets();

    private static final class LogHandler14319 extends Handler {
        boolean failed;

        @Override
        public void publish(LogRecord record) {
            if ("Could not load image: https://host-in-the-trusted-network.com/test.jpg".equals(record.getMessage())) {
                failed = true;
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9984">#9984</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket9984() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(9984, "tile.png"));
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, true, true).getTransparency());
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, false, true).getTransparency());
        long expectedTransparency = Utils.getJavaVersion() < 11 ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
        assertEquals(expectedTransparency, ImageProvider.read(file, false, false).getTransparency());
        assertEquals(expectedTransparency, ImageProvider.read(file, true, false).getTransparency());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10030">#10030</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket10030() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(10030, "tile.jpg"));
        BufferedImage img = ImageProvider.read(file, true, true);
        assertNotNull(img);
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14319">#14319</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    @SuppressFBWarnings(value = "LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE")
    public void testTicket14319() throws IOException {
        LogHandler14319 handler = new LogHandler14319();
        Logger.getLogger(SVGConst.SVG_LOGGER).addHandler(handler);
        ImageIcon img = new ImageProvider(
                new File(TestUtils.getRegressionDataDir(14319)).getAbsolutePath(), "attack.svg").get();
        assertNotNull(img);
        assertFalse(handler.failed);
    }

    /**
     * Test fetching an image using {@code wiki://} protocol.
     */
    @Test
    public void testWikiProtocol() {
        // https://commons.wikimedia.org/wiki/File:OpenJDK_logo.svg
        assertNotNull(ImageProvider.get("wiki://OpenJDK_logo.svg"));
    }

    /**
     * Test fetching an image using {@code data:} URL.
     */
    @Test
    public void testDataUrl() {
        // Red dot image, taken from https://en.wikipedia.org/wiki/Data_URI_scheme#HTML
        assertNotNull(ImageProvider.get("data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"+
                "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="));
    }

    /**
     * Unit test of {@link ImageProvider#getPadded}.
     */
    @Test
    public void testGetPadded() {
        final EnumSet<GetPaddedOptions> noDefault = EnumSet.of(GetPaddedOptions.NO_DEFAULT);
        final Dimension iconSize = new Dimension(16, 16);

        assertNull(ImageProvider.getPadded(new Node(), new Dimension(0, 0)));
        assertNotNull(ImageProvider.getPadded(new Node(), iconSize));
        assertNull(ImageProvider.getPadded(new Node(), iconSize, noDefault));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("node amenity=restaurant"), iconSize, noDefault));
        assertNull(ImageProvider.getPadded(OsmUtils.createPrimitive("node barrier=hedge"), iconSize,
                EnumSet.of(GetPaddedOptions.NO_DEFAULT, GetPaddedOptions.NO_DEPRECATED)));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("way waterway=stream"), iconSize, noDefault));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("relation type=route route=railway"), iconSize, noDefault));
    }

    /**
     * Test getting a cursor
     */
    @Ignore("manual execution only, as the look of the cursor cannot be checked automatedly")
    @Test
    public void testGetCursor() throws InterruptedException {
        JFrame frame = new JFrame();
        frame.setSize(500, 500);
        frame.setLayout(new GridLayout(2, 2));
        JPanel leftUpperPanel = new JPanel(), rightUpperPanel = new JPanel(), leftLowerPanel = new JPanel(), rightLowerPanel = new JPanel();
        leftUpperPanel.setBackground(Color.DARK_GRAY);
        rightUpperPanel.setBackground(Color.DARK_GRAY);
        leftLowerPanel.setBackground(Color.DARK_GRAY);
        rightLowerPanel.setBackground(Color.DARK_GRAY);
        frame.add(leftUpperPanel);
        frame.add(rightUpperPanel);
        frame.add(leftLowerPanel);
        frame.add(rightLowerPanel);

        leftUpperPanel.setCursor(ImageProvider.getCursor("normal", "select_add")); // contains diagonal sensitive to alpha blending
        rightUpperPanel.setCursor(ImageProvider.getCursor("crosshair", "joinway")); // combination of overlay and hotspot not top left
        leftLowerPanel.setCursor(ImageProvider.getCursor("hand", "parallel_remove")); // reasonably nice bitmap cursor
        rightLowerPanel.setCursor(ImageProvider.getCursor("rotate", null)); // ugly bitmap cursor, cannot do much here

        frame.setVisible(true);

        // hover over the four quadrant to observe different cursors

        // draw red dot at hotspot when clicking
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Graphics graphics = frame.getGraphics();
                graphics.setColor(Color.RED);
                graphics.drawRect(e.getX(), e.getY(), 1, 1);
            }

            @Override
            public void mousePressed(MouseEvent e) { }

            @Override
            public void mouseReleased(MouseEvent e) { }

            @Override
            public void mouseEntered(MouseEvent e) { }

            @Override
            public void mouseExited(MouseEvent e) { }
        });
        Thread.sleep(9000); // test would time out after 10s
    }
}
