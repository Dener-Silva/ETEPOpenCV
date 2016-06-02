package com.example.dener.opencvdemo;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Faz o processamento necessário para retornar a imagem da folha na posição correta.
 * Created by dener on 31/05/2016.
 */
public class Homography {
    private final Stopwatch stopwatch = new Stopwatch();
    private final Size gaussianBlurSize = new Size(3, 3);
    private final Size homSize = new Size(3, 3);

    Mat calculate(Mat frame, ObjectParams objectParams) {
        stopwatch.start();
        Mat img_scene = frame.clone();

        //Passo 1: Pré-processamento
        preProcess(img_scene);
        Log.v("Homografia", "Pré-processamento: " + stopwatch.split() + "ms");

        //Detectando KeyPoints
        MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
        objectParams.fd.detect(img_scene, keypoints_scene);
        Log.v("Homografia", "Detectando KeyPoints: " + stopwatch.split() + "ms");

        //Caso a imagem seja totalmente preta, keypoints_scene não terá nada dentro.
        // Para evitar um IndexOutOfBoundsException, a função acaba aqui.
        if (keypoints_scene.size().equals(new Size(1, 0)) || objectParams.keypoints_object.size().equals(new Size(1, 0))) {
            return null;
        }

        //– Step 2: Calculate descriptors (feature vectors)
        Mat descriptors_scene = new Mat();
        objectParams.extractor.compute(img_scene, keypoints_scene, descriptors_scene);
        List<MatOfDMatch> matches = new LinkedList<>();
        objectParams.matcher.knnMatch(descriptors_scene, objectParams.descriptors_object, matches, 5);
        Log.v("Homografia", "Calculando descriptors: " + stopwatch.split() + "ms");

        // ratio test
        LinkedList<DMatch> good_matches = new LinkedList<>();
        for (MatOfDMatch matOfDMatch : matches) {
            DMatch[] matOfDMatch_array = matOfDMatch.toArray();
            if (matOfDMatch_array[0].distance / matOfDMatch_array[1].distance < 0.9) {
                good_matches.add(matOfDMatch_array[0]);
            }
        }

        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        List<Point> pts1 = new ArrayList<>();
        List<Point> pts2 = new ArrayList<>();
        List<KeyPoint> keypoints_scene_list = keypoints_scene.toList();
        for (int i = 0; i < good_matches.size(); i++) {
            pts1.add(keypoints_scene_list.get(good_matches.get(i).queryIdx).pt);
            pts2.add(objectParams.keypoints_object_list.get(good_matches.get(i).trainIdx).pt);
        }

        // convertion of data types - there is maybe a more beautiful way
        Mat outputMask = new Mat();
        MatOfPoint2f pts1Mat = new MatOfPoint2f();
        pts1Mat.fromList(pts1);
        MatOfPoint2f pts2Mat = new MatOfPoint2f();
        pts2Mat.fromList(pts2);

        Log.v("Homografia", "good_matches: " + stopwatch.split() + "ms");

        //Pelo menos 4 pontos em cada Mat são necessários para a homografia.
        if (pts1Mat.total() < 4 || pts2Mat.total() < 4)
            return null;

        // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
        // the smaller the allowed reprojection error (here 15), the more matches are filtered
        Mat revHomog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);
        Log.v("Homografia", "Homografia: " + stopwatch.split() + "ms");
        //O tamanho da matriz de transformação revHomog deve ser 3*3.
        // Caso não seja, é sinal de que a homografia falhou.
        if (!revHomog.size().equals(homSize) || !niceHomography(revHomog)) {
            return null;
        }
        Mat imgOut = new Mat(objectParams.img_object.size(), CvType.CV_8UC4);
        Imgproc.warpPerspective(img_scene, imgOut, revHomog, imgOut.size());

        // binariza imagem usando um threshold adaptativo
        Imgproc.adaptiveThreshold(imgOut, imgOut, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, 61, 15);

        Log.v("Homografia", "Warp Perspective: " + stopwatch.split() + "ms");
        return imgOut;
    }

    void preProcess(Mat img_scene) {
        Imgproc.cvtColor(img_scene, img_scene, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(img_scene, img_scene, gaussianBlurSize, 0);
    }

    boolean niceHomography(Mat h) {
        double det = h.get(0, 0)[0] * h.get(1, 1)[0] - h.get(1, 0)[0] * h.get(0, 1)[0];
        if (det < 0)
            return false;

        double N1 = Math.sqrt(h.get(0, 0)[0] * h.get(0, 0)[0] + h.get(1, 0)[0] * h.get(1, 0)[0]);
        if (N1 > 2 || N1 < 0.05)//Padrão if (N1 > 4 || N1 < 0.1)
            return false;

        double N2 = Math.sqrt(h.get(0, 1)[0] * h.get(0, 1)[0] + h.get(1, 1)[0] * h.get(1, 1)[0]);
        if (N2 > 2 || N2 < 0.05)//Padrão if (N2 > 4 || N2 < 0.1)
            return false;

        double N3 = Math.sqrt(h.get(2, 0)[0] * h.get(2, 0)[0] + h.get(2, 1)[0] * h.get(2, 1)[0]);
        return N3 <= 0.001; //Padrão return (N3 <= 0.002)
    }
}