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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// OpenCV Classes

public class MainActivity_show_camera extends AppCompatActivity implements CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

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
    private State lastState = State.Initializing;

    /**
     * Armazena o frame atual para que a thread de processamento o leia.
     * Não atribua valores diretamente a esta variável! Utilize Setter e Getter para evitar conflitos.
     */
    private Mat mRgba;
    /**
     * Armazena as linhas a serem desenhadas sobre o frame atual.
     * Não atribua valores diretamente a esta variável! Utilize Setter e Getter para evitar conflitos.
     */
    private Point[] HPoints = new Point[4];

    //Variável para medir o tempo de inicialização
    Stopwatch initStopwatch = new Stopwatch();

    private Mat descriptors_object;
    private Mat img_object;
    private MatOfKeyPoint keypoints_object;
    private List<KeyPoint> keypoints_object_list;
    private Size gaussianBlurSize = new Size(3, 3);
    private FeatureDetector fd;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;
//    private CLAHE clahe;
    Worker worker = new Worker();
    Leitor leitor = new Leitor();

    /**
     * Mude para true para funções de teste.
     */
    private boolean debug = false;
    /**
     * Mude para true para mostrar o contorno na tela.
     */
    private boolean cont = false;

    public MainActivity_show_camera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initStopwatch.start();
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.show_camera);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        final Switch contornosSW = (Switch) findViewById(R.id.switch1);
        assert contornosSW != null;
        contornosSW.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                cont = isChecked;
            }
        });

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
        setState(lastState);
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
//        clahe.setTilesGridSize(new Size(100, 100));

        //Como o objeto não muda em tempo de execução, suas características serão processadas aqui.
        img_object = new Mat();
        descriptors_object = new Mat();
        keypoints_object = new MatOfKeyPoint();

        //Lendo arquivo PNG
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.gabarito_kp10, null);

        if (drawable != null) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Utils.bitmapToMat(bitmap, img_object);
            if (debug) {
                Utils.matToBitmap(img_object, bitmap);//Esta linha seria usada para debug
            }
        } else {
            Log.e("Inicialização", "Imagem não encontrada");
        }

        //Pré-processando a imagem
        Imgproc.cvtColor(img_object, img_object, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.GaussianBlur(img_object, img_object, gaussianBlurSize, 0);
//        Imgproc.adaptiveThreshold(img_object, img_object, 255, 1, 1, 11, 2);

        //Calculando KeyPoints
        fd.detect(img_object, keypoints_object);
        keypoints_object_list = keypoints_object.toList();

        //Computando descriptor
        extractor.compute(img_object, keypoints_object, descriptors_object);

        //Criando a thread de cálculos
        t = new Thread(worker);

        //Informando que a inicialização terminou
        Log.d("Inicialização", "Finalizado. Estado = rodando");
    }

    void ExibirCamera() {
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
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
//                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onPause() {
        setState(State.Paused);
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
        Mat frame = inputFrame.rgba();
        switch (state) {
            case Running:
                setmRgba(frame);
                switch (t.getState()) {
                    case TERMINATED:
                        //Caso o processamento tenha acabado, a Thread é recriada
                        t = new Thread(worker);
                        break;
                    case NEW:
                        //Atribuindo o frame em mRgba para que a outra Thread leia.
                        t.start();
                        break;
                }
                break;
            case Initializing:
                setState(State.Running);
                Log.v("onCameraFrame", "Tempo decorrido de onCreate até receber o primeiro frame: " +
                        initStopwatch.getElapsedTime() + "ms");
                break;
            //Outros casos virão aqui
        }

        //<editor-fold desc="Apenas para debug.">
        if (cont) {
            Mat HFrame = frame.clone();
            Point[] HPoints = getHPoints();
            for (int i = 0; i < 4; i++) {
                if (HPoints[i] == null) {
                    return frame;
                }
            }
            Imgproc.line(HFrame, HPoints[0], HPoints[1], new Scalar(0, 255, 0), 4);
            Imgproc.line(HFrame, HPoints[1], HPoints[2], new Scalar(0, 255, 0), 4);
            Imgproc.line(HFrame, HPoints[2], HPoints[3], new Scalar(0, 255, 0), 4);
            Imgproc.line(HFrame, HPoints[3], HPoints[0], new Scalar(0, 255, 0), 4);
            return HFrame;
        }
        //</editor-fold>

        //O frame só é exibido para o usuário ao retornar esta função.
        return frame; // This function must return
    }

    class Worker implements Runnable {
        Stopwatch workerStopwatch = new Stopwatch();
        final Size homSize = new Size(3, 3);

        public void run() {
            outerLoop:
            while (state == State.Running) {
                //Este bloco Try limita as iterações por segundo nesta Thread, para liberar tempo
                // de CPU. Atualmente desativado.
//                try {
//                    Thread.sleep(Math.max(1000 - (int) workerStopwatch.getElapsedTime(), 0));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                Mat img_scene = getmRgba().clone();
                MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
                Mat descriptors_scene = new Mat();
                Log.v("Homografia", "Imagem processada em " + workerStopwatch.getElapsedTime() + "ms");
                workerStopwatch.start();

                //Passo 1: Pré-processamento
                Imgproc.cvtColor(img_scene, img_scene, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.equalizeHist(img_scene, img_scene);
//            clahe.apply(img_scene, img_scene);
                Imgproc.GaussianBlur(img_scene, img_scene, gaussianBlurSize, 0);
//                Imgproc.adaptiveThreshold(img_scene, img_scene, 255, 1, 1, 11, 2);

                Log.v("Homografia", "Pré-processamento: " + workerStopwatch.split() + "ms");

                //Detectando KeyPoints
                fd.detect(img_scene, keypoints_scene);

                Log.v("Homografia", "Detectando KeyPoints: " + workerStopwatch.split() + "ms");

                //Caso a imagem seja totalmente preta, keypoints_scene não terá nada dentro.
                // Para evitar um IndexOutOfBoundsException, a função acaba aqui.
                if (keypoints_scene.size().equals(new Size(1, 0)) || keypoints_object.size().equals(new Size(1, 0))) {
                    continue;
                }

                //– Step 2: Calculate descriptors (feature vectors)
                extractor.compute(img_scene, keypoints_scene, descriptors_scene);
                List<MatOfDMatch> matches = new LinkedList<>();
                matcher.knnMatch(descriptors_scene, descriptors_object, matches, 5);

                Log.v("Homografia", "Calculando descriptors: " + workerStopwatch.split() + "ms");

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
                    pts2.add(keypoints_object_list.get(good_matches.get(i).trainIdx).pt);
                }

                // convertion of data types - there is maybe a more beautiful way
                Mat outputMask = new Mat();
                MatOfPoint2f pts1Mat = new MatOfPoint2f();
                pts1Mat.fromList(pts1);
                MatOfPoint2f pts2Mat = new MatOfPoint2f();
                pts2Mat.fromList(pts2);

                Log.v("Homografia", "good_matches: " + workerStopwatch.split() + "ms");

                //Pelo menos 4 pontos em cada Mat são necessários para a homografia.
                if (pts1Mat.total() < 4 || pts2Mat.total() < 4)
                    continue;

                // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
                // the smaller the allowed reprojection error (here 15), the more matches are filtered
                Mat revHomog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);
                Log.v("Homografia", "Homografia: " + workerStopwatch.split() + "ms");
                //O tamanho da matriz de transformação revHomog deve ser 3*3.
                // Caso não seja, é sinal de que a homografia falhou.
                if (!revHomog.size().equals(homSize) || !niceHomography(revHomog)) {
                    continue;
                }
                Mat imgOut = new Mat(img_object.size(), CvType.CV_8UC4);
                Imgproc.warpPerspective(img_scene, imgOut, revHomog, imgOut.size());

                // binariza imagem usando um threshold adaptativo
                Imgproc.adaptiveThreshold(imgOut, imgOut, 255,
                        Imgproc.ADAPTIVE_THRESH_MEAN_C,
                        Imgproc.THRESH_BINARY, 61, 15);

                //É imgOut a imagem que devemos usar na etapa de ler as respostas.

                Log.v("Homografia", "Warp Perspective: " + workerStopwatch.split() + "ms");

                //<editor-fold desc="Apenas para debug.">
                if (cont) {
                    // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
                    // the smaller the allowed reprojection error (here 15), the more matches are filtered
                    Mat homog = Calib3d.findHomography(pts2Mat, pts1Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);

                    Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
                    Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

                    obj_corners.put(0, 0, 0, 0);
                    obj_corners.put(1, 0, img_object.cols(), 0);
                    obj_corners.put(2, 0, img_object.cols(), img_object.rows());
                    obj_corners.put(3, 0, 0, img_object.rows());

                    Core.perspectiveTransform(obj_corners, scene_corners, homog);

                    Point[] HPoints = new Point[4];
                    for (int i = 0; i < 4; i++) {
                        HPoints[i] = new Point(scene_corners.get(i, 0));
                    }

                    setHPoints(HPoints);
                }
                if (debug) {
                    Bitmap scene_bmp = Bitmap.createBitmap(img_scene.cols(), img_scene.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img_scene, scene_bmp);

                    // outputMask contains zeros and ones indicating which matches are filtered
                    LinkedList<DMatch> better_matches = new LinkedList<>();
                    for (int i = 0; i < good_matches.size(); i++) {
                        if (outputMask.get(i, 0) == null) {
                            continue outerLoop;
                        }
                        if (outputMask.get(i, 0)[0] != 0.0) {
                            better_matches.add(good_matches.get(i));
                        }
                    }

                    // DRAWING OUTPUT
                    Mat outputImg = new Mat();
                    // this will draw all matches, works fine
                    MatOfDMatch better_matches_mat = new MatOfDMatch();
                    better_matches_mat.fromList(better_matches);
                    Features2d.drawMatches(img_scene, keypoints_scene, img_object, keypoints_object, better_matches_mat, outputImg);
                    Bitmap matches_bmp = Bitmap.createBitmap(outputImg.cols(), outputImg.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(outputImg, matches_bmp);

                    Bitmap warp_bmp = Bitmap.createBitmap(imgOut.cols(), imgOut.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(imgOut, warp_bmp);
                    Log.v("Homografia", "Debug: " + workerStopwatch.split() + "ms");
                }
                //</editor-fold>
                //TODO: Mudar estado para ObjectFound e ler as respostas na prova.
                leitor.LerProva(imgOut);
            }
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
            return N3 <= 0.001; //Padrão if (N3 <= 0.002)
        }
    }

    synchronized Mat getmRgba() {
        //Setter e getter de mRgba são sincronizados para evitar conflito entre as threads.
        return mRgba;
    }

    synchronized void setmRgba(Mat mRgba) {
        this.mRgba = mRgba;
    }

    synchronized Point[] getHPoints() {
        return HPoints;
    }

    synchronized void setHPoints(Point[] HPoints) {
        this.HPoints = HPoints;
    }

    public Context getContext() {
        return this;
    }

    void setState(State state) {
        this.lastState = this.state;
        this.state = state;
    }
}