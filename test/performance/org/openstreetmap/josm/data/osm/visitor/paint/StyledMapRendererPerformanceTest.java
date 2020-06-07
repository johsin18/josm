// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.junit.BeforeClass;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

/**
 * Performance test of {@code StyledMapRenderer}.
 */
public class StyledMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {

    @BeforeClass
    public static void readMapPaintStyles() {
        // TODO Test should have it's own copy of styles because change in style can influence performance
        MapPaintStyles.readFromPreferences();
    }

    @Override
    protected Rendering buildRenderer() {
        return new StyledMapRenderer(g, nc, false);
    }
}
