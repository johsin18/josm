// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.PaintingTest;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Stopwatch;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Performance test for {@code OsmDataLayer}.
 */
public class OsmDataLayerPerformanceTest extends PaintingTest {
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules josmTestRules = new JOSMTestRules().main().projection().preferences().timeout(15 * 60 * 1000);

    @Test
    public final void testHatchFilling() throws IOException {
        DataSet dataSet = new DataSet();
        // downloaded area covers about quarter of the screen in the center
        Bounds bounds = new Bounds(-0.000_01, -0.000_01, 0.000_01, 0.000_01);
        dataSet.addDataSource(new DataSource(bounds, "test"));

        OsmDataLayer osmDataLayer = new OsmDataLayer(dataSet, "test data layer", null);
        nc.zoomTo(new Bounds(-0.000_02, -0.000_02, 0.000_02, 0.000_02));

        final int NUM_ITERATIONS = 1000;

        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < NUM_ITERATIONS; i++) {
            osmDataLayer.hatchNonDownloadedArea(g, nc);
        }

        System.out.format("Rendering took % 4.2f ms in average.\n", (float) stopwatch.elapsed() / NUM_ITERATIONS);

        dumpRenderedImage();
    }
}
