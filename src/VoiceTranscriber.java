import javax.sound.sampled.AudioFormat;

public class VoiceTranscriber {
    public static void main(String[] args) {
        System.out.println("hello");
        AudioCapture ac = new AudioCapture("Built-in Microphone",5,
                new AudioFormat(8000f, 8, 2, true, false)
        );
        ac.setUpMicrophone();
        //ac.setUpTargetDataLine(new AudioFormat());
    }
}

//TODO: allow user to specify audioformat (e.g. encoding other than linear PCM)

