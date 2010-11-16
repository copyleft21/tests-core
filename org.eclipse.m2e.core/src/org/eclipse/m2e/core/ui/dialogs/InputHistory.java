
package org.eclipse.m2e.core.ui.dialogs;

import java.beans.Beans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;

import org.eclipse.m2e.core.MavenPlugin;


public class InputHistory {
  /** the history limit */
  protected static final int MAX_HISTORY = 10;

  /** dialog settings to store input history */
  protected IDialogSettings dialogSettings;

  /** the Map of field ids to List of comboboxes that share the same history */
  private Map<String, List<ControlWrapper>> comboMap;

  private List<String> privileged;

  public InputHistory(String sectionName) {
    this(sectionName, new String[0]);
  }

  public InputHistory(String sectionName, String[] privileged) {
    comboMap = new HashMap<String, List<ControlWrapper>>();

    MavenPlugin plugin = MavenPlugin.getDefault();
    if(plugin != null) {
      IDialogSettings pluginSettings = plugin.getDialogSettings();
      dialogSettings = pluginSettings.getSection(sectionName);
      if(dialogSettings == null) {
        dialogSettings = pluginSettings.addNewSection(sectionName);
        pluginSettings.addSection(dialogSettings);
      }
    }
    assert privileged != null;
    this.privileged = Arrays.asList(privileged);
  }

  /** Loads the input history from the dialog settings. */
  public void load() {
    if(Beans.isDesignTime()) {
      return;
    }

    for(Map.Entry<String, List<ControlWrapper>> e : comboMap.entrySet()) {
      String id = e.getKey();
      Set<String> items = new LinkedHashSet<String>();
      String[] itemsArr = dialogSettings.getArray(id);
      items.addAll(privileged);
      if(itemsArr != null) {
        items.addAll(Arrays.asList(itemsArr));
      }
      for(ControlWrapper wrapper : e.getValue()) {
        if(!wrapper.isDisposed()) {
          wrapper.setItems(items.toArray(new String[0]));
        }
      }
    }
  }

  /** Saves the input history into the dialog settings. */
  public void save() {
    if(Beans.isDesignTime()) {
      return;
    }

    for(Map.Entry<String, List<ControlWrapper>> e : comboMap.entrySet()) {
      String id = e.getKey();

      Set<String> history = new LinkedHashSet<String>(MAX_HISTORY);

      for(ControlWrapper wrapper : e.getValue()) {
        String lastValue = wrapper.text;
        if(lastValue != null && lastValue.trim().length() > 0) {
          history.add(lastValue);
        }
      }

      ControlWrapper wrapper = e.getValue().iterator().next();
      String[] items = wrapper.items;
      if(items != null) {
        for(int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ ) {
          // do not store the privileged items if they are not selected.
          // we eventually inject the same or different set next time
          if(!privileged.contains(items[j])) {
            history.add(items[j]);
          }
        }
      }

      dialogSettings.put(id, history.toArray(new String[history.size()]));
    }
  }

  /** Adds an input control to the list of fields to save. */
  public void add(Control combo) {
    add(null, combo);
  }

  /** Adds an input control to the list of fields to save. */
  public void add(String id, final Control combo) {
    if(combo != null) {
      if(id == null) {
        id = String.valueOf(combo.getData("name"));
      }
      List<ControlWrapper> combos = comboMap.get(id);
      if(combos == null) {
        combos = new ArrayList<ControlWrapper>();
        comboMap.put(id, combos);
      }
      if(combo instanceof Combo) {
        combos.add(new ComboWrapper((Combo) combo));
      } else if(combo instanceof CCombo) {
        combos.add(new CComboWrapper((CCombo) combo));
      }
    }
  }

  abstract private class ControlWrapper {
    protected Control control;

    protected String text;

    protected String[] items;

    protected ControlWrapper(Control control) {
      this.control = control;
      control.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          text = getText();
          items = getItems();
        }
      });
    }

    protected boolean isDisposed() {
      return control.isDisposed();
    }

    abstract protected String getText();

    abstract protected String[] getItems();

    abstract protected void setItems(String[] items);
  }

  private class ComboWrapper extends ControlWrapper {
    private Combo combo;

    protected ComboWrapper(Combo combo) {
      super(combo);
      this.combo = combo;
    }

    protected String getText() {
      return combo.getText();
    }

    protected String[] getItems() {
      return combo.getItems();
    }

    protected void setItems(String[] items) {
      String value = combo.getText();
      combo.setItems(items);
      if(value.length() > 0) {
        // setItems() clears the text input, so we need to restore it
        combo.setText(value);
      } else if(items.length > 0) {
        combo.setText(items[0]);
      }
    }
  }

  private class CComboWrapper extends ControlWrapper {
    private CCombo combo;

    protected CComboWrapper(CCombo combo) {
      super(combo);
      this.combo = combo;
    }

    protected String getText() {
      return combo.getText();
    }

    protected String[] getItems() {
      return combo.getItems();
    }

    protected void setItems(String[] items) {
      String value = combo.getText();
      combo.setItems(items);
      if(value.length() > 0) {
        // setItems() clears the text input, so we need to restore it
        combo.setText(value);
      }
    }
  }
}
