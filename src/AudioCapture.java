import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.TimeUnit;

public class AudioCapture {
    private int bufferLimitFactor = 5;  // fraction of line buffer size that will be read in

    private AudioFormat format = null;
    private String macOSMicrophoneName = "Built-in Microphone";
    private Mixer micMixer = null;

    private static Boolean audioCaptureStopped = false;

    public AudioCapture(String microphoneName, int bufferLimitFactor, AudioFormat format) {
        this.macOSMicrophoneName = microphoneName;
        this.bufferLimitFactor = bufferLimitFactor;
        this.format = format;
    }

    public void setUpMicrophone() {
        // TESTING ONLY
        //        this.listMixers();
        this.getMixer();
        this.setUpTargetDataLine();
    }

    // To play or capture sound using the Java Sound API, you need at least three things:
    private void listMixers() {
        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println("MIXER: "+info);
            for(Line.Info lineInfo : AudioSystem.getMixer(info).getTargetLineInfo()) {
                System.out.println("\tTARGET DATA LINE: "+lineInfo.toString());
            }
        }
    }

    // Obtain mixer associate with built in microphone
    private void getMixer() {
        Mixer.Info micInfo = null;
        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
//            System.out.println(info);
            if (info.getName().equals(macOSMicrophoneName)) {
                System.out.println("found microphone!");
                micInfo = info;
                break;
            }
        }
        if(micInfo == null) {
            System.out.println("Could not find microphone info");
        }
        else {
            this.micMixer = AudioSystem.getMixer(micInfo);
        }
    }

    // Set up target data line to read audio from Mixer
    private void setUpTargetDataLine() {
        TargetDataLine line;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, this.format);
        if(!this.micMixer.isLineSupported(info)){
            //TODO: throw exception
            System.out.println("TargetDataLine not supported by mic mixer");
        }
        // get and open the line
        try {
            line = (TargetDataLine) this.micMixer.getLine(info);
            System.out.println("line buffer size: "+line.getBufferSize());

            // reserve line for this app's use by opening; optional param of buffer size
            line.open(format);

            // line is ready to start capturing audio data (make sure app is ready to begin reading from data line)
            // else you waste processing on filling capture buffer, to have it overflow
            // .read attempts to read in 'len' bytes of data into the byte array, starting from a byte offset
            // you can request more data than the byte array can actually hold
            //            int numBytesRead = line.read(new byte[] {}, 0, 5);

            System.out.println("target data line is ready for use!");
//            TODO: uncomment this next method when ready
            this.readAudioData(line);

//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException ie) {
//                System.out.println("this is rude");
//            }
            System.out.println("line will now close");
            line.close();

        } catch (LineUnavailableException luaEx) {
            //TODO: throw exception
            System.out.println("line is unavailable");
        }
    }

    // references:
        // TODO: run this method in a separate thread
    // Overview of Java Sound API: https://docs.oracle.com/javase/tutorial/sound/sampled-overview.html
    private void readAudioData(TargetDataLine line) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        AudioInputStream in = new AudioInputStream();
        byte[] data = new byte[line.getBufferSize() / bufferLimitFactor];

        Thread audioCaptureThread = new Thread(new Runnable() {
            public void run() {
                System.out.println("Begin audio capture");
                int numBytesRead;
                // begin audio capture
                line.start();

                // global boolean set by another thread
                while(!audioCaptureStopped) {
                    // read next chunk from TDL and fill in the data buffer
                    numBytesRead = line.read(data, 0, data.length);

                    // save data chunk
                    out.write(data, 0, numBytesRead);
                }

                // flush out remaining data in the Mixer
                if(audioCaptureStopped) line.flush();
            }
        });

        audioCaptureThread.start();
        while(audioCaptureThread.isAlive()) {
            Scanner scan = new Scanner(System.in);
            System.out.println("End Recording: Enter 0");
            String input = scan.nextLine();
            if(input.equals("0")) {
                System.out.println("User ended recording");
//                audioCaptureThread.interrupt();
                audioCaptureStopped = true;
            }
        }

    }

    public class AudioCaptureRunner extends Thread {
        public void run() {
            System.out.println("AudioCaptureRunner is running");

        }
    }
}
