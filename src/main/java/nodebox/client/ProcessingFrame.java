package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import processing.core.PApplet;
import processing.core.PVector;
import SimpleOpenNI.*;


public class ProcessingFrame extends JFrame {
    private Applet applet;

    public ProcessingFrame() {
        super("Processing Frame");
        setLayout(new BorderLayout());
        applet = new Applet();
        add(applet, BorderLayout.CENTER);
        applet.init();
        setSize(500, 500);
    }

    public Map<Integer, Map<String, List<Float>>> getSkeletonData() {
        return applet.skeletonData;
    }

    public class Applet extends PApplet {
        private SimpleOpenNI context;
        private boolean autoCalib=true;

        private Map<Integer, Map<String, List<Float>>> skeletonData = ImmutableMap.of();

        public void setup() {
            context = new SimpleOpenNI(this);

            // enable depthMap generation
            if(context.enableDepth() == false)
            {
                println("Can't open the depthMap, maybe the camera is not connected!");
                exit();
                return;
            }

            // enable skeleton generation for all joints
            context.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);

            background(200,0,0);

            stroke(0,0,255);
            strokeWeight(3);
            smooth();

            size(context.depthWidth(), context.depthHeight());
            ProcessingFrame.this.setSize(width, height);
        }

        public void draw() {
            // update the cam
            context.update();

            // draw depthImageMap
            image(context.depthImage(),0,0);

            // draw the skeleton if it's available
            int[] userList = context.getUsers();

            ImmutableMap.Builder<Integer, Map<String, List<Float>>> builder = new ImmutableMap.Builder<Integer, Map<String, List<Float>>>();

            for(int i=0;i<userList.length;i++)
            {
                if(context.isTrackingSkeleton(userList[i])) {
                    drawSkeleton(userList[i]);
                    builder.put(userList[i], retrieveSkeletonData(userList[i]));
                }
            }
            skeletonData = builder.build();
        }

        public Map<String, List<Float>> retrieveSkeletonData(int userId) {
            Map<String, List<Float>> map = new HashMap<String, List<Float>>();

            getJointPosition(userId, SimpleOpenNI.SKEL_HEAD, "head", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_NECK, "neck", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, "shoulder_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, "shoulder_right", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, "elbow_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, "elbow_right", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_TORSO, "torso", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_HIP, "hip_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_HIP, "hip_right", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_KNEE, "knee_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, "knee_right", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_HAND, "hand_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_HAND, "hand_right", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_LEFT_FOOT, "foot_left", map);
            getJointPosition(userId, SimpleOpenNI.SKEL_RIGHT_FOOT, "foot_right", map);

            return ImmutableMap.copyOf(map);
        }

        // draw the skeleton with the selected joints
        public void drawSkeleton(int userId)
        {
            // to get the 3d joint data

            context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

            context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
            context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
            context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

            context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
            context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
            context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

            context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
            context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

            context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
            context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
            context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

            context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
            context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
            context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);
        }

        private void getJointPosition(int userId, int skelData, String jointName, Map<String, List<Float>> data) {
            PVector jointPos = new PVector();
            context.getJointPositionSkeleton(userId, skelData, jointPos);
            ImmutableList<Float> floats = ImmutableList.of(jointPos.x, jointPos.y, jointPos.z);
            data.put(jointName, floats);
        }

        // -----------------------------------------------------------------
        // SimpleOpenNI events

        public void onNewUser(int userId)
        {
            println("onNewUser - userId: " + userId);
            println("  start pose detection");

            if(autoCalib)
                context.requestCalibrationSkeleton(userId,true);
            else
                context.startPoseDetection("Psi",userId);
        }

        public void onLostUser(int userId)
        {
            println("onLostUser - userId: " + userId);
        }

        public void onExitUser(int userId)
        {
            println("onExitUser - userId: " + userId);
        }

        public void onReEnterUser(int userId)
        {
            println("onReEnterUser - userId: " + userId);
        }

        public void onStartCalibration(int userId)
        {
            println("onStartCalibration - userId: " + userId);
        }

        public void onEndCalibration(int userId, boolean successfull)
        {
            println("onEndCalibration - userId: " + userId + ", successfull: " + successfull);

            if (successfull)
            {
                println("  User calibrated !!!");
                context.startTrackingSkeleton(userId);
            }
            else
            {
                println("  Failed to calibrate user !!!");
                println("  Start pose detection");
                context.startPoseDetection("Psi",userId);
            }
        }

        public void onStartPose(String pose,int userId)
        {
            println("onStartPose - userId: " + userId + ", pose: " + pose);
            println(" stop pose detection");

            context.stopPoseDetection(userId);
            context.requestCalibrationSkeleton(userId, true);
        }

        public void onEndPose(String pose,int userId)
        {
            println("onEndPose - userId: " + userId + ", pose: " + pose);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new ProcessingFrame();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

