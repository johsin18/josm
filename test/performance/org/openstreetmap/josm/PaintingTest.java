package org.openstreetmap.josm;

import org.junit.Before;
import org.junit.BeforeClass;
import org.openstreetmap.josm.gui.NavigatableComponent;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Base class for tests painting something into a Graphics2D object.
 */
public abstract class PaintingTest {
    protected static final int IMG_WIDTH = 2048, IMG_HEIGHT = 1536;

    protected Graphics2D g;
    protected BufferedImage img;
    protected NavigatableComponent nc;

    @BeforeClass
    public static void createGui() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    @Before
    public void init() {
        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_HEIGHT);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        nc = new NavigatableComponent() {
            {
                setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);
                updateLocationState();
            }

            @Override
            protected boolean isVisibleOnScreen() {
                return true;
            }

            @Override
            public Point getLocationOnScreen() {
                return new Point(0, 0);
            }
        };
    }

    /**
     * add a call manually to verify that the rendering is set up properly
     * @throws IOException if any I/O error occurs
     */
    protected void dumpRenderedImage() throws IOException {
        ImageIO.write(img, "png", new File("testresult.png"));
    }
}
