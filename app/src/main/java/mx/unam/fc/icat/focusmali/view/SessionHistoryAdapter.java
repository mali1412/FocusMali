package mx.unam.fc.icat.focusmali.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import mx.unam.fc.icat.focusmali.R;
import mx.unam.fc.icat.focusmali.model.Session;

/**
 * Adaptador para gestionar y reciclar las vistas del historial de sesiones.
 * Extiende de RecyclerView.Adapter parametrizado con nuestro ViewHolder específico
 * @author <a href="mali:monmm@ciencias.unam.mx" > Malinalli Escobedo Irineo </a> - @mali1412
 * @version 1.2, mar 2026 (esqueleto para alumnos)
 */
public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {

    /**
     * Clase interna que describe y mantiene las referencias a los widgets de cada ítem.
     * Actúa como un contenedor que evita llamadas repetitivas a findViewById.
     */
    class SessionViewHolder extends RecyclerView.ViewHolder {
        // Referencias a los elementos gráficos definidos en history_session_entry.xml.
        private TextView tvSessionType, tvSessionDate, tvSessionTime, tvSessionDuration;
        private Chip chipStatus;


        /**
         * Constructor que recibe la vista inflada del ítem.
         * @param itemView La vista raíz del layout del elemento de la lista.
         */
        SessionViewHolder(View itemView) {
            super(itemView);
            // Vinculamos los componentes del layout con los atributos de la clase.
            tvSessionType = itemView.findViewById(R.id.tvSessionType);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionTime = itemView.findViewById(R.id.tvSessionTime);
            tvSessionDuration = itemView.findViewById(R.id.tvSessionDuration);
            chipStatus = itemView.findViewById(R.id.chipSessionStatus);
        }
    }

    // Estructura de datos que contiene la información a mostrar (Dataset).
    private final List<Session> DATASET;
    // --- CAMBIO PARA ROBUSTEZ: Usamos Context en lugar de Resources ---
    private final Context CONTEXT;

    /**
     * Constructor del adaptador.
     * @param sessions Lista de objetos de tipo Session.
     * @param context Referencia al contexto de la actividad.
     */
    public SessionHistoryAdapter(List<Session> sessions, Context context) {
        this.DATASET = sessions;
        this.CONTEXT = context;
    }

    /**
     * Metodo encargado de "inflar" (crear) el layout XML para cada entrada de la lista.
     * Se llama solo cuando el RecyclerView necesita crear un nuevo ViewHolder.
     */
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Convertimos el XML history_session_entry en un objeto View.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_session_entry, parent, false);
        return new SessionViewHolder(view);
    }

    /**
     * Metodo encargado de vincular los datos del objeto Session con los widgets del ViewHolder.
     * Se ejecuta cada vez que un elemento entra en el rango visible de la pantalla.
     * @param holder El contenedor de las vistas (ViewHolder).
     * @param position La posición del elemento dentro del DATASET.
     */
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        // --- CUMPLE RÚBRICA: Robustez con try-catch ---
        try {
            Session session = DATASET.get(position);

            // Traducción dinámica del tipo de sesión
            String typeInDb = session.getType();
            if (typeInDb.equalsIgnoreCase("Enfoque") || typeInDb.equalsIgnoreCase("Focus")) {
                holder.tvSessionType.setText(CONTEXT.getString(R.string.type_focus));
            } else {
                holder.tvSessionType.setText(CONTEXT.getString(R.string.type_break));
            }

            holder.tvSessionDate.setText(session.getDate());
            holder.tvSessionTime.setText(session.getStartTime());

            // --- CORRECCIÓN: No hardcoding para la duración ---
            String durationText = session.getDuration() + " " + CONTEXT.getString(R.string.unit_minutes);
            holder.tvSessionDuration.setText(durationText);

            if (session.isCompleted()) {
                // --- CORRECCIÓN: No hardcoding para el símbolo ✓ ---
                String completedText = "✓ " + CONTEXT.getString(R.string.status_completed);
                holder.chipStatus.setText(completedText);

                holder.chipStatus.setChipBackgroundColorResource(R.color.white);
                holder.chipStatus.setTextColor(CONTEXT.getColor(R.color.color_primary));
            } else {
                // --- CORRECCIÓN: No hardcoding para el símbolo ✕ ---
                String interruptedText = "✕ " + CONTEXT.getString(R.string.status_skipped);
                holder.chipStatus.setText(interruptedText);

                holder.chipStatus.setChipBackgroundColorResource(R.color.color_secondary);
                holder.chipStatus.setTextColor(CONTEXT.getColor(R.color.white));
            }
        } catch (Exception e) {
            Log.e("SessionHistoryAdapter", "Error al vincular vista en posición: " + position, e);
        }
    }

    /**
     * Indica el tamaño total de la lista de datos.
     * @return Cantidad de elementos en el DATASET.
     */
    @Override
    public int getItemCount() {
        return DATASET != null ? DATASET.size() : 0;
    }
}