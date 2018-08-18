import javax.sound.sampled.*;
import javax.xml.transform.Source;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.io.File;

public class AudioCapture {
    private int bufferLimitFactor = 5;  // fraction of line buffer size that will be read in

    private AudioFormat format = null;
    private String macOSMicrophoneName = "Built-in Microphone";
    private String macOSSpeakerName = "Port Built-in Output";   // speaker output is mainly for testing purposes
    private Mixer micMixer = null;
    private Mixer speakerMixer = null;

    private TargetDataLine tdl = null;

    private static Boolean audioCaptureStopped = false;

    private static short FOUNDSPEAKERMIXER = 0;
    private static short FOUNDMICMIXER = 0;


    public AudioCapture(int bufferLimitFactor, AudioFormat format, String microphoneName, String speakerName) {
        this.macOSMicrophoneName = microphoneName;
        this.bufferLimitFactor = bufferLimitFactor;
        this.format = format;
        if (!speakerName.equals("")) {
            macOSSpeakerName = speakerName;
        }
    }

    public boolean setUpAudioSystem() {
        // TESTING ONLY
        this.listMixers();

        if(this.getMixer()) {
    //        this.listSpeakerLines();
            return true;
        }
        else return false;

    }

    // To play or capture sound using the Java Sound API, you need at least three things:
    private void listMixers() {
        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println("MIXER: "+info);
            for(Line.Info lineInfo : AudioSystem.getMixer(info).getTargetLineInfo()) {
                System.out.println("\tDATA LINE: "+lineInfo.toString());
            }
        }
    }

    // Obtain mixer associated with built in microphone and speaker
    private boolean getMixer() {
        boolean success_flag = false;
        Mixer.Info micInfo = null;
        Mixer.Info speakerInfo = null;
        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
//            System.out.println(info);
            if(FOUNDMICMIXER == 0) {
                if (info.getName().equals(macOSMicrophoneName)) {
                    System.out.println("found microphone!");
                    micInfo = info;
                    FOUNDMICMIXER = 1;
                }
            }
            if(FOUNDSPEAKERMIXER == 0) {
                if (info.getName().equals(macOSSpeakerName)) {
                    System.out.println("found speaker!");
                    speakerInfo = info;
                    FOUNDSPEAKERMIXER = 1;
                }
            }
        }
        if(micInfo == null) {
            System.out.println("Could not find microphone info");
        }
        else {
            this.micMixer = AudioSystem.getMixer(micInfo);
            success_flag = true;
        }
        if(speakerInfo == null) {
            System.out.println("Could not find speaker info");
        }
        else {
            this.speakerMixer = AudioSystem.getMixer(speakerInfo);
        }
        return success_flag;
    }


    private void listSpeakerLines() {
        System.out.println("in listSpeakerLines");
        System.out.println(this.speakerMixer.getMixerInfo());
        for (Line line : this.speakerMixer.getSourceLines()) {
            System.out.println("Source lines assoc with speaker mixer");
            System.out.println(line.getLineInfo());
        }
        for (Line line : this.speakerMixer.getTargetLines()) {
            System.out.println("Target lines assoc with speaker mixer");
            System.out.println(line.getLineInfo());
        }
    }

    // Set up target data line to read audio from Mixer
    public void handleTargetDataLine() {
        TargetDataLine targetLine;
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, this.format);
        if(!this.micMixer.isLineSupported(targetInfo)){
            //TODO: throw exception
            System.out.println("TargetDataLine not supported by mic mixer");
        }
        else {
            // get and open the line
            try {
                this.tdl = (TargetDataLine) this.micMixer.getLine(targetInfo);
                System.out.println("target line buffer size: " + this.tdl.getBufferSize());

                // reserve line for this app's use by opening; optional param of buffer size
                this.tdl.open(format);

                // line is ready to start capturing audio data (make sure app is ready to begin reading from data line)
                // else you waste processing on filling capture buffer, to have it overflow
                // .read attempts to read in 'len' bytes of data into the byte array, starting from a byte offset
                // you can request more data than the byte array can actually hold
                //            int numBytesRead = line.read(new byte[] {}, 0, 5);

                System.out.println("target data line is open and ready!");
//            TODO: uncomment this next method when ready
                this.readAudioData();

//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException ie) {
//                System.out.println("this is rude");
//            }
                System.out.println("line will now close");
                this.tdl.close();

            } catch (LineUnavailableException luaEx) {
                //TODO: throw exception
                luaEx.printStackTrace();
                System.out.println("target line is unavailable");
            }
        }
    }


    // TODO: test playback
    private SourceDataLine setUpSourceLine() {
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, this.format);
        for (AudioFormat sourceformat:sourceInfo.getFormats()){
            System.out.println(sourceformat);
        }
        if(!this.speakerMixer.isLineSupported(sourceInfo)){
            //TODO: throw exception
            System.out.println("Source info not supported by speaker mixer");
            return null;
        }
        else {
            try {
                SourceDataLine sourceLine = (SourceDataLine) this.speakerMixer.getLine(sourceInfo);
                sourceLine.open(this.format);
                System.out.println("Source line setup success");
                return sourceLine;
            } catch (LineUnavailableException luaEx) {
                //TODO: throw exception
                luaEx.printStackTrace();
                System.out.println("Source line is unavailable");
                return null;
            }
        }
    }

    // references:
        // TODO: run this method in a separate thread
    // Overview of Java Sound API: https://docs.oracle.com/javase/tutorial/sound/sampled-overview.html
    private void readAudioData() {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // array for I/O audio data
        byte[] data = new byte[this.tdl.getBufferSize() / bufferLimitFactor];

        // TESTING SOURCE LINE
//        SourceDataLine sourceLine = setUpSourceLine();
//        if(sourceLine == null) {
//            System.out.println("setUpSourceLine() failed");
//        }

//        Thread audioCaptureThread = new Thread(new Runnable() {
//            public void run() {
            try {
                System.out.println("Begin audio capture");
                int numBytesRead;
                int totalFramesRead = 0;

                //TODO: specify file path in parameters
                File fileOut = new File("/Users/jeffreygu/Desktop/test.WAV");
                AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

                // begin audio capture
                this.tdl.start();
                AudioInputStream audioInputStream = new AudioInputStream(this.tdl);

                // global boolean set by another thread
//                    while (!audioCaptureStopped) {
                // read next chunk from TDL and fill in the data buffer
//                        numBytesRead = line.read(data,0, data.length);

                //TODO: write byte chunk to audio file
//                       0 out.write(data, 0, numBytesRead);
                AudioSystem.write(audioInputStream, fileType, fileOut);

//                    TEST PLAYBACK
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                        System.out.println("sleepin");
//                    } catch (InterruptedException e) {
//                        System.out.println("sleep interrupted");
//                        e.printStackTrace();
//                    }
//                    sourceLine.write(data, 0, numBytesRead);
                //END TEST PLAYBACK

                // flush out remaining data in the Mixer
//                    if (audioCaptureStopped) line.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("readAudioData: IO exception");
            }
//            }
//        });

//        audioCaptureThread.start();
//        while(audioCaptureThread.isAlive()) {
//            Scanner scan = new Scanner(System.in);
//            System.out.println("End Recording: Enter 0");
//            String input = scan.nextLine();
//            if(input.equals("0")) {
//                System.out.println("User ended recording");
//                audioCaptureStopped = true;
//            }
//        }

    }

    public void finishCapture() {
        this.tdl.stop();
        this.tdl.close();
        this.tdl.flush();
        System.out.println("finished recording");
    }

//    public class AudioCaptureRunner extends Thread {
//        public void run() {
//            System.out.println("AudioCaptureRunner is running");
//        }
//    }
}
