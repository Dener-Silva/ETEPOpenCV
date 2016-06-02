package com.example.dener.opencvdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Caixa de diálogo que exibe o resultado ao usuário.
 * Created by dener on 01/06/2016.
 */
public class ReaderDialogFragment extends DialogFragment {

    Prova p;

    private TextView[] textViews;


    public interface ReaderDialogListener {
        void onDialogDismissed(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    ReaderDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ReaderDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ReaderDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View content = inflater.inflate(R.layout.dialog, null);
        builder.setView(content)
                .setTitle(R.string.prova)
                // Add action buttons
                .setPositiveButton(R.string.ok, null);
        //<editor-fold desc="Definindo textViews">
        textViews = new TextView[]{
                (TextView) content.findViewById(R.id.questao1),
                (TextView) content.findViewById(R.id.questao2),
                (TextView) content.findViewById(R.id.questao3),
                (TextView) content.findViewById(R.id.questao4),
                (TextView) content.findViewById(R.id.questao5),
                (TextView) content.findViewById(R.id.questao6),
                (TextView) content.findViewById(R.id.questao7),
                (TextView) content.findViewById(R.id.questao8),
                (TextView) content.findViewById(R.id.questao9),
                (TextView) content.findViewById(R.id.questao10),
                (TextView) content.findViewById(R.id.questao11),
                (TextView) content.findViewById(R.id.questao12),
                (TextView) content.findViewById(R.id.questao13),
                (TextView) content.findViewById(R.id.questao14),
                (TextView) content.findViewById(R.id.questao15),
                (TextView) content.findViewById(R.id.questao16),
                (TextView) content.findViewById(R.id.ra),
                (TextView) content.findViewById(R.id.codigoDaProva),
                (TextView) content.findViewById(R.id.tipoDeProva),
        };
        setTextViews();
        //</editor-fold>
        return builder.create();
    }

    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        mListener.onDialogDismissed(this);
    }

    /**
     * Define o objeto Prova que será exibido
     *
     * @param prova Objeto Prova que será exibido
     */
    public void exibirProva(Prova prova) {
        this.p = prova;
    }

    /**
     * Gera os textos a serem exibidos na caixa de diáligo.
     */
    private void setTextViews() {
        String raText = "RA: ";
        textViews[16].setText(appendCode(raText, p.ra));

        String codText = "Cód.: ";
        textViews[17].setText(appendCode(codText, p.codigoDaProva));

        String tipoText = "Tipo: ";
        textViews[18].setText(appendChecked(tipoText, p.tipo, false));

        for (int q = 0; q < p.questao.length; q++) {
            String text = "Questão " + (q + 1) + ": ";
            textViews[q].setText(appendChecked(text, p.questao[q], true));
        }
    }

    /**
     * Adiciona a alternativa marcada ao fim do texto.
     * Adiciona "Em branco" se não houver nada marcado,
     * ou "Inválida" se houver mais de uma alternativa marcada.
     *
     * @param text Texto inicial
     * @param q    Questão
     * @return Texto final
     */
    String appendChecked(String text, Integer q, boolean feminino) {
        String ret;
        if (q == null)
            ret = text.concat("Em branco");
        else {
            switch (q) {
                case 0:
                    ret = text.concat("A");
                    break;
                case 1:
                    ret = text.concat("B");
                    break;
                case 2:
                    ret = text.concat("C");
                    break;
                case 3:
                    ret = text.concat("D");
                    break;
                case 4:
                    ret = text.concat("E");
                    break;
                case -1:
                    if (feminino) {
                        ret = text.concat("Inválida");
                    } else {
                        ret = text.concat("Inválido");
                    }
                    break;
                default:
                    ret = text.concat("Erro");
            }
        }
        return ret;
    }

    /**
     * Adiciona o valor numérico ao fim do texto.
     * Adiciona "Em branco" se não houver nada marcado,
     * ou "Inválido" se houver mais de uma alternativa marcada.
     *
     * @param text    Texto inicial
     * @param integer Valor
     * @return Texto final
     */
    String appendCode(String text, Integer integer) {
        String ret;
        if (integer == null) {
            ret = text.concat("Em branco");
        } else if (integer == -1) {
            ret = text.concat("Inválido");
        } else {
            ret = text.concat(integer.toString());
        }
        return ret;
    }
}
