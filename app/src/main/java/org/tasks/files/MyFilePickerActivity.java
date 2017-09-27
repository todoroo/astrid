package org.tasks.files;

import android.os.Bundle;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.tasks.R;
import org.tasks.injection.ActivityModule;
import org.tasks.injection.InjectingApplication;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import static org.tasks.preferences.ResourceResolver.getData;

public class MyFilePickerActivity extends FilePickerActivity {

    @Inject Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ActivityModule(this))
                .inject(this);
        theme.applyThemeAndStatusBarColor(this, getDelegate());
        setTitle(null);
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(getData(this, R.attr.asContentBackground));
    }
}
