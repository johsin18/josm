// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.ListSelectionModel;
import javax.swing.JPopupMenu;
import javax.swing.table.AbstractTableModel;
import javax.swing.JScrollPane;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.data.osm.Filters;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.actions.search.SearchAction;

/**
 *
 * @author Petr_Dlouhý
 */
public class FilterDialog extends ToggleDialog implements DataChangeListener, LayerChangeListener {
    private JTable userTable;
    private Filters filters = new Filters();
    private SideButton addButton;
    private SideButton editButton;
    private SideButton deleteButton;
    private SideButton upButton;
    private SideButton downButton;
    private JPopupMenu popupMenu;

    public FilterDialog(){
       super(tr("Filter"), "filter", tr("Filter objects and hide/disable them."),
               Shortcut.registerShortcut("subwindow:filter", tr("Toggle: {0}", tr("Filter")), KeyEvent.VK_F, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 162);

       Layer.listeners.add(this);
       build();
    }

    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel(new GridLayout(1, 4));

        addButton = new SideButton(marktr("Add"), "add", "SelectionList", tr("Add filter."),
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    Filter filter = (Filter)SearchAction.showSearchDialog(new Filter());
                    if(filter != null){
                       filters.addFilter(filter);
                       filters.filter();
                    }
                 }
              });
        pnl.add(addButton);

        editButton = new SideButton(marktr("Edit"), "edit", "SelectionList", tr("Edit filter."),
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    int index = userTable.getSelectionModel().getMinSelectionIndex();
                    if(index < 0) return;
                    Filter f = filters.getFilter(index);
                    Filter filter = (Filter)SearchAction.showSearchDialog(f);
                    if(filter != null){
                       filters.setFilter(index, filter);
                       filters.filter();
                    }
                 }
              });
        pnl.add(editButton);

        deleteButton = new SideButton(marktr("Delete"), "delete", "SelectionList", tr("Delete filter."),
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    int index = userTable.getSelectionModel().getMinSelectionIndex();
                    if(index < 0) return;
                    filters.removeFilter(index);
                 }
              });
        pnl.add(deleteButton);

        upButton = new SideButton(marktr("Up"), "up", "SelectionList", tr("Move filter up."),
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    int index = userTable.getSelectionModel().getMinSelectionIndex();
                    if(index < 0) return;
                    filters.moveUpFilter(index);
                    userTable.getSelectionModel().setSelectionInterval(index-1, index-1);
                 }
              });
        pnl.add(upButton);
        
        downButton = new SideButton(marktr("Down"), "down", "SelectionList", tr("Move filter down."),
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    int index = userTable.getSelectionModel().getMinSelectionIndex();
                    if(index < 0) return;
                    filters.moveDownFilter(index);
                    userTable.getSelectionModel().setSelectionInterval(index+1, index+1);
                 }
              });
        pnl.add(downButton);
        return pnl;
    }

    protected String[] columnToolTips = {
        tr("Filter elements"),
        tr("Disable elements"),
        tr("Apply also for children"),
        tr("Inverse filter"),
        null,
        tr("Filter mode")
    };

    protected void build() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        userTable = new JTable(filters){
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                   public String getToolTipText(MouseEvent e) {
                       String tip = null;
                       java.awt.Point p = e.getPoint();
                       int index = columnModel.getColumnIndexAtX(p.x);
                       int realIndex = columnModel.getColumn(index).getModelIndex();
                       return columnToolTips[realIndex];
                   }
               };
           }
        };   
  
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userTable.getColumnModel().getColumn(0).setMaxWidth(1);
        userTable.getColumnModel().getColumn(1).setMaxWidth(1);
        userTable.getColumnModel().getColumn(3).setMaxWidth(1);
        userTable.getColumnModel().getColumn(4).setMaxWidth(1);
        userTable.getColumnModel().getColumn(5).setMaxWidth(1);

        userTable.getColumnModel().getColumn(0).setResizable(false);
        userTable.getColumnModel().getColumn(1).setResizable(false);
        userTable.getColumnModel().getColumn(3).setResizable(false);
        userTable.getColumnModel().getColumn(4).setResizable(false);
        userTable.getColumnModel().getColumn(5).setResizable(false);

        pnl.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // -- the button row
        pnl.add(buildButtonRow(), BorderLayout.SOUTH);
        /*userTable.addMouseListener(new DoubleClickAdapter());*/
        add(pnl, BorderLayout.CENTER);
    }

    public void layerRemoved(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.remove(this);
        }
    }

    public void layerAdded(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.add(this);
        }
    }

   public void activeLayerChange(Layer oldLayer, Layer newLayer) {
      filters.filter();
   }

   public void dataChanged(OsmDataLayer l){
      filters.filter();
   }
}
