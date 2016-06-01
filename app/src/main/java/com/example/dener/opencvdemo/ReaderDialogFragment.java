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
        void onDialogPositiveClick(DialogFragment dialog);
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
                    + " must implement NoticeDialogListener");
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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        mListener.onDialogPositiveClick(ReaderDialogFragment.this);
                    }
                });
        //<editor-fold desc="Definindo textViews">
        textViews = new TextView[] {
                (TextView)content.findViewById(R.id.questao1),
                (TextView)content.findViewById(R.id.questao2),
                (TextView)content.findViewById(R.id.questao3),
                (TextView)content.findViewById(R.id.questao4),
                (TextView)content.findViewById(R.id.questao5),
                (TextView)content.findViewById(R.id.questao6),
                (TextView)content.findViewById(R.id.questao7),
                (TextView)content.findViewById(R.id.questao8),
                (TextView)content.findViewById(R.id.questao9),
                (TextView)content.findViewById(R.id.questao10),
                (TextView)content.findViewById(R.id.questao11),
                (TextView)content.findViewById(R.id.questao12),
                (TextView)content.findViewById(R.id.questao13),
                (TextView)content.findViewById(R.id.questao14),
                (TextView)content.findViewById(R.id.questao15),
                (TextView)content.findViewById(R.id.questao16),
                (TextView)content.findViewById(R.id.ra),
                (TextView)content.findViewById(R.id.codigoDaProva),
                (TextView)content.findViewById(R.id.tipoDeProva),
        };
        setTextViews();
        //</editor-fold>
        return builder.create();
    }

    public void exibirProva (Prova prova) {
        this.p = prova;
    }

    private void setTextViews() {

        String raText = "RA: ";
        if (p.ra == null) {
            raText = raText.concat("Inválido");
        } else if (p.ra == 0) {
            raText = raText.concat("Em branco");
        } else {
            raText = raText.concat(p.ra.toString());
        }
        textViews[16].setText(raText);

        String codText = "Cód.: ";
        if (p.codigoDaProva == null) {
            codText = codText.concat("Inválido");
        } else if (p.codigoDaProva == 0) {
            codText = codText.concat("Em branco");
        } else {
            codText = codText.concat(p.codigoDaProva.toString());
        }
        textViews[17].setText(codText);

        String tipoText = "Tipo: ";
        textViews[18].setText(appendChecked(tipoText, p.tipo));

        for (int q = 0; q < p.questao.length; q++){
            String text = "Questão " + (q + 1) + ": ";
            textViews[q].setText(appendChecked(text, p.questao[q]));
        }
    }

    String appendChecked(String text, boolean[] q) {
        int quant = 0;
        //Contando a quantidade de alternativas marcadas.
        for (boolean b : q){
            if (b){
                quant++;
            }
        }
        //Se a quantidade for igual a 1, a questão é válida
        String ret = "";
        if (quant == 1) {
            for (int a = 0; a < q.length; a++){
                if (q[a]){
                    switch (a) {
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
                    }
                }
            }
        } else {
            if (quant == 0) {
                ret = text.concat("Em branco");
            } else {
                ret = text.concat("Inválida");
            }
        }
        return ret;
    }
}
