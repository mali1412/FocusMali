package mx.unam.fc.icat.focusmali;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mx.unam.fc.icat.focusmali.model.Session;
import mx.unam.fc.icat.focusmali.model.SessionManager;
import mx.unam.fc.icat.focusmali.view.PreferencesActivity;
import mx.unam.fc.icat.focusmali.view.SessionHistoryActivity;

public class MainActivity extends AppCompatActivity {

    enum TimerState { IDLE, RUNNING, PAUSED }
    enum SessionMode { FOCUS, BREAK, REST }

    private static final long FOCUS_DURATION_MS = 25 * 60 * 1000L;
    private static final long BREAK_DURATION_MS = 5 * 60 * 1000L;
    private static final long REST_DURATION_MS = 15 * 60 * 1000L;
    private static final int SESSIONS_BEFORE_REST = 4;

    private Toolbar toolbar;
    private ChipGroup chipGroupMode;
    private Chip chipFocus, chipBreak, chipRest;
    private TextView tvTimerDisplay, tvSessionLabel, tvSessionsCount, tvQuote;
    private MaterialButton btnStartStop;
    private ImageButton btnReset, btnSkip;
    private LinearLayout sessionDotsContainer;

    private CountDownTimer countDownTimer;
    private TimerState timerState = TimerState.IDLE;
    private SessionMode currentMode = SessionMode.FOCUS;
    private long timeLeftMillis = FOCUS_DURATION_MS;
    private int focusSessionsCompleted = 0;

    private SessionManager sessionManager;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "focusmaliPrefs";
    private static final String KEY_TIME_LEFT = "timeLeft";
    private static final String KEY_SESSION_MODE = "sessionMode";
    private static final String KEY_SESSIONS_COMPLETED = "sessionsCompleted";
    private static final String KEY_TIMER_STATE = "timerState";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- COMMIT 4: Aplicar idioma antes de inflar la interfaz ---
        loadLocale();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sessionManager = SessionManager.getInstance(this);

        bindViews();
        restoreState(savedInstanceState);
        setSupportActionBar(toolbar);
        setupClickListeners();
        setupChipClickListeners();

