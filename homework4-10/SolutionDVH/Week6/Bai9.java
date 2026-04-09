interface AudioPlayable {
    void playAudio(String file);
}

interface VideoPlayable {
    void playVideo(String file);
}

class AudioPlayer implements AudioPlayable {
    public void playAudio(String file) {
        System.out.println("Playing audio: " + file);
    }
}

class VideoPlayer implements VideoPlayable {
    public void playVideo(String file) {
        System.out.println("Playing video: " + file);
    }
}

class MediaPlayer {
    private AudioPlayable audio;
    private VideoPlayable video;

    public MediaPlayer(AudioPlayable audio, VideoPlayable video) {
        this.audio = audio;
        this.video = video;
    }

    public void playAudio(String file) {
        if (audio != null) audio.playAudio(file);
    }

    public void playVideo(String file) {
        if (video != null) video.playVideo(file);
    }
}

public class Bai9 {
    public static void main(String[] args) {
        MediaPlayer player = new MediaPlayer(new AudioPlayer(), new VideoPlayer());
        player.playAudio("song.mp3");
        player.playVideo("movie.mp4");
    }
}