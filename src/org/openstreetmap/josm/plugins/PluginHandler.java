// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.jar.JarFile;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * PluginHandler is basically a collection of static utility functions used to bootstrap
 * and manage the loaded plugins.
 * @since 1326
 */
public final class PluginHandler {

    /**
     * Deprecated plugins that are removed on start
     */
    static final Collection<DeprecatedPlugin> DEPRECATED_PLUGINS;
    static {
        String inCore = tr("integrated into main program");

        DEPRECATED_PLUGINS = Arrays.asList(new DeprecatedPlugin[] {
            new DeprecatedPlugin("mappaint", inCore),
            new DeprecatedPlugin("unglueplugin", inCore),
            new DeprecatedPlugin("lang-de", inCore),
            new DeprecatedPlugin("lang-en_GB", inCore),
            new DeprecatedPlugin("lang-fr", inCore),
            new DeprecatedPlugin("lang-it", inCore),
            new DeprecatedPlugin("lang-pl", inCore),
            new DeprecatedPlugin("lang-ro", inCore),
            new DeprecatedPlugin("lang-ru", inCore),
            new DeprecatedPlugin("ewmsplugin", inCore),
            new DeprecatedPlugin("ywms", inCore),
            new DeprecatedPlugin("tways-0.2", inCore),
            new DeprecatedPlugin("geotagged", inCore),
            new DeprecatedPlugin("landsat", tr("replaced by new {0} plugin", "lakewalker")),
            new DeprecatedPlugin("namefinder", inCore),
            new DeprecatedPlugin("waypoints", inCore),
            new DeprecatedPlugin("slippy_map_chooser", inCore),
            new DeprecatedPlugin("tcx-support", tr("replaced by new {0} plugin", "dataimport")),
            new DeprecatedPlugin("usertools", inCore),
            new DeprecatedPlugin("AgPifoJ", inCore),
            new DeprecatedPlugin("utilsplugin", inCore),
            new DeprecatedPlugin("ghost", inCore),
            new DeprecatedPlugin("validator", inCore),
            new DeprecatedPlugin("multipoly", inCore),
            new DeprecatedPlugin("multipoly-convert", inCore),
            new DeprecatedPlugin("remotecontrol", inCore),
            new DeprecatedPlugin("imagery", inCore),
            new DeprecatedPlugin("slippymap", inCore),
            new DeprecatedPlugin("wmsplugin", inCore),
            new DeprecatedPlugin("ParallelWay", inCore),
            new DeprecatedPlugin("dumbutils", tr("replaced by new {0} plugin", "utilsplugin2")),
            new DeprecatedPlugin("ImproveWayAccuracy", inCore),
            new DeprecatedPlugin("Curves", tr("replaced by new {0} plugin", "utilsplugin2")),
            new DeprecatedPlugin("epsg31287", tr("replaced by new {0} plugin", "proj4j")),
            new DeprecatedPlugin("licensechange", tr("no longer required")),
            new DeprecatedPlugin("restart", inCore),
            new DeprecatedPlugin("wayselector", inCore),
            new DeprecatedPlugin("openstreetbugs", tr("replaced by new {0} plugin", "notes")),
            new DeprecatedPlugin("nearclick", tr("no longer required")),
            new DeprecatedPlugin("notes", inCore),
            new DeprecatedPlugin("mirrored_download", inCore),
            new DeprecatedPlugin("ImageryCache", inCore),
            new DeprecatedPlugin("commons-imaging", tr("replaced by new {0} plugin", "apache-commons")),
            new DeprecatedPlugin("missingRoads", tr("replaced by new {0} plugin", "ImproveOsm")),
            new DeprecatedPlugin("trafficFlowDirection", tr("replaced by new {0} plugin", "ImproveOsm")),
            new DeprecatedPlugin("kendzi3d-jogl", tr("replaced by new {0} plugin", "jogl")),
            new DeprecatedPlugin("josm-geojson", tr("replaced by new {0} plugin", "geojson")),
        });
    }

    private PluginHandler() {
        // Hide default constructor for utils classes
    }

    /**
     * Description of a deprecated plugin
     */
    public static class DeprecatedPlugin implements Comparable<DeprecatedPlugin> {
        /** Plugin name */
        public final String name;
        /** Short explanation about deprecation, can be {@code null} */
        public final String reason;

        /**
         * Constructs a new {@code DeprecatedPlugin} with a given reason.
         * @param name The plugin name
         * @param reason The reason about deprecation
         */
        public DeprecatedPlugin(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime + ((name == null) ? 0 : name.hashCode());
            return prime * result + ((reason == null) ? 0 : reason.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DeprecatedPlugin other = (DeprecatedPlugin) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (reason == null) {
                if (other.reason != null)
                    return false;
            } else if (!reason.equals(other.reason))
                return false;
            return true;
        }

        @Override
        public int compareTo(DeprecatedPlugin o) {
            int d = name.compareTo(o.name);
            if (d == 0)
                d = reason.compareTo(o.reason);
            return d;
        }
    }

    /**
     * ClassLoader that makes the addURL method of URLClassLoader public.
     *
     * Like URLClassLoader, but allows to add more URLs after construction.
     */
    public static class DynamicURLClassLoader extends URLClassLoader {

        /**
         * Constructs a new {@code DynamicURLClassLoader}.
         * @param urls the URLs from which to load classes and resources
         * @param parent the parent class loader for delegation
         */
        public DynamicURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
    }

