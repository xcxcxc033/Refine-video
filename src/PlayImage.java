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
	
	//peter
	public int[][][] histogram = new int[4][4][4];
	public int[][][] nextHistogram = new int[4][4][4];
	public int frameRate = 15;
	public int secForScene = 5;
	public int SceneFrameCounter = 0;
	public boolean findNextScene = false;
	public int senceDiff = 0;
	public int senceThreshold = 5000000;
	public int[] sumFrame;
	public int numOfFrame = 0;
	public int sumIndex = 0;
	public int[] senceChangeFrame;
	public int senceChangeIndex =0;
	//peter
	

	public PlayImage(String filename) {
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
		
		
		//peter
		sumFrame = new int[(int) (file.length() / len)];
		senceChangeFrame = new int[(int) (file.length() / len)];
		senceChangeFrame[0]=0;
		//peter

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
						; i++) {
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
		
		if(findNextScene){
			SceneFrameCounter = 0;
			System.out.println("findNextScene" + findNextScene);
		
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {

				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind + height * width];
				byte b = bytes[ind + height * width * 2];
//peter
				int red = r;
				if (r < 0) red = red+256;
				int green = g;
				if (g < 0) green = green+256;
				int blue = b;
				if (b < 0) blue = blue+256;
				
				r = (byte) red;
				g = (byte) green;
				b = (byte) blue;
				
				nextHistogram[red / 64][green / 64][blue / 64]++;
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
						| (b & 0xff);
				// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x, y, pix);
				ind++;
			}
		}
		for(int i = 0; i < histogram.length; i++)
            for(int j = 0; j < histogram[i].length; j++)
                for(int p = 0; p < histogram[i][j].length; p++){
                	senceDiff = Math.abs(histogram[i][j][p] - nextHistogram[i][j][p]);
                }
		if(senceDiff >= senceThreshold){
			findNextScene = false;
			for(int i = 0; i < histogram.length; i++)
	            for(int j = 0; j < histogram[i].length; j++)
	                for(int p = 0; p < histogram[i][j].length; p++){
	                	histogram[i][j][p] = nextHistogram[i][j][p];
	                }
			senceChangeIndex++;
			senceChangeFrame[senceChangeIndex]=numOfFrame;
		}
		senceDiff = 0;
		
	}
		else{
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {

				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind + height * width];
				byte b = bytes[ind + height * width * 2];
//peter
				if( SceneFrameCounter >= frameRate*secForScene){
				int red = r;
				if (r < 0) red = red+256;
				int green = g;
				if (g < 0) green = green+256;
				int blue = b;
				if (b < 0) blue = blue+256;
				
				r = (byte) red;
				g = (byte) green;
				b = (byte) blue;
				
				histogram[red / 64][green / 64][blue / 64]++;
				findNextScene = true;
				}
				
//peter			
//				 for(int i = 0; i < histogram.length; i++)
//			            for(int j = 0; j < histogram[i].length; j++)
//			                for(int p = 0; p < histogram[i][j].length; p++){
//			                	
//			                }
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
						| (b & 0xff);
				// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x, y, pix);
				ind++;
				
				
			}
		}
		sumFrame[sumIndex] = numOfFrame;
		sumIndex++;
		SceneFrameCounter++;
		System.out.println(SceneFrameCounter);
		}
		
		numOfFrame++;
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
	public BufferedImage getCurrentImgScenery() {
		synchronized (currentLock) {
			if(current >= bufferedImgs.length){
				return null;
			}
			synchronized (locks[current]) {
				if (last == current) {
					return null;
				} else {
					last = current;
					return bufferedImgs[sumFrame[current]];
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