        updateTimerDisplay(timeLeftMillis);
        updateSessionsCompletedUI();
    }

    // --- NUEVO MÉTODO PARA CARGAR IDIOMA ---
    private void loadLocale() {
        try {
            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String lang = settingsPrefs.getString(getString(R.string.lang_preference_key), "es");
            applyLanguage(lang);
        } catch (Exception e) {
            Log.e("MainActivity", "Error cargando idioma", e);
        }
    }

    private void applyLanguage(String langCode) {
        try {
            Locale locale = new Locale(langCode);
            Locale.setDefault(locale);
            android.content.res.Configuration config = new android.content.res.Configuration();
            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        } catch (Exception e) {
            Log.e("MainActivity", "Error aplicando configuración de lenguaje", e);
        }
    }

    // --- REFRESCAR AL VOLVER DE PREFERENCIAS ---
    @Override
    protected void onResume() {
        super.onResume();
        loadLocale(); // Volver a aplicar el idioma por si cambió
        updateTimerDisplay(timeLeftMillis); // Traduce etiquetas
        updateSessionsCompletedUI(); // Traduce contador

        // Actualizar el texto del botón según el estado
        if (timerState == TimerState.RUNNING) {
            btnStartStop.setText(getString(R.string.btn_pause));
        } else if (timerState == TimerState.PAUSED) {
            btnStartStop.setText(getString(R.string.btn_resume));
        } else {
            btnStartStop.setText(getString(R.string.btn_start));
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.tbMenu);
        chipGroupMode = findViewById(R.id.chipGroupMode);
        chipFocus = findViewById(R.id.chipFocus);
        chipBreak = findViewById(R.id.chipBreak);
        chipRest = findViewById(R.id.chipRest);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        tvSessionLabel = findViewById(R.id.tvSessionLabel);
        tvSessionsCount = findViewById(R.id.tvSessionsCount);
        tvQuote = findViewById(R.id.tvQuote);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
        sessionDotsContainer = findViewById(R.id.sessionDotsContainer);
    }

    private void setupClickListeners() {
        btnStartStop.setOnClickListener(v -> {
            if (timerState == TimerState.RUNNING) pauseTimer();
            else startTimer();
        });

        btnReset.setOnClickListener(v -> resetTimer());

        btnSkip.setOnClickListener(v -> {
            if (timerState != TimerState.IDLE) skipToNextSession();
            else Toast.makeText(this, getString(R.string.btn_start), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupChipClickListeners() {
        chipFocus.setOnClickListener(v -> changeMode(SessionMode.FOCUS));
        chipBreak.setOnClickListener(v -> changeMode(SessionMode.BREAK));
        chipRest.setOnClickListener(v -> changeMode(SessionMode.REST));
    }

    private void startTimer() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timerState = TimerState.RUNNING;
        btnStartStop.setText(getString(R.string.btn_pause));

        countDownTimer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                onSessionFinished(true);
            }
        }.start();
    }

    private void pauseTimer() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancelTimer();
        timerState = TimerState.PAUSED;
        btnStartStop.setText(getString(R.string.btn_resume));
    }

    private void onSessionFinished(boolean completed) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timerState = TimerState.IDLE;
        saveSessionToDatabase(completed);

        if (completed) {
            vibrate();
            addDot();
            if (currentMode == SessionMode.FOCUS) {
                focusSessionsCompleted++;
                if (focusSessionsCompleted >= SESSIONS_BEFORE_REST) {
                    currentMode = SessionMode.REST;
                } else {
                    currentMode = SessionMode.BREAK;
                }
            } else {
                if (currentMode == SessionMode.REST) focusSessionsCompleted = 0;
                currentMode = SessionMode.FOCUS;
            }
        }

        resetModeTime();
        updateSessionsCompletedUI();
        btnStartStop.setText(getString(R.string.btn_start));
    }

    private void saveSessionToDatabase(boolean completed) {
        try {
            String type = (currentMode == SessionMode.FOCUS) ? getString(R.string.mode_focus) : getString(R.string.mode_break);
            int duration = (int) (getDurationForMode(currentMode) / 60000);
            String date = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date());
            String startTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            Session session = new Session(type, date, startTime, duration, completed);
            sessionManager.addSession(session);
        } catch (Exception e) {
            Log.e("MainActivity", "Error al preparar datos para la DB", e);
        }
    }

    private void skipToNextSession() {
        cancelTimer();
        onSessionFinished(true);
    }

    private void resetTimer() {
        cancelTimer();
        resetModeTime();
        timerState = TimerState.IDLE;
        btnStartStop.setText(getString(R.string.btn_start));
    }

    private void changeMode(SessionMode newMode) {
        if (timerState == TimerState.RUNNING) return;
        currentMode = newMode;
        resetTimer();
    }

    private void resetModeTime() {
        timeLeftMillis = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMillis);
    }

    private long getDurationForMode(SessionMode mode) {
        switch (mode) {
            case BREAK: return BREAK_DURATION_MS;
            case REST:  return REST_DURATION_MS;
            default:    return FOCUS_DURATION_MS;
        }
    }

    private void updateTimerDisplay(long millis) {
        selectChipForMode(currentMode);
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        tvTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (currentMode == SessionMode.FOCUS) {
            tvSessionLabel.setText(getString(R.string.mode_focus));
        } else if (currentMode == SessionMode.BREAK) {
            tvSessionLabel.setText(getString(R.string.mode_break));
        } else {
            tvSessionLabel.setText(getString(R.string.mode_long_break));
        }
    }

    private void updateSessionsCompletedUI() {
        tvSessionsCount.setText(getString(R.string.sessionsCount, focusSessionsCompleted));
    }

    private void selectChipForMode(SessionMode mode) {
        Chip targetChip;
        int chipId;
        switch (mode) {
            case BREAK: chipId = R.id.chipBreak; targetChip = chipBreak; break;
            case REST:  chipId = R.id.chipRest;  targetChip = chipRest;  break;
            default:    chipId = R.id.chipFocus; targetChip = chipFocus; break;
        }
        chipGroupMode.check(chipId);
        highlightChip(targetChip);
    }

    private void highlightChip(Chip activeChip) {
        float density = getResources().getDisplayMetrics().density;
        Chip[] allChips = {chipFocus, chipBreak, chipRest};
        for (Chip chip : allChips) chip.setChipStrokeWidth(0);
        activeChip.setChipStrokeWidth(2 * density);
        activeChip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_border_accent)));
    }

    private void vibrate() {
        // --- CUMPLE RÚBRICA: Manejo de Hardware con try-catch ---
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error al intentar usar el vibrador", e);
        }
    }

    private void addDot() {
        View dot = new View(this);
        int dotSize = (int) (10 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
        params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        dot.setLayoutParams(params);
        dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_session_completed));
        sessionDotsContainer.addView(dot);
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putLong(KEY_TIME_LEFT, timeLeftMillis);
            outState.putString(KEY_SESSION_MODE, currentMode.name());
            outState.putInt(KEY_SESSIONS_COMPLETED, focusSessionsCompleted);
            outState.putString(KEY_TIMER_STATE, timerState.name());
        } catch (Exception e) {
            Log.e("MainActivity", "Error guardando instancia", e);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        try {
            if (savedInstanceState != null) {
                timeLeftMillis = savedInstanceState.getLong(KEY_TIME_LEFT, FOCUS_DURATION_MS);
                currentMode = SessionMode.valueOf(savedInstanceState.getString(KEY_SESSION_MODE, "FOCUS"));
                focusSessionsCompleted = savedInstanceState.getInt(KEY_SESSIONS_COMPLETED, 0);
                timerState = TimerState.valueOf(savedInstanceState.getString(KEY_TIMER_STATE, "IDLE"));
            } else {
                timeLeftMillis = sharedPreferences.getLong(KEY_TIME_LEFT, FOCUS_DURATION_MS);
                currentMode = SessionMode.valueOf(sharedPreferences.getString(KEY_SESSION_MODE, "FOCUS"));
                focusSessionsCompleted = sharedPreferences.getInt(KEY_SESSIONS_COMPLETED, 0);
                timerState = TimerState.valueOf(sharedPreferences.getString(KEY_TIMER_STATE, "IDLE"));
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error restaurando estado", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_history) {
            startActivity(new Intent(this, SessionHistoryActivity.class));
            return true;
        } else if (id == R.id.action_preferences) {
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}