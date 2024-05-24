package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class AlternateMenu implements Menu {
    public Activity activity;

    public AlternateMenu(Activity activity) {
        this.activity = activity;
    }
    ArrayList<MenuItem> items = new ArrayList<MenuItem>();
    public static void showAlternateMenu(Activity a) {
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        Menu currentMenu = new AlternateMenu(a);
        a.onCreateOptionsMenu(currentMenu);
        a.onPrepareOptionsMenu(currentMenu);
        final ArrayAdapter<CharSequence> options  = new ArrayAdapter<CharSequence>(a, android.R.layout.select_dialog_item);
        for (int i=0; i<currentMenu.size(); i++) {
            MenuItem m = currentMenu.getItem(i);
            if (m.isVisible())
                options.add(currentMenu.getItem(i).getTitle());
        }
        builder.setAdapter(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int item) {
                for (int i = 0 ; i < currentMenu.size(); i++) {
                    if (currentMenu.getItem(i).getTitle().equals(options.getItem(item))) {
                        a.onOptionsItemSelected(currentMenu.getItem(i));
                        return;
                    }
                }
            }
        });

        builder.create();
        builder.show();
    }

    @Override
    public MenuItem add(CharSequence charSequence) {
        return add(0,0,0,charSequence);
    }

    @Override
    public MenuItem add(int id) {
        return add(0,0,0,id);
    }

    @Override
    public MenuItem add(int i, int id, int i2, CharSequence charSequence) {
        MenuItem item = new MyMenuItem(id);
        items.add(item);
        return item.setTitle(charSequence);
    }

    @Override
    public MenuItem add(int i, int id, int i2, int titleRes) {
        MenuItem item = new MyMenuItem(id);
        items.add(item);
        return item.setTitle(titleRes);
    }

    @Override
    public SubMenu addSubMenu(CharSequence charSequence) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i, int i1, int i2, CharSequence charSequence) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i, int i1, int i2, int i3) {
        return null;
    }

    @Override
    public int addIntentOptions(int i, int i1, int i2, ComponentName componentName, Intent[] intents, Intent intent, int i3, MenuItem[] menuItems) {
        return 0;
    }

    @Override
    public void removeItem(int i) {
        items.remove(i);
    }

    @Override
    public void removeGroup(int i) {

    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public void setGroupCheckable(int i, boolean b, boolean b1) {

    }

    @Override
    public void setGroupVisible(int i, boolean b) {

    }

    @Override
    public void setGroupEnabled(int i, boolean b) {

    }

    @Override
    public boolean hasVisibleItems() {
        return false;
    }

    @Override
    public MenuItem findItem(int id) {
        for (int i = 0 ; i < items.size() ; i++)
            if (items.get(i).getItemId() == id) {
                return items.get(i);
            }
        return null;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public MenuItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean performShortcut(int i, KeyEvent keyEvent, int i1) {
        return false;
    }

    @Override
    public boolean isShortcutKey(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean performIdentifierAction(int i, int i1) {
        return false;
    }

    @Override
    public void setQwertyMode(boolean b) {

    }

    class MyMenuItem implements MenuItem {

        private final int id;
        private boolean visible = true;
        CharSequence title = "";

        public MyMenuItem(int id) {
            this.id = id;
        }

        @Override
        public int getItemId() {
            return id;
        }

        @Override
        public int getGroupId() {
            return 0;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public MenuItem setTitle(CharSequence charSequence) {
            title = charSequence;
            return this;
        }

        @Override
        public MenuItem setTitle(int res) {
            title = activity.getResources().getString(res);
            return this;
        }

        @Override
        public CharSequence getTitle() {
            return title;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence charSequence) {
            return this;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return title;
        }

        @Override
        public MenuItem setIcon(Drawable drawable) {
            return this;
        }

        @Override
        public MenuItem setIcon(int i) {
            return this;
        }

        @Override
        public Drawable getIcon() {
            return null;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            return this;
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char c, char c1) {
            return this;
        }

        @Override
        public MenuItem setNumericShortcut(char c) {
            return this;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char c) {
            return this;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public MenuItem setCheckable(boolean b) {
            return this;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public MenuItem setChecked(boolean b) {
            return this;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public MenuItem setVisible(boolean b) {
            visible = b;
            return this;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public MenuItem setEnabled(boolean b) {
            return this;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
            return this;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int i) {

        }

        @Override
        public MenuItem setShowAsActionFlags(int i) {
            return this;
        }

        @Override
        public MenuItem setActionView(View view) {
            return this;
        }

        @Override
        public MenuItem setActionView(int i) {
            return this;
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            return this;
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener onActionExpandListener) {
            return this;
        }
    }
}
