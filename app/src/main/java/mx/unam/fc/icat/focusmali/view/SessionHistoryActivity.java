package mx.unam.fc.icat.focusmali.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import mx.unam.fc.icat.focusmali.R;
import mx.unam.fc.icat.focusmali.model.Session;
import mx.unam.fc.icat.focusmali.model.SessionManager;

/**
 * Actividad que visualiza el historial cronológico de las sesiones de enfoque y descanso.
 * Se utiliza como práctica para el manejo de RecyclerView, adaptadores y filtrado de datos.
 * @author <a href="mailto:mali@ciencias.unam.mx" > Malinalli Escobedo Irineo </a> - @mali1412
 */
public class SessionHistoryActivity extends AppCompatActivity {

    // Componentes de la Interfaz de Usuario.
    private Toolbar toolbar;
    private TextView tvResultCount;
    private ConstraintLayout layoutEmpty;
    private RecyclerView recyclerView;

    // TODO: Declarar los componentes de filtrado (ChipGroup y Chips individuales).

    // Lógica y Datos.
    private SessionHistoryAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- CAMBIO COMMIT 4: Aplicar idioma antes de inflar la vista ---
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lang = settingsPrefs.getString(getString(R.string.lang_preference_key), "es");
        applyLanguage(lang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_session);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterLogic();
        updateHistoryDisplay();
    }

    /**
     * Método auxiliar para forzar el idioma en esta actividad.
     */
    private void applyLanguage(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    /**
     * Vincula las variables con los componentes del XML.
     */
    private void bindViews() {
        toolbar = findViewById(R.id.history_toolbar);
        tvResultCount = findViewById(R.id.tvResultCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        recyclerView = findViewById(R.id.recyclerViewHistory);

        // TODO: Vincular Chips mediante findViewById y asignar IDs correspondientes.

        sessionManager = SessionManager.getInstance(this);
    }

    /**
     * Configuración del sistema de filtrado por temporalidad.
     */
    private void setupFilterLogic() {
        // TODO (Opcional): Implementar el funcionamiento del ChipGroup (filtrado).
    }

    /**
     * Configura la Toolbar como ActionBar de la actividad.
     * Habilita el botón de retroceso (Up Navigation) y asigna el título
     * desde los recursos de cadena para soporte multi-idioma.
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // El título ahora es dinámico
            getSupportActionBar().setTitle(R.string.title_history);
        }
    }

    /**
     * Inicializa el RecyclerView con su LayoutManager y Adaptador.
     * Vincula la lista de sesiones obtenida del SessionManager con la
     * interfaz visual mediante el SessionHistoryAdapter.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Obtenemos los datos iniciales.
        List<Session> history = sessionManager.getHistory();

        // Inicializamos el adaptador pasando 'this' como contexto para acceder a recursos.
        adapter = new SessionHistoryAdapter(history, getResources());
        recyclerView.setAdapter(adapter);
    }

    /**
     * Gestiona la visibilidad de la UI y actualiza el contador.
     */
    private void updateHistoryDisplay() {
        List<Session> sessions = sessionManager.getHistory();
        boolean isEmpty = (sessions == null || sessions.isEmpty());

        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        int count = (sessions != null ? sessions.size() : 0);

        // Uso de string con formato para soporte multi-idioma.
        //String countText = getString(R.string.session_count, (sessions != null ? sessions.size() : 0));
        String countText = getResources().getQuantityString(R.plurals.session_count_plural, count, count);
        tvResultCount.setText(countText);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}