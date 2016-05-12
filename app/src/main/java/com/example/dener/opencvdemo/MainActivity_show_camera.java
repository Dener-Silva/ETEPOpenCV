package com.example.dener.opencvdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

// OpenCV Classes

public class MainActivity_show_camera extends AppCompatActivity implements CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Used in Camera selection from menu (when implemented)
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    //Índice das permissões. Neste caso há apenas a câmera.
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    //Thread usada para processamento em segundo plano
    private Thread t;
    //Estado atual (não tem muita função por enquanto)
    private State state = State.Initializing;

    /**
     * Armazena o frame atual para que a thread de processamento o leia.
     * Não atribua valores diretamente a esta variável! Utilize Setter e Getter para evitar conflitos.
     */
    private Mat mRgba;
    private Mat descriptors_object;
    private Size gaussianBlurSize = new Size(9, 9);
    private Mat img_object;
    private MatOfKeyPoint keypoints_object;
    private FeatureDetector fd;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;
    //    private CLAHE clahe;
    private double max_dist = 0, min_dist = 100;
    Bitmap matchesBmp;

    public MainActivity_show_camera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.show_camera);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        //Checando permissão de acesso à câmera. Caso haja permissão, conectar à câmera.
        //Caso não haja, solicita a permissão ao usuário.
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getContext(),
                    new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            ExibirCamera();
        }
        //
    }

    @Override
    public void onResume() {
        super.onResume();
        IniciarOpenCV();
    }

    void IniciarOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        if (state == State.Initializing) {
            //Inicializando variáveis, somente após o OpenCV ter inicializado com sucesso.
            InicializarVariaveis();
        }
    }

    void InicializarVariaveis() {
        //Criando objetos
        fd = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//        clahe = Imgproc.createCLAHE();

        //Como o objeto não muda em tempo de execução, suas características serão processadas aqui.
        img_object = new Mat();
        descriptors_object = new Mat();
        keypoints_object = new MatOfKeyPoint();

        //Lendo arquivo PNG
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.gabarito_kp3, null);

        if (drawable != null) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Utils.bitmapToMat(bitmap, img_object);
        } else {
            Log.e("Inicialização", "Imagem não encontrada");
        }

        //Pré-processando a imagem
        Imgproc.cvtColor(img_object, img_object, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.GaussianBlur(img_object, img_object, gaussianBlurSize, 0);
//        Imgproc.adaptiveThreshold(img_object, img_object, 255, 1, 1, 11, 2);
//        Utils.matToBitmap(img_object, bitmap);//Esta linha seria usada para debug

        //Calculando KeyPoints
        fd.detect(img_object, keypoints_object);

        //Computando descriptor
        extractor.compute(img_object, keypoints_object, descriptors_object);

        //Criando a thread de cálculos
        t = new Thread(new Worker());

        //Informando que a inicialização terminou
        state = State.Running;
        Log.d("Inicialização", "Finalizado. Estado = rodando");
    }

    void ExibirCamera() {
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // camera-related task you need to do.
                    ExibirCamera();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    AlertDialog ad = new AlertDialog.Builder(getContext()).create();
                    ad.setCancelable(false); // This blocks the 'BACK' button
                    ad.setMessage("Este aplicativo precisa de permissão para acessar a câmera para funcionar.");
                    ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((Activity) getContext()).finish();
                        }
                    });
                    ad.show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    /**
     * Chamado a cada vez que um frame é recebido da câmera.
     *
     * @param inputFrame Frame recebido da câmera
     * @return Imagem que será exibida na tela
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        switch (state) {
            case Running:
                switch (t.getState()) {
                    case TERMINATED:
                        //Caso o processamento tenha acabado, a Thread é recriada
                        setmRgba(inputFrame.rgba());
                        t = new Thread(new Worker());
                        break;
                    case NEW:
                        //Atribuindo o frame em mRgba para que a outra Thread leia.
                        setmRgba(inputFrame.rgba());
                        t.start();
                        break;
                }
                break;
            //Outros casos virão aqui
        }
        //O frame só é exibido para o usuário ao retornar esta função.
        return inputFrame.rgba(); // This function must return
    }

    class Worker implements Runnable {
        public void run() {
            Mat img_scene = getmRgba().clone();
            MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
            Mat descriptors_scene = new Mat();
            MatOfDMatch matches = new MatOfDMatch(), gm = new MatOfDMatch();
            LinkedList<DMatch> good_matches = new LinkedList<>();
            List<DMatch> matchesList;
            LinkedList<Point> objList = new LinkedList<>(), sceneList = new LinkedList<>();
            MatOfPoint2f obj = new MatOfPoint2f(), scene = new MatOfPoint2f();

            //Passo 1: Pré-processamento
            Imgproc.cvtColor(img_scene, img_scene, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.equalizeHist(img_scene, img_scene);
//            clahe.apply(img_scene, img_scene);
            Imgproc.GaussianBlur(img_scene, img_scene, gaussianBlurSize, 0);
//            Imgproc.adaptiveThreshold(img_scene, img_scene, 255, 1, 1, 11, 2);

            //Detectando KeyPoints
            fd.detect(img_scene, keypoints_scene);
            //Caso a imagem seja totalmente preta, keypoints_scene não terá nada dentro. Para evitar um
            //IndexOutOfBoundsException, a função acaba aqui.
            if (!keypoints_scene.size().equals(keypoints_object.size())) {
                return;
            }

            //– Step 2: Calculate descriptors (feature vectors)
            extractor.compute(img_scene, keypoints_scene, descriptors_scene);
            if ((descriptors_object.type() == descriptors_scene.type() &&
                    descriptors_object.cols() == descriptors_scene.cols())) {
                matcher.match(descriptors_scene, descriptors_object, matches);
            }

            matchesList = matches.toList();

            //– Quick calculation of max and min distances between keypoints
            for (int i = 0; i < descriptors_object.rows(); i++) {
                Double dist = (double) matchesList.get(i).distance;
                if (dist < min_dist) min_dist = dist;
                if (dist > max_dist) max_dist = dist;
            }

            for (int i = 0; i < descriptors_object.rows(); i++) {
                if (matchesList.get(i).distance < 3 * min_dist) {
                    good_matches.addLast(matchesList.get(i));
                }
            }

            gm.fromList(good_matches);

            ////Este bloco é apenas para debug. Comente para melhorar a performance.
            if (keypoints_scene.size().equals(keypoints_object.size())) {
                Mat rgb = new Mat();
                Mat matchesMat = new Mat();
                Imgproc.cvtColor(mRgba, rgb, Imgproc.COLOR_BGR2RGB);
                Features2d.drawMatches(img_scene, keypoints_scene, img_object, keypoints_object, gm, matchesMat);
                matchesBmp = Bitmap.createBitmap(matchesMat.cols(), matchesMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matchesMat, matchesBmp);
                Log.d("Teste", "equal");
            }

            List<KeyPoint> keypoints_objectList = keypoints_object.toList();
            List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

            for (int i = 0; i < good_matches.size(); i++) {
                objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
                sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
            }
            obj.fromList(objList);

            scene.fromList(sceneList);

            if (objList.size() >= 4 && sceneList.size() >= 4) {
                //Homografia para desenhar os contornos do objeto na cena. Apenas para debug.
                Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 0.5);

                Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
                Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

                obj_corners.put(0, 0, 0, 0);
                obj_corners.put(1, 0, img_object.cols(), 0);
                obj_corners.put(2, 0, img_object.cols(), img_object.rows());
                obj_corners.put(3, 0, 0, img_object.rows());

                Core.perspectiveTransform(obj_corners, scene_corners, hg);

                Imgproc.cvtColor(img_scene, img_scene, Imgproc.COLOR_GRAY2BGR);
                Imgproc.line(img_scene, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
                Imgproc.line(img_scene, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
                Imgproc.line(img_scene, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
                Imgproc.line(img_scene, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);

                /*
                Mat warpimg = img_object.clone();
                org.opencv.core.Size ims = new org.opencv.core.Size(img_object.cols(), img_object.rows());
                Imgproc.warpPerspective(img_object, warpimg, H, ims);
                */

                ////Este bloco é apenas para debug. Comente para melhorar a performance.
                Bitmap scene_bmp = Bitmap.createBitmap(img_scene.cols(), img_scene.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_scene, scene_bmp);
                Log.d("WarpingPerspective", "Objeto encontrado");
                //TODO: Usar homografia e Warp para ajustar a posição do objeto (prova).
                //TODO: Mudar estado para ObjectFound e ler as respostas na prova.
            }
        }
    }

    synchronized Mat getmRgba() {
        //Setter e getter de mRgba são sincronizados para evitar conflito entre as threads.
        return mRgba;
    }

    synchronized void setmRgba(Mat mRgba) {
        this.mRgba = mRgba;
    }

    public Context getContext() {
        return this;
    }
}