package com.example.dener.opencvdemo;

import android.util.Log;

import org.opencv.core.Mat;

/**
 * Lê a imagem da prova, e detecta o que foi marcado.
 * Created by dener on 28/05/2016.
 */
public class Leitor {
    Mat mat;
    int cols, rows;
    float threshold = 128;
    final int contarPixels = 2;
    Stopwatch stopwatch = new Stopwatch();
    double maskSize = 0.021437578814628;

    static class Questoes {
        static double
                step = 0.0239596469104666,
                stepY = 0.0240641711229946,
                posX1a8 = 0.353720050441362,
                posX9a16 = 0.711853720050441,
                posY = 0.660873440285205;
    }

    static class Tipo {
        static double
                step = 0.0239596469104666,
                posX = 0.197351828499369,
                posY = 0.631907308377897;
    }

    static class Campos {
        static double
                step = 0.0239596469104666,
                stepY = 0.0169340463458110,
                posXRA = 0.513240857503153,
                posXCod = 0.723833543505675,
                posY = 0.382352941176471;
    }

    public Prova LerProva(Mat test) {
        stopwatch.start();
        mat = test;
        rows = mat.rows();
        cols = mat.cols();
        Prova p = new Prova();
        p.ra = ReadCode(Campos.posXRA, Campos.posY, Campos.step, Campos.stepY, 8);
        p.codigoDaProva = ReadCode(Campos.posXCod, Campos.posY, Campos.step, Campos.stepY, 8);
        p.tipo = ReadQuestion(Tipo.posX, Tipo.posY, Tipo.step, 3);
        for (int i = 0; i < 8; i++) {
            double y = Questoes.posY + ((double) i * Questoes.stepY);
            p.questao[i] = ReadQuestion(Questoes.posX1a8, y, Questoes.step, 5);
        }
        for (int i = 8; i < 16; i++) {
            double y = Questoes.posY + ((double) (i - 8) * Questoes.stepY);
            p.questao[i] = ReadQuestion(Questoes.posX9a16, y, Questoes.step, 5);
        }
        Log.v("Leitura", "Leitura: " + stopwatch.getElapsedTime());
        return p;
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
            b[i] = Average(x, startY, maskSize);
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
    Integer ReadCode(double startX, double startY, double step, double stepY, int size) {
        Integer ret = 0;

        for (int i = 0; i < size; i++) {
            double x = startX + ((double) i * step);
            int quant = 0;
            for (int j = 0; j < 10; j++) {
                double y = startY + ((double) j * stepY);
                if (Average(x, y, maskSize)) {
                    quant++;
                    ret += j * (int) Math.pow (10, size - i - 1);
                }
                if (quant > 1) {
                    return null;
                }
            }
        }
        return ret;
    }

    boolean Average(double startX, double startY, double maskSize) {
        int startXPos = (int) Math.round((float) cols * startX);
        int endXPos = (int) Math.round((float) cols * (startX + maskSize));
        int startYPos = (int) Math.round((float) rows * startY);
        int endYPos = (int) Math.round((float) rows * (startY + maskSize));

        int light = 0;
        int dark = 0;
        float quant = ((endXPos - startXPos) * (endYPos - startYPos))/(contarPixels * contarPixels);
        int quantThreshold = (int)((threshold / 255f) * quant);
        for (int i = startXPos; i < endXPos; i+= contarPixels) {
            for (int j = startYPos; j < endYPos; j+= contarPixels) {
                if (mat.get(j, i)[0] > 0) {
                    light++;
                } else {
                    dark++;
                }
                if (light > quant - quantThreshold) {
                    return false;
                }
                if (dark > quantThreshold) {
                    return true;
                }
            }
        }
        return false;
    }
}