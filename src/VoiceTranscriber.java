import javax.sound.sampled.AudioFormat;

public class VoiceTranscriber {
    public static void main(String[] args) {
        int RECORD_TIME = 20000;

        AudioCapture ac = new AudioCapture(5,
                new AudioFormat(16000f, 8, 2, true, false),
                "Built-in Microphone", ""
        );
        if(ac.setUpAudioSystem()){
            ac.handleTargetDataLine();
        }

        Thread stopper = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(RECORD_TIME);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                ac.finishCapture();
            }
        });
    }
}

//TODO: allow user to specify audioformat (e.g. encoding other than linear PCM)