    /**
     * List of unmaintained plugins. Not really up-to-date as the vast majority of plugins are not maintained after a few months, sadly...
     */
    static final List<String> UNMAINTAINED_PLUGINS = Collections.unmodifiableList(Arrays.asList(
        "gpsbabelgui",
        "Intersect_way",
        "ContourOverlappingMerge", // See #11202, #11518, https://github.com/bularcasergiu/ContourOverlappingMerge/issues/1
        "LaneConnector",           // See #11468, #11518, https://github.com/TrifanAdrian/LanecConnectorPlugin/issues/1
        "Remove.redundant.points"  // See #11468, #11518, https://github.com/bularcasergiu/RemoveRedundantPoints (not even created an issue...)
    ));

    /**
     * Default time-based update interval, in days (pluginmanager.time-based-update.interval)
     */
    public static final int DEFAULT_TIME_BASED_UPDATE_INTERVAL = 30;

    /**
     * All installed and loaded plugins (resp. their main classes)
     */
    public static final Collection<PluginProxy> pluginList = new LinkedList<>();

    /**
     * All exceptions that occured during plugin loading
     * @since 8938
     */
    public static final Map<String, Exception> pluginLoadingExceptions = new HashMap<>();

    /**
     * Global plugin ClassLoader.
     */
    private static DynamicURLClassLoader pluginClassLoader;

    /**
     * Add here all ClassLoader whose resource should be searched.
     */
    private static final List<ClassLoader> sources = new LinkedList<>();
    static {
        try {
            sources.add(ClassLoader.getSystemClassLoader());
            sources.add(org.openstreetmap.josm.gui.MainApplication.class.getClassLoader());
        } catch (SecurityException ex) {
            Main.debug(ex);
            sources.add(ImageProvider.class.getClassLoader());
        }
    }

    private static PluginDownloadTask pluginDownloadTask;

