// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

/**
 * Performance test of {@code WireframeMapRenderer}.
 */
public class WireframeMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {
    @Override
    protected Rendering buildRenderer() {
        return new WireframeMapRenderer(g, nc, false);
    }
}
