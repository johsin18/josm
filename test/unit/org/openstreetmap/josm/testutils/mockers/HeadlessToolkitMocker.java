// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import mockit.Mock;
import mockit.MockUp;
import sun.awt.HeadlessToolkit;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;

/**
 * MockUp for a {@link Toolkit} that allows to mock getBestCursorSize
 */
public class HeadlessToolkitMocker extends MockUp<HeadlessToolkit> {
    public static Dimension bestCursorSize = new Dimension(32, 32);

    @Mock
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight) throws HeadlessException {
        return bestCursorSize;
    }
}
