// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.PaintingTest;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Abstract superclass of {@code StyledMapRendererPerformanceTest} and {@code WireframeMapRendererPerformanceTest}.
 */
public abstract class AbstractMapRendererPerformanceTestParent extends PaintingTest {

    private static DataSet dsRestriction;
    private static DataSet dsMultipolygon;
    private static DataSet dsOverpass;
    private static DataSet dsCity;

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    @BeforeClass
    public static void loadData() throws Exception {
        // Force reset of preferences
        StyledMapRenderer.PREFERENCE_ANTIALIASING_USE.put(true);
        StyledMapRenderer.PREFERENCE_TEXT_ANTIALIASING.put("gasp");

        try (InputStream fisR = Files.newInputStream(Paths.get("nodist/data/restriction.osm"));
             InputStream fisM = Files.newInputStream(Paths.get("nodist/data/multipolygon.osm"));
             InputStream fisC = Compression.getUncompressedFileInputStream(new File("nodist/data/neubrandenburg.osm.bz2"));
             InputStream fisO = Compression.getUncompressedFileInputStream(new File("nodist/data/overpass-download.osm.bz2"));) {
            dsRestriction = OsmReader.parseDataSet(fisR, NullProgressMonitor.INSTANCE);
            dsMultipolygon = OsmReader.parseDataSet(fisM, NullProgressMonitor.INSTANCE);
            dsCity = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
            dsOverpass = OsmReader.parseDataSet(fisO, NullProgressMonitor.INSTANCE);
        }
    }

    @AfterClass
    public static void clean() {
        dsRestriction = null;
        dsMultipolygon = null;
        dsCity = null;
        dsOverpass = null;
    }

    protected abstract Rendering buildRenderer();

    protected final void test(int iterations, DataSet ds, Bounds bounds) {
        nc.zoomTo(bounds);
        Rendering visitor = buildRenderer();
        for (int i = 0; i < iterations; i++) {
            visitor.render(ds, true, bounds);
        }
    }

    @Test
    public void testRestriction() {
        test(700, dsRestriction, new Bounds(51.12, 14.147472381591795, 51.128, 14.162492752075195));
    }

    @Test
    public void testRestrictionSmall() {
        test(1500, dsRestriction, new Bounds(51.125, 14.147, 51.128, 14.152));
    }

    @Test
    public void testMultipolygon() {
        test(400, dsMultipolygon, new Bounds(60, -180, 85, -122));
    }

    @Test
    public void testMultipolygonSmall() {
        test(850, dsMultipolygon, new Bounds(-90, -180, 90, 180));
    }

    /**
     * Complex polygon (Lake Ontario) with small download area.
     */
    @Test
    public void testOverpassDownload() {
        test(20, dsOverpass, new Bounds(43.4510496, -76.536684, 43.4643202, -76.4954853));
    }

    @Test
    public void testCity() {
        test(50, dsCity, new Bounds(53.51, 13.20, 53.59, 13.34));
    }

    @Test
    public void testCitySmall() {
        test(70, dsCity, new Bounds(52, 11, 55, 14));
    }

    @Test
    public void testCityPart1() {
        test(250, dsCity, new Bounds(53.56, 13.25, 53.57, 13.26));
    }

    @Test
    public void testCityPart2() {
        test(200, dsCity, new Bounds(53.55, 13.29, 53.57, 13.30));
    }

    @Test
    public void testCitySmallPart2() {
        test(200, dsCity, new Bounds(53.56, 13.295, 53.57, 13.30));
    }
}
