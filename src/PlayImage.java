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
	private int motionlessFrame = 0;
	private double[] evaluateMotionResult;
	private boolean processFinished = false;
	private int[] frameNumberToPlay;
	

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
				PlayImage.this.evaluateMotionResult = PlayImage.this.getEvaluateMotionResult();
				PlayImage.this.frameNumberToPlay = PlayImage.this.generateFrameNumberToPlay(10);
				for(int i = 0; i != PlayImage.this.frameNumberToPlay.length; i++){
					System.out.println(PlayImage.this.frameNumberToPlay[i]);
				}
				processFinished = true;
				
			}
		};
		read.start();

	}
	
	public int[] generateFrameNumberToPlay(double valve){
		double[] evaluateValue = getEvaluateValue();
		int[] temp = new int[bufferedImgs.length];
		int current = 0;
		for(int i = 0; i!= evaluateValue.length; i++){
			if(evaluateValue[i] > valve){
				temp[current] = i;
				current++;
			}
		}
		return temp;
	}
	public double[] getEvaluateValue(){
		return evaluateMotionResult;
	}
	
	
	
	
	public BufferedImage getCurrentImageIgnoreMotionless(){
		if(this.processFinished == false){
			return null;
		}
		synchronized (currentLock) {
			if(frameNumberToPlay[current] >= bufferedImgs.length){
				return null;
			}
			synchronized (locks[current]) {
				if (last == current) {
					return null;
				} else if(bufferedImgs[frameNumberToPlay[current]] == null){
					System.out.println("fdsfssfsdfsf");
					return null;
				}
				else {
					
					last = current;
					return bufferedImgs[frameNumberToPlay[current]];
				}

			}
		}
	}
	
	public double[] getEvaluateMotionResult(){
//		EvaluateMotion evaluateMotion = new EvaluateMotionByFramePredict(15, 15, 5, 5);
		EvaluateMotion evaluateMotion = new EvaluateMotionByCompareAverageValueInBlock(15, 15, 100);
		
		double[] result = new double[bufferedImgs.length];
		result[0] = 0;
		for(int i = 1; i < bufferedImgs.length;i++){
			System.out.println(bufferedImgs[0]);
			System.out.println(bufferedImgs[1]);
			result[i] = evaluateMotion.evaluateMotionBetweenImage(bufferedImgs[i-1], bufferedImgs[i]);
			System.out.println(i);
			System.out.println(result[i]);
		}
		return result;
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
			for (int i = 0; i < bufferedImgs.length; i++) {
				
				synchronized (locks[i]) {
					if (bufferedImgs[i] == null) {
						BufferedImage temp = readNextFrame(is);
						if(i == 0){
							System.out.println(temp);
						}
						bufferedImgs[i] = temp;
						loadedFrame = i;
						System.out.println(i);
					}
				}
				
				// System.out.println(i);
			}
		} catch (IOException e) {
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
//		System.out.println(img);
		return img;

	}

	public BufferedImage getCurrentImg() {
		EvaluateMotion evaluateMotion = new EvaluateMotionByCompareAverageValueInBlock(15, 15, 100);
		synchronized (currentLock) {
			if(current >= bufferedImgs.length){
				return null;
			}
			synchronized (locks[current]) {
				if (last == current) {
					return null;
				} else {
					if(last!= 0 && last != -1){
						System.out.println(evaluateMotion.evaluateMotionBetweenImage(bufferedImgs[last], bufferedImgs[current]));
					}
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
