package com.example.dener.opencvdemo;

import android.util.Log;

import org.opencv.core.Mat;

/**
 * Created by dener on 28/05/2016.
 */
public class Leitor {
    Mat mat;
    int totalX, totalY;
    double threshold = 128;

    static class Questoes {
        static double maskSize = 0.021437578814628,
                step = 0.0239596469104666,
                stepY = 0.0240641711229946,
                posX1a8 = 0.353720050441362,
                posX9a16 = 0.711853720050441,
                posY = 0.660873440285205;
    }

    public void ReadTest(Mat test) {
        mat = test;
        Prova p = new Prova();
        for (int i = 0; i < 8; i++) {
            double y = Questoes.posY + ((double) i * Questoes.stepY);
            p.questao[i] = ReadQuestion(Questoes.posX1a8, y, Questoes.step, 5);
        }
        for (int i = 8; i < 16; i++) {
            double y = Questoes.posY + ((double) (i - 8) * Questoes.stepY);
            p.questao[i] = ReadQuestion(Questoes.posX9a16, y, Questoes.step, 5);
        }
        Log.d("t", "t");
    }

    /**
     * Retorna qual (quais) letras está (estão) marcadas. Os parâmetros devem ser dados como fração
     * do tamanho da folha (para manter invariabilidade de tamanho).
     *
     * @param startX Ponto X (topo esquerda) para iniciar a leitura
     * @param startY Ponto Y (topo esquerda) para iniciar a leitura
     * @param step   Espaço entre o canto esquerdo de cada alternativa (em proporção com a largura
     *               total)
     * @param size   Quantas letras a questão tem
     * @return Vetor com true nas questões marcadas
     */
    boolean[] ReadQuestion(double startX, double startY, double step, int size) {
        boolean[] b = new boolean[size];

        for (int i = 0; i < size; i++) {
            double x = startX + ((double) i * step);
            double avg = Average(x, startY, Questoes.maskSize);
            b[i] = avg < threshold;
        }
        return b;
    }

    /**
     * Retorna o número (RA ou código da prova) marcado no campo. Os parâmetros devem ser dados
     * como fração do tamanho da folha (para manter invariabilidade de tamanho).
     *
     * @param startX Ponto X (topo esquerda) para iniciar a leitura
     * @param startY Ponto Y (topo esquerda) para iniciar a leitura
     * @param step   Tamanho da máscara (em proporção com a largura total)
     * @param size   Quantos dígitos serão lidos
     * @return Número lido. Zero se o campo estiver vazio, nulo caso a leitura seja inválida.
     */
    Integer ReadCode(double startX, double startY, double step, int size) {
        return null;
    }

    double Average(double startX, double startY, double maskSize) {
        int startXPos = (int) Math.round((float) mat.cols() * startX);
        int endXPos = (int) Math.round((float) mat.cols() * (startX + maskSize));
        int startYPos = (int) Math.round((float) mat.rows() * startY);
        int endYPos = (int) Math.round((float) mat.rows() * (startY + maskSize));

        double sum = 0;
        int quant = (endXPos - startXPos) * (endYPos - startYPos);
        for (int i = startXPos; i < endXPos; i++) {
            for (int j = startYPos; j < endYPos; j++) {
                sum += mat.get(j, i)[0];
            }
        }
        return sum /(float) quant;
    }
}
