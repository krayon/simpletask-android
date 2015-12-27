package nl.mpcjanssen.simpletask;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

abstract class ThemedActivity extends AppCompatActivity {

    @Override
<<<<<<< HEAD
    protected void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
=======
    public void onCreate(Bundle savedInstanceState) {
        SimpletaskApplication app = (SimpletaskApplication) getApplication();
>>>>>>> origin/macroid
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        super.onCreate(savedInstanceState);
    }
}
