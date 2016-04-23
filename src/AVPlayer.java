import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.border.LineBorder;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

public class AVPlayer {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img;
	BufferedImage[] bufferedImgs;
	PlayImage playImage;

	// peter
	JButton btnReplay;
	JButton btnStart;
	JButton btnStop;

	// peter

	

	public void initialize(String[] args) {

		this.playImage = new PlayImage(args[0]);

		img = playImage.getFirstImage();
		// Use labels to display the images

		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Video: " + args[0]);
		lbText1.setHorizontalAlignment(SwingConstants.LEFT);
		JLabel lbText2 = new JLabel("Audio: " + args[1]);
		lbText2.setHorizontalAlignment(SwingConstants.LEFT);
		lbIm1 = new JLabel(new ImageIcon(img));

		JLabel btnMainLabel = new JLabel();

		JLabel last = new JLabel("last");
		// JLabel button_stop = new JLabel("btn_stop");

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 3;

		frame.getContentPane().add(btnMainLabel, c);

		frame.pack();
		frame.setVisible(true);

		frame.setSize(500, 450);


		ButtonLayOut btnLayOut = new ButtonLayOut();
		btnMainLabel.setPreferredSize(new Dimension(300, 60));
		btnReplay = btnLayOut.initButton(btnMainLabel, 150, 60, "replay.png");
		btnStart = btnLayOut.initButton(btnMainLabel, 200, 60, "start.png");
		btnStop = btnLayOut.initButton(btnMainLabel, 150, 60, "stop.png");

		btnMainLabel.setBorder(new LineBorder(Color.green, 0));
		btnMainLabel.setLayout(new BorderLayout());

		btnMainLabel.add(btnReplay, BorderLayout.WEST);
		btnMainLabel.add(btnStart, BorderLayout.CENTER);
		btnMainLabel.add(btnStop, BorderLayout.EAST);
		setBtnListener();
		

	}

	public void updateFrame() {
		// System.out.print(100000000);
		// System.out.printf("%d, ", current);
		System.out.println(new Date());

		try {
			
		

			while (true) {
			//	BufferedImage img = playImage.getCurrentImg();
				BufferedImage img = playImage.getCurrentImageProcessed();
				while (img == null) {
					//img = playImage.getCurrentImg();
					img =  playImage.getCurrentImageProcessed();
					Thread.sleep(10);
					// System.out.println(img);
				}
				//System.out.println(img);
				lbIm1.setIcon(new ImageIcon(img));
				img = null;
			}

			// System.out.println(i);
			// Thread.sleep(intervalTime);
			// synchronized (currentLock) {
			// current++;
			// }

		}

		catch (InterruptedException e) {

		}
	}

	// peter

	PlaySound playSound;

	// peter

	public void playWAV(String filename) {
		
		
		// opens the inputStream
		FileInputStream inputStream1;
		InputStream inputStream2;
		byte[] buffer = null;
		try {
			System.out.println("step - 1");
			inputStream1 = new FileInputStream(filename);
			//inputStream1 = new FileInputStream("a.wav");
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		File file = new File(filename);
		
	
		System.out.println("step - 2");
		calculate_audio audio = new calculate_audio();
		buffer = audio.calculate_audio(inputStream1,(int)file.length());
/**		
		FileInputStream stream = null;
		try {
			stream = new FileInputStream("a.wav");
			//stream = new FileInputStream("a.wav");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/		
		
		
		// initializes the playSound Object
		playSound = new PlaySound(buffer,filename);
		// plays the sound
		try {
			playSound.play();
		} catch (PlayWaveException | IOException e) {
			e.printStackTrace();
			return;
		}
		
	}

	public void setBtnListener() {
		btnStart.addActionListener(new ActionListener() {
			boolean is_pause = false;

			@Override
			public void actionPerformed(ActionEvent e) {

				// start button was clicked
				if (!is_pause) {
					AVPlayer.this.playImage.startOrContinue();
					btnStart.setIcon(ButtonLayOut.ChangeImgSize(new ImageIcon(
							"pause.png"), 60, 60));
					playSound.startOrResume();
					is_pause = true;
				} else {
					AVPlayer.this.playImage.pause();
					btnStart.setIcon(ButtonLayOut.ChangeImgSize(new ImageIcon(
							"start.png"), 60, 60));
					playSound.Stop();
					is_pause = false;
				}

			}
		});
	}

	public static void main(final String[] args) {
		if (args.length < 2) {
			System.err
					.println("usage: java -jar AVPlayer.jar [RGB file] [WAV file]");
			return;
		}

		final AVPlayer ren = new AVPlayer();
		ren.initialize(args);
		
		

		Thread updateFrameThread = new Thread() {
			public void run() {
				ren.updateFrame();
			}
		};
		updateFrameThread.start();

		ren.playWAV(args[1]);

	}

}