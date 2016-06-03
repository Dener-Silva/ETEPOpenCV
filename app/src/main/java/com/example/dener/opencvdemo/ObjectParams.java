package com.example.dener.opencvdemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Esta classe armazena objetos para serem recuperados de forma conveniente depois.
 * Created by dener on 31/05/2016.
 */
public class ObjectParams {
    Mat img_object;
    Mat descriptors_object;
    MatOfKeyPoint keypoints_object;
    List<KeyPoint> keypoints_object_list;

    FeatureDetector fd;
    DescriptorExtractor extractor;
    DescriptorMatcher matcher;

    ObjectParams(Activity activity) {
        //Criando objetos
        fd = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //Como o objeto não muda em tempo de execução, suas características serão processadas aqui.
        img_object = new Mat();
        descriptors_object = new Mat();
        keypoints_object = new MatOfKeyPoint();

        //Lendo arquivo PNG
        Drawable drawable = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.gabarito_kp10, null);

        if (drawable != null) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Utils.bitmapToMat(bitmap, img_object);
        } else {
            Log.e("Inicialização", "Imagem não encontrada");
        }

        //Pré-processando a imagem
        Imgproc.cvtColor(img_object, img_object, Imgproc.COLOR_BGR2GRAY);

        //Calculando KeyPoints
        fd.detect(img_object, keypoints_object);
        keypoints_object_list = keypoints_object.toList();

        //Computando descriptor
        extractor.compute(img_object, keypoints_object, descriptors_object);

        //Informando que a inicialização terminou
        Log.d("Inicialização", "Finalizado.");
        ((MainActivity_show_camera)activity).setState(State.Initialized);
    }
}