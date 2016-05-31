package com.example.dener.opencvdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

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

    /**
     * Armazena o frame atual para que a thread de processamento o leia.
     * Não atribua valores diretamente a esta variável! Utilize Setter e Getter para evitar conflitos.
     */
    private Mat mRgba;

    //Variável para medir o tempo de inicialização
    private Stopwatch initStopwatch = new Stopwatch();

    private ObjectParams objectParams;
    private Worker worker = new Worker();
    private Leitor leitor = new Leitor();
    private Homography homography = new Homography();

    //Thread usada para processamento em segundo plano
    private Thread t = new Thread(worker);

    //Estado atual
    private State state = State.Initializing;
    private State lastState = State.Initializing;

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
            new Thread() {
                @Override
                public void run() {
                    objectParams = new ObjectParams((Activity) getContext());
                }
            }.start();
        }
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
            case Initialized:
                setState(State.Running);
                Log.v("onCameraFrame", "Tempo decorrido de onCreate até receber o primeiro frame: " +
                        initStopwatch.getElapsedTime() + "ms");
                break;
            //Outros casos virão aqui
        }

        //O frame só é exibido para o usuário ao retornar esta função.
        return frame; // This function must return
    }

    class Worker implements Runnable {

        Stopwatch workerStopwatch = new Stopwatch();
        final int limite = 0;

        public void run() {
            while (state == State.Running) {
                Log.v("Homografia", "Imagem processada em " + workerStopwatch.getElapsedTime() + "ms");
                limitarFrequencia();
                workerStopwatch.start();

                Mat imgOut = homography.calculate(getmRgba(), objectParams);

                if (imgOut == null)
                    continue;

                //TODO: Mudar estado para ObjectFound e ler as respostas na prova.
                leitor.LerProva(imgOut);
            }
        }

        void limitarFrequencia() {
            //Este bloco Try limita as iterações por segundo nesta Thread, para liberar tempo
            // de CPU. Atualmente desativado.
            try {
                Thread.sleep(Math.max(limite - (int) workerStopwatch.getElapsedTime(), 0));
            } catch (InterruptedException e) {
                e.printStackTrace();
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

    void setState(State state) {
        this.lastState = this.state;
        this.state = state;
    }
}