interface AudioPlayable{void playAudio(String file);}
interface VideoPlayable{void playVideo(String file);}
class AudioPlayer implements AudioPlayable{public void playAudio(String file){System.out.println("Chạy audio : "+file);}}
class VideoPlayer implements VideoPlayable{public void playVideo(String file){System.out.println("Chạy video : "+file);}}
class MediaPlayer{
    private AudioPlayable a;
    private VideoPlayable b;
    public MediaPlayer(AudioPlayable audio,VideoPlayable video){this.a=audio;this.b=video;}
public void playAudio(String file){a.playAudio(file);}
    public void playVideo(String file){b.playVideo(file);}


}
public class Playable {public static void main(String[]args){
    AudioPlayable a=new AudioPlayer();
    VideoPlayable b=new VideoPlayer();
    MediaPlayer c=new MediaPlayer(a,b);
    c.playAudio("SONG GIO");
    c.playVideo("BAC PHAN");
}
}
