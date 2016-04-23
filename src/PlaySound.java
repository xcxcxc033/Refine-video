//package org.wikijava.sound.playWave;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

/**
 * 
 * <Replace this with a short description of the class.>
 * 
 * @author Giulio
 */
public class PlaySound {

    private InputStream waveStream;
    int readBytes = 0;
    InputStream waveStreamForCalculate; 

    private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

    /**
     * CONSTRUCTOR
     */
    public PlaySound(InputStream waveStream , InputStream waveStreamForCalculate) {
	//this.waveStream = waveStream;
	this.waveStream = new BufferedInputStream(waveStream);
	this.waveStreamForCalculate = new BufferedInputStream(waveStreamForCalculate);
    }

   // byte[] bytes = IOUtils.toByteArray(waveStream);

    //Peter
    
    SourceDataLine dataLine = null;
    Clip clip = null;
    //Peter
    
   // Clip clip = AudioSystem.getClip(); //cx
    
	public void play() throws PlayWaveException {

	AudioInputStream audioInputStream = null;
	AudioInputStream audioInputStreamForCalculate = null;
	try {
	    audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
	    
	    clip = AudioSystem.getClip();
	} catch (UnsupportedAudioFileException e1) {
	    throw new PlayWaveException(e1);
	} catch (IOException e1) {
	    throw new PlayWaveException(e1);
	} catch (LineUnavailableException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	try {
		audioInputStreamForCalculate = AudioSystem.getAudioInputStream(this.waveStreamForCalculate);
	} catch (UnsupportedAudioFileException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	} catch (IOException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}

	// Obtain the information about the AudioInputStream
	AudioFormat audioFormat = audioInputStream.getFormat();
	Info info = new Info(SourceDataLine.class, audioFormat);

	// opens the audio channel
	//SourceDataLine dataLine = null; //Peter Delete
	try {
		clip.open(audioInputStream);
//	    dataLine = (SourceDataLine) AudioSystem.getLine(info);
//	    dataLine.open(audioFormat, this.EXTERNAL_BUFFER_SIZE);
	} catch (LineUnavailableException e1) {
	    throw new PlayWaveException(e1);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	

	// Starts the music :P

//	dataLine.start();

	
	byte[] audioBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];
//	int framesize = audioBuffer.length;
//	byte[] packet = new byte[framesize];
	try {
		readBytes = audioInputStreamForCalculate.read(audioBuffer, 0, audioBuffer.length);
		//int numBytesRead = audioInputStream.read(packet, 0, framesize);
		System.out.println("b1"+readBytes);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	System.out.println(this.EXTERNAL_BUFFER_SIZE/15/60/5);
	System.out.println("b2" + readBytes);
	
	for(int i = 0; i < readBytes; i++){
		audioBuffer[i] = (byte)(-1 * audioBuffer[i]);
//		System.out.println(audioBuffer[i]);
	}
		


    }
	
	public void startOrResume(){
		if(clip == null){
			try {
				this.play();
			} catch (PlayWaveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			this.Resume();
		}
	}
	//peter
		public void Stop(){
//			dataLine.stop();
			clip.stop();
			System.out.println("stop");
		}
		
		public void Resume(){
			clip.start();
			
//			dataLine.start();
			System.out.println("resume");
		}
		//peter
	
}