    public static Collection<ClassLoader> getResourceClassLoaders() {
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * Removes deprecated plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>.
     *
     * Also notifies the user about removed deprecated plugins
     *
     * @param parent The parent Component used to display warning popup
     * @param plugins the collection of plugins
     */
    static void filterDeprecatedPlugins(Component parent, Collection<String> plugins) {
        Set<DeprecatedPlugin> removedPlugins = new TreeSet<>();
        for (DeprecatedPlugin depr : DEPRECATED_PLUGINS) {
            if (plugins.contains(depr.name)) {
                plugins.remove(depr.name);
                Main.pref.removeFromCollection("plugins", depr.name);
                removedPlugins.add(depr);
            }
        }
        if (removedPlugins.isEmpty())
            return;

        // notify user about removed deprecated plugins
        //
        StringBuilder sb = new StringBuilder(32);
        sb.append("<html>")
          .append(trn(
                "The following plugin is no longer necessary and has been deactivated:",
                "The following plugins are no longer necessary and have been deactivated:",
                removedPlugins.size()))
          .append("<ul>");
        for (DeprecatedPlugin depr: removedPlugins) {
            sb.append("<li>").append(depr.name);
            if (depr.reason != null) {
                sb.append(" (").append(depr.reason).append(')');
            }
            sb.append("</li>");
        }
        sb.append("</ul></html>");
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(
                    parent,
                    sb.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    /**
     * Removes unmaintained plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>. Also removes the plugin from the list
     * of plugins in the preferences, if necessary.
     *
     * Asks the user for every unmaintained plugin whether it should be removed.
     * @param parent The parent Component used to display warning popup
     *
     * @param plugins the collection of plugins
     */
    static void filterUnmaintainedPlugins(Component parent, Collection<String> plugins) {
        for (String unmaintained : UNMAINTAINED_PLUGINS) {
            if (!plugins.contains(unmaintained)) {
                continue;
            }
            String msg = tr("<html>Loading of the plugin \"{0}\" was requested."
                    + "<br>This plugin is no longer developed and very likely will produce errors."
                    +"<br>It should be disabled.<br>Delete from preferences?</html>", unmaintained);
            if (confirmDisablePlugin(parent, msg, unmaintained)) {
                Main.pref.removeFromCollection("plugins", unmaintained);
                plugins.remove(unmaintained);
            }
        }
    }

    /**
     * Checks whether the locally available plugins should be updated and
     * asks the user if running an update is OK. An update is advised if
     * JOSM was updated to a new version since the last plugin updates or
     * if the plugins were last updated a long time ago.
     *
     * @param parent the parent component relative to which the confirmation dialog
     * is to be displayed
     * @return true if a plugin update should be run; false, otherwise
     */
    public static boolean checkAndConfirmPluginUpdate(Component parent) {
        if (!checkOfflineAccess()) {
            Main.info(tr("{0} not available (offline mode)", tr("Plugin update")));
            return false;
        }
        String message = null;
        String togglePreferenceKey = null;
        int v = Version.getInstance().getVersion();
        if (Main.pref.getInteger("pluginmanager.version", 0) < v) {
            message =
                "<html>"
                + tr("You updated your JOSM software.<br>"
                        + "To prevent problems the plugins should be updated as well.<br><br>"
                        + "Update plugins now?"
                )
                + "</html>";
            togglePreferenceKey = "pluginmanager.version-based-update.policy";
        } else {
            long tim = System.currentTimeMillis();
            long last = Main.pref.getLong("pluginmanager.lastupdate", 0);
            Integer maxTime = Main.pref.getInteger("pluginmanager.time-based-update.interval", DEFAULT_TIME_BASED_UPDATE_INTERVAL);
            long d = (tim - last) / (24 * 60 * 60 * 1000L);
            if ((last <= 0) || (maxTime <= 0)) {
                Main.pref.put("pluginmanager.lastupdate", Long.toString(tim));
            } else if (d > maxTime) {
                message =
                    "<html>"
                    + tr("Last plugin update more than {0} days ago.", d)
                    + "</html>";
                togglePreferenceKey = "pluginmanager.time-based-update.policy";
            }
        }
        if (message == null) return false;

        UpdatePluginsMessagePanel pnlMessage = new UpdatePluginsMessagePanel();
        pnlMessage.setMessage(message);
        pnlMessage.initDontShowAgain(togglePreferenceKey);

        // check whether automatic update at startup was disabled
        //
        String policy = Main.pref.get(togglePreferenceKey, "ask").trim().toLowerCase(Locale.ENGLISH);
        switch(policy) {
        case "never":
            if ("pluginmanager.version-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Skipping plugin update after JOSM upgrade. Automatic update at startup is disabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Skipping plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return false;

        case "always":
            if ("pluginmanager.version-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Running plugin update after JOSM upgrade. Automatic update at startup is enabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Running plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return true;

        case "ask":
            break;

        default:
            Main.warn(tr("Unexpected value ''{0}'' for preference ''{1}''. Assuming value ''ask''.", policy, togglePreferenceKey));
        }

        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Update plugins"),
                        ImageProvider.get("dialogs", "refresh"),
                        tr("Click to update the activated plugins"),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Skip update"),
                        ImageProvider.get("cancel"),
                        tr("Click to skip updating the activated plugins"),
                        null /* no specific help context */
                )
        };

        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                pnlMessage,
                tr("Update plugins"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                ht("/Preferences/Plugins#AutomaticUpdate")
        );

        if (pnlMessage.isRememberDecision()) {
            switch(ret) {
            case 0:
                Main.pref.put(togglePreferenceKey, "always");
                break;
            case JOptionPane.CLOSED_OPTION:
            case 1:
                Main.pref.put(togglePreferenceKey, "never");
                break;
            default: // Do nothing
            }
        } else {
            Main.pref.put(togglePreferenceKey, "ask");
        }
        return ret == 0;
    }

    private static boolean checkOfflineAccess() {
        if (Main.isOffline(OnlineResource.ALL)) {
            return false;
        }
        if (Main.isOffline(OnlineResource.JOSM_WEBSITE)) {
            for (String updateSite : Main.pref.getPluginSites()) {
                try {
                    OnlineResource.JOSM_WEBSITE.checkOfflineAccess(updateSite, Main.getJOSMWebsite());
                } catch (OfflineAccessException e) {
                    Main.trace(e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Alerts the user if a plugin required by another plugin is missing, and offer to download them &amp; restart JOSM
     *
     * @param parent The parent Component used to display error popup
     * @param plugin the plugin
     * @param missingRequiredPlugin the missing required plugin
     */
    private static void alertMissingRequiredPlugin(Component parent, String plugin, Set<String> missingRequiredPlugin) {
        StringBuilder sb = new StringBuilder(48);
        sb.append("<html>")
          .append(trn("Plugin {0} requires a plugin which was not found. The missing plugin is:",
                "Plugin {0} requires {1} plugins which were not found. The missing plugins are:",
                missingRequiredPlugin.size(),
                plugin,
                missingRequiredPlugin.size()))
          .append(Utils.joinAsHtmlUnorderedList(missingRequiredPlugin))
          .append("</html>");
        ButtonSpec[] specs = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Download and restart"),
                        ImageProvider.get("restart"),
                        trn("Click to download missing plugin and restart JOSM",
                            "Click to download missing plugins and restart JOSM",
                            missingRequiredPlugin.size()),
                        null /* no specific help text */
                ),
                new ButtonSpec(
                        tr("Continue"),
                        ImageProvider.get("ok"),
                        trn("Click to continue without this plugin",
                            "Click to continue without these plugins",
                            missingRequiredPlugin.size()),
                        null /* no specific help text */
                )
        };
        if (0 == HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE,
                null, /* no special icon */
                specs,
                specs[0],
                ht("/Plugin/Loading#MissingRequiredPlugin"))) {
            downloadRequiredPluginsAndRestart(parent, missingRequiredPlugin);
        }
    }

    private static void downloadRequiredPluginsAndRestart(final Component parent, final Set<String> missingRequiredPlugin) {
        // Update plugin list
        final ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                Main.pref.getOnlinePluginSites());
        Main.worker.submit(pluginInfoDownloadTask);

        // Continuation
        Main.worker.submit(() -> {
            // Build list of plugins to download
            Set<PluginInformation> toDownload = new HashSet<>(pluginInfoDownloadTask.getAvailablePlugins());
            for (Iterator<PluginInformation> it = toDownload.iterator(); it.hasNext();) {
                PluginInformation info = it.next();
                if (!missingRequiredPlugin.contains(info.getName())) {
                    it.remove();
                }
            }
            // Check if something has still to be downloaded
            if (!toDownload.isEmpty()) {
                // download plugins
                final PluginDownloadTask task = new PluginDownloadTask(parent, toDownload, tr("Download plugins"));
                Main.worker.submit(task);
                Main.worker.submit(() -> {
                    // restart if some plugins have been downloaded
                    if (!task.getDownloadedPlugins().isEmpty()) {
                        // update plugin list in preferences
                        Set<String> plugins = new HashSet<>(Main.pref.getCollection("plugins"));
                        for (PluginInformation plugin : task.getDownloadedPlugins()) {
                            plugins.add(plugin.name);
                        }
                        Main.pref.putCollection("plugins", plugins);
                        // restart
                        new RestartAction().actionPerformed(null);
                    } else {
                        Main.warn("No plugin downloaded, restart canceled");
                    }
                });
            } else {
                Main.warn("No plugin to download, operation canceled");
            }
        });
    }

    private static void alertJOSMUpdateRequired(Component parent, String plugin, int requiredVersion) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>Plugin {0} requires JOSM version {1}. The current JOSM version is {2}.<br>"
                        +"You have to update JOSM in order to use this plugin.</html>",
                        plugin, Integer.toString(requiredVersion), Version.getInstance().getVersionString()
                ),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                ht("/Plugin/Loading#JOSMUpdateRequired")
        );
    }

    /**
     * Checks whether all preconditions for loading the plugin <code>plugin</code> are met. The
     * current JOSM version must be compatible with the plugin and no other plugins this plugin
     * depends on should be missing.
     *
     * @param parent The parent Component used to display error popup
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @return true, if the preconditions are met; false otherwise
     */
    public static boolean checkLoadPreconditions(Component parent, Collection<PluginInformation> plugins, PluginInformation plugin) {

        // make sure the plugin is compatible with the current JOSM version
        //
        int josmVersion = Version.getInstance().getVersion();
        if (plugin.localmainversion > josmVersion && josmVersion != Version.JOSM_UNKNOWN_VERSION) {
            alertJOSMUpdateRequired(parent, plugin.name, plugin.localmainversion);
            return false;
        }

        // Add all plugins already loaded (to include early plugins when checking late ones)
        Collection<PluginInformation> allPlugins = new HashSet<>(plugins);
        for (PluginProxy proxy : pluginList) {
            allPlugins.add(proxy.getPluginInformation());
        }

        return checkRequiredPluginsPreconditions(parent, allPlugins, plugin, true);
    }

    /**
     * Checks if required plugins preconditions for loading the plugin <code>plugin</code> are met.
     * No other plugins this plugin depends on should be missing.
     *
     * @param parent The parent Component used to display error popup. If parent is
     * null, the error popup is suppressed
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @param local Determines if the local or up-to-date plugin dependencies are to be checked.
     * @return true, if the preconditions are met; false otherwise
     * @since 5601
     */
    public static boolean checkRequiredPluginsPreconditions(Component parent, Collection<PluginInformation> plugins,
            PluginInformation plugin, boolean local) {

        String requires = local ? plugin.localrequires : plugin.requires;

        // make sure the dependencies to other plugins are not broken
        //
        if (requires != null) {
            Set<String> pluginNames = new HashSet<>();
            for (PluginInformation pi: plugins) {
                pluginNames.add(pi.name);
            }
            Set<String> missingPlugins = new HashSet<>();
            List<String> requiredPlugins = local ? plugin.getLocalRequiredPlugins() : plugin.getRequiredPlugins();
            for (String requiredPlugin : requiredPlugins) {
                if (!pluginNames.contains(requiredPlugin)) {
                    missingPlugins.add(requiredPlugin);
                }
            }
            if (!missingPlugins.isEmpty()) {
                if (parent != null) {
                    alertMissingRequiredPlugin(parent, plugin.name, missingPlugins);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Get the class loader for loading plugin code.
     *
     * @return the class loader
     */
    public static synchronized DynamicURLClassLoader getPluginClassLoader() {
        if (pluginClassLoader == null) {
            pluginClassLoader = AccessController.doPrivileged((PrivilegedAction<DynamicURLClassLoader>)
                    () -> new DynamicURLClassLoader(new URL[0], Main.class.getClassLoader()));
            sources.add(0, pluginClassLoader);
        }
        return pluginClassLoader;
    }

    /**
     * Add more plugins to the plugin class loader.
     *
     * @param plugins the plugins that should be handled by the plugin class loader
     */
    public static void extendPluginClassLoader(Collection<PluginInformation> plugins) {
        // iterate all plugins and collect all libraries of all plugins:
        File pluginDir = Main.pref.getPluginsDirectory();
        DynamicURLClassLoader cl = getPluginClassLoader();

        for (PluginInformation info : plugins) {
            if (info.libraries == null) {
                continue;
            }
            for (URL libUrl : info.libraries) {
                cl.addURL(libUrl);
            }
            File pluginJar = new File(pluginDir, info.name + ".jar");
            I18n.addTexts(pluginJar);
            URL pluginJarUrl = Utils.fileToURL(pluginJar);
            cl.addURL(pluginJarUrl);
        }
    }

    /**
     * Loads and instantiates the plugin described by <code>plugin</code> using
     * the class loader <code>pluginClassLoader</code>.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugin the plugin
     * @param pluginClassLoader the plugin class loader
     */
    public static void loadPlugin(Component parent, PluginInformation plugin, ClassLoader pluginClassLoader) {
        String msg = tr("Could not load plugin {0}. Delete from preferences?", plugin.name);
        try {
            Class<?> klass = plugin.loadClass(pluginClassLoader);
            if (klass != null) {
                Main.info(tr("loading plugin ''{0}'' (version {1})", plugin.name, plugin.localversion));
                PluginProxy pluginProxy = plugin.load(klass);
                pluginList.add(pluginProxy);
                Main.addMapFrameListener(pluginProxy, true);
            }
            msg = null;
        } catch (PluginException e) {
            pluginLoadingExceptions.put(plugin.name, e);
            Main.error(e);
            if (e.getCause() instanceof ClassNotFoundException) {
                msg = tr("<html>Could not load plugin {0} because the plugin<br>main class ''{1}'' was not found.<br>"
                        + "Delete from preferences?</html>", plugin.name, plugin.className);
            }
        } catch (RuntimeException e) {
            pluginLoadingExceptions.put(plugin.name, e);
            Main.error(e);
        }
        if (msg != null && confirmDisablePlugin(parent, msg, plugin.name)) {
            Main.pref.removeFromCollection("plugins", plugin.name);
        }
    }

    /**
     * Loads the plugin in <code>plugins</code> from locally available jar files into memory.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the list of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadPlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Loading plugins ..."));
            monitor.subTask(tr("Checking plugin preconditions..."));
            List<PluginInformation> toLoad = new LinkedList<>();
            for (PluginInformation pi: plugins) {
                if (checkLoadPreconditions(parent, plugins, pi)) {
                    toLoad.add(pi);
                }
            }
            // sort the plugins according to their "staging" equivalence class. The
            // lower the value of "stage" the earlier the plugin should be loaded.
            //
            toLoad.sort(Comparator.comparingInt(o -> o.stage));
            if (toLoad.isEmpty())
                return;

            extendPluginClassLoader(toLoad);
            monitor.setTicksCount(toLoad.size());
            for (PluginInformation info : toLoad) {
                monitor.setExtraText(tr("Loading plugin ''{0}''...", info.name));
                loadPlugin(parent, info, getPluginClassLoader());
                monitor.worked(1);
            }
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early} set to true.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadEarlyPlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> earlyPlugins = new ArrayList<>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (pi.early) {
                earlyPlugins.add(pi);
            }
        }
        loadPlugins(parent, earlyPlugins, monitor);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early} set to false.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadLatePlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> latePlugins = new ArrayList<>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (!pi.early) {
                latePlugins.add(pi);
            }
        }
        loadPlugins(parent, latePlugins, monitor);
    }

    /**
     * Loads locally available plugin information from local plugin jars and from cached
     * plugin lists.
     *
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the list of locally available plugin information
     *
     */
    private static Map<String, PluginInformation> loadLocallyAvailablePluginInformation(ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            ReadLocalPluginInformationTask task = new ReadLocalPluginInformationTask(monitor);
            try {
                task.run();
            } catch (RuntimeException e) {
                Main.error(e);
                return null;
            }
            Map<String, PluginInformation> ret = new HashMap<>();
            for (PluginInformation pi: task.getAvailablePlugins()) {
                ret.put(pi.name, pi);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertMissingPluginInformation(Component parent, Collection<String> plugins) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
          .append(trn("JOSM could not find information about the following plugin:",
                "JOSM could not find information about the following plugins:",
                plugins.size()))
          .append(Utils.joinAsHtmlUnorderedList(plugins))
          .append(trn("The plugin is not going to be loaded.",
                "The plugins are not going to be loaded.",
                plugins.size()))
          .append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                ht("/Plugin/Loading#MissingPluginInfos")
        );
    }

    /**
     * Builds the set of plugins to load. Deprecated and unmaintained plugins are filtered
     * out. This involves user interaction. This method displays alert and confirmation
     * messages.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the set of plugins to load (as set of plugin names)
     */
    public static List<PluginInformation> buildListOfPluginsToLoad(Component parent, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Determine plugins to load..."));
            Set<String> plugins = new HashSet<>(Main.pref.getCollection("plugins", new LinkedList<String>()));
            if (Main.isDebugEnabled()) {
                Main.debug("Plugins list initialized to " + plugins);
            }
            String systemProp = System.getProperty("josm.plugins");
            if (systemProp != null) {
                plugins.addAll(Arrays.asList(systemProp.split(",")));
                if (Main.isDebugEnabled()) {
                    Main.debug("josm.plugins system property set to '" + systemProp+"'. Plugins list is now " + plugins);
                }
            }
            monitor.subTask(tr("Removing deprecated plugins..."));
            filterDeprecatedPlugins(parent, plugins);
            monitor.subTask(tr("Removing unmaintained plugins..."));
            filterUnmaintainedPlugins(parent, plugins);
            if (Main.isDebugEnabled()) {
                Main.debug("Plugins list is finally set to " + plugins);
            }
            Map<String, PluginInformation> infos = loadLocallyAvailablePluginInformation(monitor.createSubTaskMonitor(1, false));
            List<PluginInformation> ret = new LinkedList<>();
            for (Iterator<String> it = plugins.iterator(); it.hasNext();) {
                String plugin = it.next();
                if (infos.containsKey(plugin)) {
                    ret.add(infos.get(plugin));
                    it.remove();
                }
            }
            if (!plugins.isEmpty()) {
                alertMissingPluginInformation(parent, plugins);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertFailedPluginUpdate(Component parent, Collection<PluginInformation> plugins) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("<html>")
          .append(trn(
                "Updating the following plugin has failed:",
                "Updating the following plugins has failed:",
                plugins.size()))
          .append("<ul>");
        for (PluginInformation pi: plugins) {
            sb.append("<li>").append(pi.name).append("</li>");
        }
        sb.append("</ul>")
          .append(trn(
                "Please open the Preference Dialog after JOSM has started and try to update it manually.",
                "Please open the Preference Dialog after JOSM has started and try to update them manually.",
                plugins.size()))
          .append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Plugin update failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/Plugin/Loading#FailedPluginUpdated")
        );
    }

    private static Set<PluginInformation> findRequiredPluginsToDownload(
            Collection<PluginInformation> pluginsToUpdate, List<PluginInformation> allPlugins, Set<PluginInformation> pluginsToDownload) {
        Set<PluginInformation> result = new HashSet<>();
        for (PluginInformation pi : pluginsToUpdate) {
            for (String name : pi.getRequiredPlugins()) {
                try {
                    PluginInformation installedPlugin = PluginInformation.findPlugin(name);
                    if (installedPlugin == null) {
                        // New required plugin is not installed, find its PluginInformation
                        PluginInformation reqPlugin = null;
                        for (PluginInformation pi2 : allPlugins) {
                            if (pi2.getName().equals(name)) {
                                reqPlugin = pi2;
                                break;
                            }
                        }
                        // Required plugin is known but not already on download list
                        if (reqPlugin != null && !pluginsToDownload.contains(reqPlugin)) {
                            result.add(reqPlugin);
                        }
                    }
                } catch (PluginException e) {
                    Main.warn(tr("Failed to find plugin {0}", name));
                    Main.error(e);
                }
            }
        }
        return result;
    }

    /**
     * Updates the plugins in <code>plugins</code>.
     *
     * @param parent the parent component for message boxes
     * @param pluginsWanted the collection of plugins to update. Updates all plugins if {@code null}
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     * @return the list of plugins to load
     * @throws IllegalArgumentException if plugins is null
     */
    public static Collection<PluginInformation> updatePlugins(Component parent,
            Collection<PluginInformation> pluginsWanted, ProgressMonitor monitor, boolean displayErrMsg) {
        Collection<PluginInformation> plugins = null;
        pluginDownloadTask = null;
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");

            // try to download the plugin lists
            //
            ReadRemotePluginInformationTask task1 = new ReadRemotePluginInformationTask(
                    monitor.createSubTaskMonitor(1, false),
                    Main.pref.getOnlinePluginSites(), displayErrMsg
            );
            task1.run();
            List<PluginInformation> allPlugins = null;

            try {
                allPlugins = task1.getAvailablePlugins();
                plugins = buildListOfPluginsToLoad(parent, monitor.createSubTaskMonitor(1, false));
                // If only some plugins have to be updated, filter the list
                if (pluginsWanted != null && !pluginsWanted.isEmpty()) {
                    for (Iterator<PluginInformation> it = plugins.iterator(); it.hasNext();) {
                        PluginInformation pi = it.next();
                        boolean found = false;
                        for (PluginInformation piw : pluginsWanted) {
                            if (pi.name.equals(piw.name)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            it.remove();
                        }
                    }
                }
            } catch (RuntimeException e) {
                Main.warn(tr("Failed to download plugin information list"));
                Main.error(e);
                // don't abort in case of error, continue with downloading plugins below
            }

            // filter plugins which actually have to be updated
            //
            Collection<PluginInformation> pluginsToUpdate = new ArrayList<>();
            for (PluginInformation pi: plugins) {
                if (pi.isUpdateRequired()) {
                    pluginsToUpdate.add(pi);
                }
            }

            if (!pluginsToUpdate.isEmpty()) {

                Set<PluginInformation> pluginsToDownload = new HashSet<>(pluginsToUpdate);

                if (allPlugins != null) {
                    // Updated plugins may need additional plugin dependencies currently not installed
                    //
                    Set<PluginInformation> additionalPlugins = findRequiredPluginsToDownload(pluginsToUpdate, allPlugins, pluginsToDownload);
                    pluginsToDownload.addAll(additionalPlugins);

                    // Iterate on required plugins, if they need themselves another plugins (i.e A needs B, but B needs C)
                    while (!additionalPlugins.isEmpty()) {
                        // Install the additional plugins to load them later
                        plugins.addAll(additionalPlugins);
                        additionalPlugins = findRequiredPluginsToDownload(additionalPlugins, allPlugins, pluginsToDownload);
                        pluginsToDownload.addAll(additionalPlugins);
                    }
                }

                // try to update the locally installed plugins
                //
                pluginDownloadTask = new PluginDownloadTask(
                        monitor.createSubTaskMonitor(1, false),
                        pluginsToDownload,
                        tr("Update plugins")
                );

                try {
                    pluginDownloadTask.run();
                } catch (RuntimeException e) {
                    Main.error(e);
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return plugins;
                }

                // Update Plugin info for downloaded plugins
                //
                refreshLocalUpdatedPluginInfo(pluginDownloadTask.getDownloadedPlugins());

                // notify user if downloading a locally installed plugin failed
                //
                if (!pluginDownloadTask.getFailedPlugins().isEmpty()) {
                    alertFailedPluginUpdate(parent, pluginDownloadTask.getFailedPlugins());
                    return plugins;
                }
            }
        } finally {
            monitor.finishTask();
        }
        if (pluginsWanted == null) {
            // if all plugins updated, remember the update because it was successful
            //
            Main.pref.putInteger("pluginmanager.version", Version.getInstance().getVersion());
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
        }
        return plugins;
    }

    /**
     * Ask the user for confirmation that a plugin shall be disabled.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param reason the reason for disabling the plugin
     * @param name the plugin name
     * @return true, if the plugin shall be disabled; false, otherwise
     */
    public static boolean confirmDisablePlugin(Component parent, String reason, String name) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Disable plugin"),
                        ImageProvider.get("dialogs", "delete"),
                        tr("Click to delete the plugin ''{0}''", name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        ImageProvider.get("cancel"),
                        tr("Click to keep the plugin ''{0}''", name),
                        null /* no specific help context */
                )
        };
        return 0 == HelpAwareOptionPane.showOptionDialog(
                    parent,
                    reason,
                    tr("Disable plugin"),
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0],
                    null // FIXME: add help topic
            );
    }

    /**
     * Returns the plugin of the specified name.
     * @param name The plugin name
     * @return The plugin of the specified name, if installed and loaded, or {@code null} otherwise.
     */
    public static Object getPlugin(String name) {
        for (PluginProxy plugin : pluginList) {
            if (plugin.getPluginInformation().name.equals(name))
                return plugin.plugin;
        }
        return null;
    }

    public static void addDownloadSelection(List<DownloadSelection> downloadSelections) {
        for (PluginProxy p : pluginList) {
            p.addDownloadSelection(downloadSelections);
        }
    }

    public static Collection<PreferenceSettingFactory> getPreferenceSetting() {
        Collection<PreferenceSettingFactory> settings = new ArrayList<>();
        for (PluginProxy plugin : pluginList) {
            settings.add(new PluginPreferenceFactory(plugin));
        }
        return settings;
    }

    /**
     * Installs downloaded plugins. Moves files with the suffix ".jar.new" to the corresponding
     * ".jar" files.
     *
     * If {@code dowarn} is true, this methods emits warning messages on the console if a downloaded
     * but not yet installed plugin .jar can't be be installed. If {@code dowarn} is false, the
     * installation of the respective plugin is silently skipped.
     *
     * @param dowarn if true, warning messages are displayed; false otherwise
     */
    public static void installDownloadedPlugins(boolean dowarn) {
        File pluginDir = Main.pref.getPluginsDirectory();
        if (!pluginDir.exists() || !pluginDir.isDirectory() || !pluginDir.canWrite())
            return;

        final File[] files = pluginDir.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".jar.new"));
        if (files == null)
            return;

        for (File updatedPlugin : files) {
            final String filePath = updatedPlugin.getPath();
            File plugin = new File(filePath.substring(0, filePath.length() - 4));
            String pluginName = updatedPlugin.getName().substring(0, updatedPlugin.getName().length() - 8);
            if (plugin.exists() && !plugin.delete() && dowarn) {
                Main.warn(tr("Failed to delete outdated plugin ''{0}''.", plugin.toString()));
                Main.warn(tr("Failed to install already downloaded plugin ''{0}''. " +
                        "Skipping installation. JOSM is still going to load the old plugin version.",
                        pluginName));
                continue;
            }
            try {
                // Check the plugin is a valid and accessible JAR file before installing it (fix #7754)
                new JarFile(updatedPlugin).close();
            } catch (IOException e) {
                if (dowarn) {
                    Main.warn(e, tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. {2}",
                            plugin.toString(), updatedPlugin.toString(), e.getLocalizedMessage()));
                }
                continue;
            }
            // Install plugin
            if (!updatedPlugin.renameTo(plugin) && dowarn) {
                Main.warn(tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. Renaming failed.",
                        plugin.toString(), updatedPlugin.toString()));
                Main.warn(tr("Failed to install already downloaded plugin ''{0}''. " +
                        "Skipping installation. JOSM is still going to load the old plugin version.",
                        pluginName));
            }
        }
    }

    /**
     * Determines if the specified file is a valid and accessible JAR file.
     * @param jar The file to check
     * @return true if file can be opened as a JAR file.
     * @since 5723
     */
    public static boolean isValidJar(File jar) {
        if (jar != null && jar.exists() && jar.canRead()) {
            try {
                new JarFile(jar).close();
            } catch (IOException e) {
                Main.warn(e);
                return false;
            }
            return true;
        } else if (jar != null) {
            Main.warn("Invalid jar file ''"+jar+"'' (exists: "+jar.exists()+", canRead: "+jar.canRead()+')');
        }
        return false;
    }

    /**
     * Replies the updated jar file for the given plugin name.
     * @param name The plugin name to find.
     * @return the updated jar file for the given plugin name. null if not found or not readable.
     * @since 5601
     */
    public static File findUpdatedJar(String name) {
        File pluginDir = Main.pref.getPluginsDirectory();
        // Find the downloaded file. We have tried to install the downloaded plugins
        // (PluginHandler.installDownloadedPlugins). This succeeds depending on the platform.
        File downloadedPluginFile = new File(pluginDir, name + ".jar.new");
        if (!isValidJar(downloadedPluginFile)) {
            downloadedPluginFile = new File(pluginDir, name + ".jar");
            if (!isValidJar(downloadedPluginFile)) {
                return null;
            }
        }
        return downloadedPluginFile;
    }

    /**
     * Refreshes the given PluginInformation objects with new contents read from their corresponding jar file.
     * @param updatedPlugins The PluginInformation objects to update.
     * @since 5601
     */
    public static void refreshLocalUpdatedPluginInfo(Collection<PluginInformation> updatedPlugins) {
        if (updatedPlugins == null) return;
        for (PluginInformation pi : updatedPlugins) {
            File downloadedPluginFile = findUpdatedJar(pi.name);
            if (downloadedPluginFile == null) {
                continue;
            }
            try {
                pi.updateFromJar(new PluginInformation(downloadedPluginFile, pi.name));
            } catch (PluginException e) {
                Main.error(e);
            }
        }
    }

    private static int askUpdateDisableKeepPluginAfterException(PluginProxy plugin) {
        final ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Update plugin"),
                        ImageProvider.get("dialogs", "refresh"),
                        tr("Click to update the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Disable plugin"),
                        ImageProvider.get("dialogs", "delete"),
                        tr("Click to disable the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        ImageProvider.get("cancel"),
                        tr("Click to keep the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                )
        };

        final StringBuilder msg = new StringBuilder(256);
        msg.append("<html>")
           .append(tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.", plugin.getPluginInformation().name))
           .append("<br>");
        if (plugin.getPluginInformation().author != null) {
            msg.append(tr("According to the information within the plugin, the author is {0}.", plugin.getPluginInformation().author))
               .append("<br>");
        }
        msg.append(tr("Try updating to the newest version of this plugin before reporting a bug."))
           .append("</html>");

        try {
            FutureTask<Integer> task = new FutureTask<>(() -> HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    msg.toString(),
                    tr("Update plugins"),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0],
                    ht("/ErrorMessages#ErrorInPlugin")
            ));
            GuiHelper.runInEDT(task);
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            Main.warn(e);
        }
        return -1;
    }

    /**
     * Replies the plugin which most likely threw the exception <code>ex</code>.
     *
     * @param ex the exception
     * @return the plugin; null, if the exception probably wasn't thrown from a plugin
     */
    private static PluginProxy getPluginCausingException(Throwable ex) {
        PluginProxy err = null;
        StackTraceElement[] stack = ex.getStackTrace();
        // remember the error position, as multiple plugins may be involved, we search the topmost one
        int pos = stack.length;
        for (PluginProxy p : pluginList) {
            String baseClass = p.getPluginInformation().className;
            baseClass = baseClass.substring(0, baseClass.lastIndexOf('.'));
            for (int elpos = 0; elpos < pos; ++elpos) {
                if (stack[elpos].getClassName().startsWith(baseClass)) {
                    pos = elpos;
                    err = p;
                }
            }
        }
        return err;
    }

    /**
     * Checks whether the exception <code>e</code> was thrown by a plugin. If so,
     * conditionally updates or deactivates the plugin, but asks the user first.
     *
     * @param e the exception
     * @return plugin download task if the plugin has been updated to a newer version, {@code null} if it has been disabled or kept as it
     */
    public static PluginDownloadTask updateOrdisablePluginAfterException(Throwable e) {
        PluginProxy plugin = null;
        // Check for an explicit problem when calling a plugin function
        if (e instanceof PluginException) {
            plugin = ((PluginException) e).plugin;
        }
        if (plugin == null) {
            plugin = getPluginCausingException(e);
        }
        if (plugin == null)
            // don't know what plugin threw the exception
            return null;

        Set<String> plugins = new HashSet<>(
                Main.pref.getCollection("plugins", Collections.<String>emptySet())
        );
        final PluginInformation pluginInfo = plugin.getPluginInformation();
        if (!plugins.contains(pluginInfo.name))
            // plugin not activated ? strange in this context but anyway, don't bother
            // the user with dialogs, skip conditional deactivation
            return null;

        switch (askUpdateDisableKeepPluginAfterException(plugin)) {
        case 0:
            // update the plugin
            updatePlugins(Main.parent, Collections.singleton(pluginInfo), null, true);
            return pluginDownloadTask;
        case 1:
            // deactivate the plugin
            plugins.remove(plugin.getPluginInformation().name);
            Main.pref.putCollection("plugins", plugins);
            GuiHelper.runInEDTAndWait(() -> JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            ));
            return null;
        default:
            // user doesn't want to deactivate the plugin
            return null;
        }
    }

    /**
     * Returns the list of loaded plugins as a {@code String} to be displayed in status report. Useful for bug reports.
     * @return The list of loaded plugins
     */
    public static Collection<String> getBugReportInformation() {
        final Collection<String> pl = new TreeSet<>(Main.pref.getCollection("plugins", new LinkedList<>()));
        for (final PluginProxy pp : pluginList) {
            PluginInformation pi = pp.getPluginInformation();
            pl.remove(pi.name);
            pl.add(pi.name + " (" + (pi.localversion != null && !pi.localversion.isEmpty()
                    ? pi.localversion : "unknown") + ')');
        }
        return pl;
    }

    /**
     * Returns the list of loaded plugins as a {@code JPanel} to be displayed in About dialog.
     * @return The list of loaded plugins (one "line" of Swing components per plugin)
     */
    public static JPanel getInfoPanel() {
        JPanel pluginTab = new JPanel(new GridBagLayout());
        for (final PluginProxy p : pluginList) {
            final PluginInformation info = p.getPluginInformation();
            String name = info.name
            + (info.version != null && !info.version.isEmpty() ? " Version: " + info.version : "");
            pluginTab.add(new JLabel(name), GBC.std());
            pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
            pluginTab.add(new JButton(new AbstractAction(tr("Information")) {
                @Override
                public void actionPerformed(ActionEvent event) {
                    StringBuilder b = new StringBuilder();
                    for (Entry<String, String> e : info.attr.entrySet()) {
                        b.append(e.getKey());
                        b.append(": ");
                        b.append(e.getValue());
                        b.append('\n');
                    }
                    JosmTextArea a = new JosmTextArea(10, 40);
                    a.setEditable(false);
                    a.setText(b.toString());
                    a.setCaretPosition(0);
                    JOptionPane.showMessageDialog(Main.parent, new JScrollPane(a), tr("Plugin information"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }), GBC.eol());

            JosmTextArea description = new JosmTextArea(info.description == null ? tr("no description available")
                    : info.description);
            description.setEditable(false);
            description.setFont(new JLabel().getFont().deriveFont(Font.ITALIC));
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            description.setBackground(UIManager.getColor("Panel.background"));
            description.setCaretPosition(0);

            pluginTab.add(description, GBC.eop().fill(GBC.HORIZONTAL));
        }
        return pluginTab;
    }

    /**
     * Returns the set of deprecated and unmaintained plugins.
     * @return set of deprecated and unmaintained plugins names.
     * @since 8938
     */
    public static Set<String> getDeprecatedAndUnmaintainedPlugins() {
        Set<String> result = new HashSet<>(DEPRECATED_PLUGINS.size() + UNMAINTAINED_PLUGINS.size());
        for (DeprecatedPlugin dp : DEPRECATED_PLUGINS) {
            result.add(dp.name);
        }
        result.addAll(UNMAINTAINED_PLUGINS);
        return result;
    }

    private static class UpdatePluginsMessagePanel extends JPanel {
        private final JMultilineLabel lblMessage = new JMultilineLabel("");
        private final JCheckBox cbDontShowAgain = new JCheckBox(
                tr("Do not ask again and remember my decision (go to Preferences->Plugins to change it later)"));

        UpdatePluginsMessagePanel() {
            build();
        }

        protected final void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.insets = new Insets(5, 5, 5, 5);
            add(lblMessage, gc);
            lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN));

            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weighty = 0.0;
            add(cbDontShowAgain, gc);
            cbDontShowAgain.setFont(cbDontShowAgain.getFont().deriveFont(Font.PLAIN));
        }

        public void setMessage(String message) {
            lblMessage.setText(message);
        }

        public void initDontShowAgain(String preferencesKey) {
            String policy = Main.pref.get(preferencesKey, "ask");
            policy = policy.trim().toLowerCase(Locale.ENGLISH);
            cbDontShowAgain.setSelected(!"ask".equals(policy));
        }

        public boolean isRememberDecision() {
            return cbDontShowAgain.isSelected();
        }
    }
}
