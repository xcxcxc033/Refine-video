import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimerTask;

public class PlayImage {
	private int width = 480;
	private int height = 270;
	private int current = 0;
	private int loadFrame = 100;
	private int loadedFrame = -1;
	private Object currentLock = new Object();
	private final int intervalTime = 66;// 66;
	private TimerTask updateFrameTimerTask;
	private Thread imgs;
	private Integer[] locks;
	private BufferedImage[] bufferedImgs;
	private int last = -1;
	private String filename;

	public PlayImage(final String filename) {
		this.filename = filename;
		File file = new File(filename);
		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// long len = file.length();
		long len = width * height * 3;
		bufferedImgs = new BufferedImage[(int) (file.length() / len)];

		locks = new Integer[bufferedImgs.length];
		for (int i = 0; i != locks.length; i++) {
			locks[i] = new Integer(i);
		}
		Thread read = new Thread() {
			public void run() {
				PlayImage.this.allFrames(filename);
			}
		};
		read.start();

	}

	public BufferedImage getFirstImage() {

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		InputStream is = null;

		File file = new File(this.filename);

		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// long len = file.length();
		long len = width * height * 3;
		bufferedImgs = new BufferedImage[(int) (file.length() / len)];

		byte[] bytes = new byte[(int) len];

		int offset = 0;
		int numRead = 0;
		try {
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int ind = 0;
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {

				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind + height * width];
				byte b = bytes[ind + height * width * 2];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
						| (b & 0xff);
				// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x, y, pix);
				ind++;
			}
		}
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
		

	}

	public void allFrames(String filename) {

		try {
			File file = new File(filename);
			InputStream is = new FileInputStream(file);
			allFrames(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void allFrames(InputStream is) {

		try {
			while (true) {
				// System.out.println(current);
				int temp_current = 0;
				synchronized (currentLock) {
					temp_current = current;
				}

				int temp_loadedFrame = loadedFrame;
				for (int i = temp_loadedFrame + 1; i < bufferedImgs.length
						&& i < loadFrame + temp_current; i++) {
					synchronized (locks[i]) {
						if (bufferedImgs[i] == null) {
							bufferedImgs[i] = readNextFrame(is);
							loadedFrame = i;
						}
					}
					// System.out.println(i);
				}
				if (temp_current + loadedFrame / 2 < temp_loadedFrame) {
					Thread.sleep(1);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {

		}
	}

	public BufferedImage readNextFrame(InputStream is) throws IOException {

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		// long len = file.length();
		long len = width * height * 3;
		byte[] bytes = new byte[(int) len];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		int ind = 0;
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {

				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind + height * width];
				byte b = bytes[ind + height * width * 2];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
						| (b & 0xff);
				// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x, y, pix);
				ind++;
			}
		}

		return img;

	}

	public BufferedImage getCurrentImg() {
		synchronized (currentLock) {
			if(current >= bufferedImgs.length){
				return null;
			}
			synchronized (locks[current]) {
				if (last == current) {
					return null;
				} else {
					last = current;
					return bufferedImgs[current];
				}

			}
		}
	}

	public void start() {
		PlayImage.this.current = 0;

		if (updateFrameTimerTask != null) {
			updateFrameTimerTask.cancel();
		}
		java.util.Timer updateFrameTimer = new java.util.Timer();
		updateFrameTimerTask = new TimerTask() {
			public void run() {

				synchronized (PlayImage.this.currentLock) {
					PlayImage.this.current++;

				}

			}
		};

		updateFrameTimer.scheduleAtFixedRate(updateFrameTimerTask, 0,
				PlayImage.this.intervalTime);

	}

	public void pause() {
		if (updateFrameTimerTask != null) {
			updateFrameTimerTask.cancel();
		}
	}

	public void startOrContinue() {
		if (updateFrameTimerTask == null) {
			this.start();
		} else {
			this.avContinue();
		}
	}

	public void avContinue() {
		java.util.Timer updateFrameTimer = new java.util.Timer();
		updateFrameTimerTask = new TimerTask() {
			public void run() {

				synchronized (PlayImage.this.currentLock) {
					PlayImage.this.current++;

				}

			}
		};
		updateFrameTimer.scheduleAtFixedRate(updateFrameTimerTask, 0,
				PlayImage.this.intervalTime);
	}

}
