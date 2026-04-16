package mx.unam.fc.icat.focusmali.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager; // Asegúrate de tener la dependencia

import java.util.Locale;

import mx.unam.fc.icat.focusmali.R;

/**
 * Actividad encargada de gestionar las preferencias del usuario.
 * Implementa un Listener para reaccionar a cambios en los ajustes (como el idioma).
 * TODO: Asegurarse de agregar la dependencia de AndroidX Preference en el build.gradle.
 * @author <a href="mailto:mali@ciencias.unam.mx" > Malinalli Escobedo Iineo </a> - @mali1412
 */
public class PreferencesActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Inicialización de la Toolbar
        Toolbar toolbar = findViewById(R.id.preferences_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_preferences);
        }

        // Inicializamos las preferencias por defecto
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Reemplazamos el contenido de la actividad con el fragmento
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_content, new PreferencesFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registramos el listener para detectar cambios mientras la actividad es visible
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Desregistramos para evitar fugas de memoria.
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Callback que se dispara cuando el usuario cambia cualquier ajuste.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Caso Idioma
        if (key.equals(getString(R.string.lang_preference_key))) {
            String lang = sharedPreferences.getString(key, "es");
            applyLanguage(lang);

            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

        // Caso Tema (Oscuro / Claro / Sistema)
        if (key.equals(getString(R.string.theme_preference_key))) {
            String themeValue = sharedPreferences.getString(key, "system");
            applyTheme(themeValue);
        }
    }

    /**
     * TODO: Implementar este método para cambiar la configuración del idioma.
     */
    private void applyLanguage(String langCode) {
        // Configurar la baseContext con el nuevo Locale.
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        // Aplicamos la configuración al contexto de la app
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Guardamos explícitamente para que MainActivity lo sepa al iniciar
        sharedPreferences.edit().putString(getString(R.string.lang_preference_key), langCode).apply();
    }



    /**
     * Aplica el modo oscuro o claro según la preferencia del usuario.
     * @param themeValue Valores esperados: "light", "dark" o "system".
     */
    private void applyTheme(String themeValue) {
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                // Sigue la configuración del sistema operativo
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


}