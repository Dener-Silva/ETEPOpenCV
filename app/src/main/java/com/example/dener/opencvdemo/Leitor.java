package com.example.dener.opencvdemo;

import android.util.Log;

import org.opencv.core.Mat;

/**
 * Lê a imagem da prova, e detecta o que foi marcado.
 * Created by dener on 28/05/2016.
 */
public class Leitor {
    //Imagem da prova
    Mat mat;
    //Colunas e linhas da imagem
    int cols, rows;
    //Limiar da detecção de alternativas assinaladas.
    //Um valor menor deixa a detecção mais sensível, mas pode causar falsos positivos.
    //Um valor maior pode causar falsos negativos.
    float threshold = 128;
    //Não é necessário computar todos os pixels para ter uma média relativamente boa.
    //O algoritmo ignora (contarPixels² - 1) pixels. Um valor maior aumenta muito a velocidade
    //da deteção, ao custo de confiabilidade.
    final int contarPixels = 2;

    Stopwatch stopwatch = new Stopwatch();

    //Pontos de interesse.
    //Os pontos devem ser dados como fração
    //do tamanho da folha (para manter invariabilidade de tamanho).
    //Todos são com relação ao topo e esquerda.
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

    /**Lê as alternativas assinaladas na prova.
     * @param test Imagem da prova. A imagem deve ser binarizada para funcionar corretamente.
     * @return Objeto Prova com as informações relevantes.
     */
    public Prova LerProva(Mat test, boolean ignorarInvalidos) {
        stopwatch.start();
        mat = test;
        rows = mat.rows();
        cols = mat.cols();
        Prova p = new Prova();
        //Lendo o RA e código da prova
        p.ra = ReadCode(Campos.posXRA, Campos.posY, Campos.step, Campos.stepY, 8);
        if (ignorarInvalidos && (p.ra == null || p.ra < 0))
            return null;
        p.codigoDaProva = ReadCode(Campos.posXCod, Campos.posY, Campos.step, Campos.stepY, 8);
        if (ignorarInvalidos && (p.codigoDaProva == null || p.codigoDaProva < 0))
            return null;
        //O tipo de prova é lido como se fosse uma questão (de 3 alternativas).
        p.tipo = ReadQuestion(Tipo.posX, Tipo.posY, Tipo.step, 3);
        //Lendo questões
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
     * Retorna qual letra está marcada. Os parâmetros devem ser dados como fração
     * do tamanho da folha (para manter invariabilidade de tamanho).
     *
     * @param startX Ponto X (topo esquerda) para iniciar a leitura
     * @param startY Ponto Y (topo esquerda) para iniciar a leitura
     * @param step   Espaço entre o canto esquerdo de cada alternativa (em proporção com a largura
     *               total)
     * @param size   Quantas letras a questão tem
     * @return Posição marcada. Nulo se o campo estiver vazio, -1 caso a leitura seja inválida.
     */
    Integer ReadQuestion(double startX, double startY, double step, int size) {
        int quant = 0;
        Integer ret = null;

        for (int i = 0; i < size; i++) {
            double x = startX + ((double) i * step);
            if (Average(x, startY)) {
                quant++;
                ret = i;
            }
            if(quant > 1) {
                return -1;
            }
        }
        return ret;
    }

    /**
     * Retorna o número (RA ou código da prova) marcado no campo. Os parâmetros devem ser dados
     * como fração do tamanho da folha (para manter invariabilidade de tamanho).
     *
     * @param startX Ponto X (topo esquerda) para iniciar a leitura
     * @param startY Ponto Y (topo esquerda) para iniciar a leitura
     * @param step   Tamanho da máscara (em proporção com a largura total)
     * @param size   Quantos dígitos serão lidos
     * @return Número lido. Nulo se o campo estiver vazio, -1 caso a leitura seja inválida.
     */
    Integer ReadCode(double startX, double startY, double step, double stepY, int size) {
        Integer ret = 0;

        for (int i = 0; i < size; i++) {
            double x = startX + ((double) i * step);
            int quant = 0;
            for (int j = 0; j < 10; j++) {
                double y = startY + ((double) j * stepY);
                if (Average(x, y)) {
                    quant++;
                    ret += j * (int) Math.pow (10, size - i - 1);
                }
                //Pode haver apenas um algarismo marcado. Caso contrário, a leitura é inválida.
                //Retornando agora economiza tempo.
                if (quant > 1) {
                    return -1;
                }
                //Se não há nada marcado, a leitura é inválida.
                //Retornando agora economiza tempo.
                if (j == 9 && quant != 1) {
                    return null;
                }
            }
        }
        return ret;
    }

    /**
     * Tira a média dos valores dos pixels. Retorna se esta alternativa foi assinalada.
     * A imagem de entrada deve ser binarizada para funcionar corretamente.
     * @param startX Ponto X (topo esquerda) para iniciar a leitura
     * @param startY Ponto Y (topo esquerda) para iniciar a leitura
     * @return Se esta alternativa foi assinalada.
     */
    boolean Average(double startX, double startY) {
        int startXPos = (int) Math.round((float) cols * startX);
        double maskSizeX = 0.021437578814628;
        int endXPos = (int) Math.round((float) cols * (startX + maskSizeX));
        int startYPos = (int) Math.round((float) rows * startY);
        double maskSizeY = 0.015151515151515;
        int endYPos = (int) Math.round((float) rows * (startY + maskSizeY));

        //Apenas é necessário visitar os pixels até o ponto em que a média já é
        //garantida estar acima ou abaixo de threshold. Isso aumenta bastante a
        //velociadade de detecção sem nenhum efeito colateral.
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