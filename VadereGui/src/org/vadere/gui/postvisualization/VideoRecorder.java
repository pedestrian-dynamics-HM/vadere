package org.vadere.gui.postvisualization;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FilenameUtils;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.postvisualization.control.AutoPlayer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.utils.MovRecorder;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;
import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.util.io.IOUtils;
import org.vadere.util.io.VadereArgumentParser;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;


enum VideoQuality {
    LOW,
    MEDIUM,
    HIGH;

    static String getValues(){
        return Arrays.stream(VideoQuality.values()).collect(Collectors.toList()).toString();
    }
}


public class VideoRecorder extends PostVisualisation {

    private static final Logger logger = Logger.getLogger(VideoRecorder.class);
    private static double numberOfPixelsPerTopoMeter = 50.0;

    private static int maxNumberOfPixels = 1280*720; // HD

    private final String videoPath;
    private final String outputDir;
    private final Object isReady = new Object();
    private final Object isFinished = new Object();
    private final PostvisualizationModel model;
    private final double resolutionScaleFactor;
    private PostvisualizationWindow postvisualizationWindow;
    private boolean isReadyForRecording = false;
    private JPanel jPanel;
    private boolean isFinishedWithRecording;
    private MovRecorder recorder;
    private int framesPerSecond = 24;

    private PostvisualizationRenderer renderer;

    static {
        System.setProperty("java.awt.headless", "true");
        System.out.println("AWT runs headless: " + java.awt.GraphicsEnvironment.isHeadless());
    }


    public VideoRecorder(String videoPath,
                         String outputDir,
                         double imageResolution) {

        this.videoPath = videoPath;
        this.outputDir = outputDir;
        this.resolutionScaleFactor = imageResolution;

        this.model = new PostvisualizationModel();
        this.renderer = new PostvisualizationRenderer(this.model);
        this.postvisualizationWindow = new PostvisualizationWindow(this.model, this.renderer);

    }

    public static void main(String[] args) {

        // read and parse user input (output directory and video path from command line)
        Logger.setMainArguments(args);
        VadereArgumentParser vadereArgumentParser = new VadereArgumentParser();
        ArgumentParser argumentParser = vadereArgumentParser.getArgumentParser();
        addSubCommandsToParser(argumentParser);
        Namespace ns = null;
        try {
            ns = vadereArgumentParser.parseArgsAndProcessInitialOptions(args);
            Localization.loadLanguageFromPreferences(VadereApplication.class);
        } catch (ArgumentParserException e) {
            throw new RuntimeException(e);
        }

        String postvisDirPath = ns.get("postvis-dir");
        checkVadereOutputDir(postvisDirPath);
        String videoPath = ns.get("video-path");
        videoPath = correctVideoPath(videoPath, postvisDirPath);

        VideoQuality videoQuality = ns.get("video-quality");
        double imageResolution = getImageResoultion(videoQuality);

        // record and save video
        VideoRecorder vd = new VideoRecorder(videoPath, postvisDirPath, imageResolution);
        vd.startPostvis();
        vd.record();
        vd.finishPostvis();

    }

    private static double getImageResoultion(VideoQuality videoQuality) {

        logger.info("Chosen image quality " + VideoQuality.getValues() +": " + videoQuality);
        double numberOfPixelsPerTopoSquareMeter = Math.pow(numberOfPixelsPerTopoMeter, 2);

        switch (videoQuality) {
            case LOW:
                numberOfPixelsPerTopoMeter = Math.sqrt(numberOfPixelsPerTopoSquareMeter / 2);
                break;
            case MEDIUM:
                break;
            case HIGH:
                numberOfPixelsPerTopoMeter = Math.sqrt(numberOfPixelsPerTopoSquareMeter * 2);
                break;
        }

        return numberOfPixelsPerTopoMeter;
    }

    private static String correctVideoPath(String videoPath, String dirPath) {

        File videoPathFile = new File(videoPath);

        if (videoPathFile.getParent() == null) {
            videoPath = (new File(dirPath, videoPath)).toString();
        }

        String fileExtension = FilenameUtils.getExtension(videoPath);
        if (!fileExtension.equals("mov")) {
            videoPath = FilenameUtils.removeExtension(videoPath) + ".mov";
            logger.warn("Got video file format (= *." + fileExtension + ") not allowed. Convert *." + fileExtension + " to *.mov.");
        }
        return videoPath;
    }

