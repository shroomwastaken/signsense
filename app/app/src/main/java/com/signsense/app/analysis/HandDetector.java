package com.signsense.app.analysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.createFromOptions;

public class HandDetector {
    private static final String TAG = "HandDetector";

    private final int[] tipIds = new int[]{4, 8, 12, 16, 20}; // IDs for fingertips

    private HandLandmarker handLandmarker;
    private Context appContext;
    private boolean draw;

    public HandDetector(Context context, RunningMode mode) {
        this.appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        // Loading from settings
        this.draw = preferences.getBoolean("draw", false);
        int maxHands = preferences.getInt("maxHands", 2);
        float detectionCon = (float) preferences.getInt("detectionCon", 50) / 100;
        float trackingCon = (float) preferences.getInt("trackingCon", 50) / 100;
        float presenceCon = (float) preferences.getInt("presenceCon", 50) / 100;

        // Loading hand detection model
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.GPU) // ALL I HAD TO DO IS TO SET IT TO FUCKING GPU MODE AND NOW IT WORKS
                .build();

        // Logs information about the hand detector config
        Log.i(TAG, "Successfully loaded Hand Detector Model");
        Log.i(TAG, "Hand Detector Configuration:");
        Log.i(TAG, "Draw: " + draw);
        Log.i(TAG, "Max Hands: " + maxHands);
        Log.i(TAG, "Detection Confidence: " + detectionCon);
        Log.i(TAG, "Tracking Confidence: " + trackingCon);
        Log.i(TAG, "Presence Confidence: " + presenceCon);

        // Setting up the Hand Landmarker
        handLandmarker = createFromOptions(appContext, HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(mode)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(detectionCon)
                .setMinTrackingConfidence(trackingCon)
                .setMinHandPresenceConfidence(presenceCon)
                .build()
        );
    }

    // Function for detecting hand when using live camera (frame by frame)
    public List<Float> detectFrame(Bitmap bitmap) {
        List<Float> landmarks = new ArrayList<>();

        // Convert bitmap (frame) to MPImage
        MPImage image = new BitmapImageBuilder(bitmap).build();

        // Detecting hand
        HandLandmarkerResult result = handLandmarker.detect(image);

        // Adding tip x and y coordinates to list of landmarks
        if (result.landmarks().size() > 0) {
            for (List<NormalizedLandmark> landmark : result.landmarks()) {
                for (int tipId : tipIds) { // Getting x and y for every tip
                    float x = landmark.get(tipId).x();
                    float y = landmark.get(tipId).y();
                    landmarks.add(x);
                    landmarks.add(y);
                }
            }
            Log.i(TAG, landmarks.toString());
        }

        return landmarks;
    }

    // Function for detecting hand when the video is uploaded (breaks it down into multiple frames)
    public List<List<Float>> detectVideo(Uri videoUri, long interval) throws IOException {
        List<List<Float>> landmarksList = new ArrayList<>();
        List<Float> landmarks = new ArrayList<>();

        // Setup retriever to get video data
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(appContext, videoUri);

        // Set video length and start time
        long videoLength = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        // Get the total frames we need to analyse based on interval (in ms) between them
        List<List<Float>> results = new ArrayList<>();
        int totalFrames = (int) (videoLength / interval);

        // Loop through each frame and add result to results list
        for (int i = 0; i < totalFrames; i++) {
            long timeStamp = i * interval;
            Bitmap frame = retriever.getFrameAtTime(timeStamp, MediaMetadataRetriever.OPTION_CLOSEST);

            //Convert frame to ARGB_8888 (required by damn mediapipe)
            Bitmap aFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

            // Convert ARGB_8888 frame to MPImage
            MPImage image = new BitmapImageBuilder(aFrame).build();

            HandLandmarkerResult result = handLandmarker.detectForVideo(image, timeStamp);

            // Adding tip coordinates to list of landmark
            if (result.landmarks().size() > 0) {
                for (List<NormalizedLandmark> landmark : result.landmarks()) {
                    for (int tipId : tipIds) { // Getting X and Y for every tip
                        float x = landmark.get(tipId).x();
                        float y = landmark.get(tipId).y();
                        landmarks.add(x);
                        landmarks.add(y);
                    }
                }

                Log.i(TAG, landmarks.toString());
            }

            // Add to the list of results
            landmarksList.add(landmarks);
        }

        retriever.release();

        Log.i(TAG, landmarksList.toString());

        return landmarksList;
    }

    public Mat drawHand(Mat frame, List<Float> landmarks) {
        if (!draw) {
            return frame;
        }

        for (int i = 0; i < landmarks.size() - 1; i += 2) {
            float x = landmarks.get(i);
            float y = landmarks.get(i + 1);

            // Drawing circles at the fingertips by finding coordinates via multiplication
            // E.g. we have 200 pixels wide screen and the fingertip X is 0.54, so we draw x at 200 * 0.54 = 108
            Imgproc.circle(
                    frame,
                    new Point(frame.width() * x, frame.height() * y),
                    5,
                    new Scalar(255, 0, 0, 255),
                    10
            );
        }

        return frame;
    }
}