    private static void checkVadereOutputDir(String postvisDirPath) {

        // check if specified correctly
        File threadFile = new File(postvisDirPath);
        File scenarioOutputDir = threadFile.isDirectory() ? threadFile : threadFile.getParentFile();
        Optional<File> trajectoryFile =
                IOUtils.getFirstFile(scenarioOutputDir, IOUtils.TRAJECTORY_FILE_EXTENSION);
        Optional<File> scenarioFile =
                IOUtils.getFirstFile(scenarioOutputDir, IOUtils.SCENARIO_FILE_EXTENSION);

        if (!threadFile.isDirectory()) {
            try {
                throw new FileNotFoundException("Directory does not exist: " + threadFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        if (!trajectoryFile.isPresent()) {
            try {
                throw new FileNotFoundException("No postvis.traj file found in specified output dir (=" + postvisDirPath + ").");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        if (!scenarioFile.isPresent()) {
            try {
                throw new FileNotFoundException("No scenario-file (*.scenario) found in specified output dir (=" + postvisDirPath + ").");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        logger.info("Vadere output successfully located in dir " + postvisDirPath + ".");

    }

    private static void addSubCommandsToParser(ArgumentParser parser) {

        parser.addArgument("--vadere-output-dir", "-i")
                .required(true)
                .setDefault("postvis-dir")
                .dest("postvis-dir") // set name in namespace
                .type(String.class)
                .help("Directory where the vadere output (trajectory file, scenario file, dataprocessor files) is stored.");
        parser.addArgument("--output", "-o")
                .required(false)
                .type(String.class)
                .dest("video-path")
                .setDefault("movie.mov")
                .help("Path of the video export. If not specified, the vadere output dir is used.");
        parser.addArgument("--quality", "-q")
                .required(false)
                .type(VideoQuality.class)
                .choices(VideoQuality.values())
                .dest("video-quality")
                .setDefault(VideoQuality.MEDIUM)
                .help("Video quality (default: medium). Choose 'low' to decrease and 'high' to increase the resolution.");

    }

    void record() {

        try {
            EventQueue.invokeAndWait(() -> {

                synchronized (isReady) {
                    if (isReadyForRecording) {
                        this.model.config.setRecording(true);

                        recorder = new MovRecorder(renderer);
                        model.addObserver(this.recorder);
                        overwriteVideoRecorderSettings();

                        AutoPlayer player = AutoPlayer.getInstance(model, recorder, new File(videoPath));
                        player.start();
                        player.run();
                        player.stop();

                        synchronized (isFinished) {
                            isFinishedWithRecording = true;
                            isFinished.notify();
                        }
                    }

                }


            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    private void overwriteVideoRecorderSettings() {

        double topographyWidth = model.getTopographyBound().getWidth();
        double topographyHeight = model.getTopographyBound().getHeight();

        double numberOfPixelsHorizontalDir = Math.floor(topographyWidth * resolutionScaleFactor);
        double numberOfPixelsVerticalDir = Math.floor(topographyHeight * resolutionScaleFactor);

        if(numberOfPixelsHorizontalDir * numberOfPixelsVerticalDir > maxNumberOfPixels){
            logger.info("Topography (=" + String.format("%.1f", topographyWidth) + " x " +
                            String.format("%.1f", topographyHeight) +
                    ") too large to apply image quality settings. Ignore image quality settings and use " +
                    maxNumberOfPixels + " pixels (HD quality).");
        }

        numberOfPixelsHorizontalDir =Math.floor(Math.min(numberOfPixelsHorizontalDir, maxNumberOfPixels/ topographyHeight));
        numberOfPixelsVerticalDir = Math.floor(Math.min(numberOfPixelsVerticalDir, maxNumberOfPixels/ topographyWidth));


        Rectangle2D.Double imageSize = new Rectangle2D.Double(0, 0, numberOfPixelsHorizontalDir, numberOfPixelsVerticalDir);
        recorder.setImageSize(imageSize);

        model.config.setFps(framesPerSecond);
        logger.info("Use " + framesPerSecond + " fps (ignore fps found in vadere config: " + model.config.getFps() + ").");
        recorder.setViewport(imageSize);

    }

    void finishPostvis() {



        try {
            EventQueue.invokeAndWait(() -> {
                synchronized (isFinished) {
                    if (isFinishedWithRecording) {
                        jPanel.setVisible(false);
                        //jFrame.dispose();
                        logger.info("Video successfully exported to " + this.videoPath);
                        System.exit(0);
                    }
                }


            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    void startPostvis() {

        try {
            EventQueue.invokeAndWait(() -> {

                try {
                    jPanel = new JPanel();
                } catch (HeadlessException e) {
                    e.printStackTrace(System.err);
                }

                jPanel.add(this.postvisualizationWindow);
                jPanel.setEnabled(false);

                this.postvisualizationWindow.loadOutputDir(new File(this.outputDir));

                synchronized (isReady) {
                    this.isReadyForRecording = true;
                    isReady.notify();
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }


    }

}
