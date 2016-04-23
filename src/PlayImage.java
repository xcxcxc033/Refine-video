import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
	private double[] evaluateSenceChangeResult;

	// peter
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
	public int senceChangeIndex = 0;

	// peter

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

		// peter
		sumFrame = new int[(int) (file.length() / len)];
		senceChangeFrame = new int[(int) (file.length() / len)];
		senceChangeFrame[0] = 0;
		// peter

		locks = new Integer[bufferedImgs.length];
		for (int i = 0; i != locks.length; i++) {
			locks[i] = new Integer(i);
		}
		Thread read = new Thread() {
			public void run() {
				PlayImage.this.allFrames(filename);
				PlayImage.this.evaluateMotionResult = PlayImage.this
						.getEvaluateMotionResult();
				PlayImage.this.evaluateSenceChangeResult = PlayImage.this.getSenceChangeResult();
				PlayImage.this.frameNumberToPlay = PlayImage.this
						.generateFrameNumberToPlay(200);
//				for (int i = 0; i != PlayImage.this.frameNumberToPlay.length; i++) {
//					System.out.println(PlayImage.this.frameNumberToPlay[i]);
//				}
				CommunicateVariables communicateVariables = CommunicateVariables.getSingular();
				communicateVariables.imageIndexInput(frameNumberToPlay);
				while(! communicateVariables.finished()){
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				frameNumberToPlay = communicateVariables.getIndex();
//				for (int i = 0; i != PlayImage.this.frameNumberToPlay.length; i++) {
//					System.out.println(PlayImage.this.frameNumberToPlay[i]);
//				}
				processFinished = true;

			}
		};
		read.start();

	}
	
	public double[] getSenceChangeResult(){
		double[] result = new double[bufferedImgs.length];
		List<Integer> temp = new ArrayList<>();
		temp.add(0);
		for(int i = 0; i != senceChangeFrame.length; i++){
			if(senceChangeFrame[i] != 0){
				temp.add(senceChangeFrame[i]);
				System.out.println(senceChangeFrame[i]);
			}
		}
		
		for(int i = 0; i != temp.size(); i++){
			for(int j = 0 ; j != frameRate * secForScene; j++){
				if(i+j < result.length){
					result[temp.get(i)+j] = 500;
				}
			}
		}
		
		return result;
	}

	public int[] generateFrameNumberToPlay(double valve) {
		double[] evaluateValue = getEvaluateValue();
		int[] temp = new int[bufferedImgs.length];
		int current = 0;
		for (int i = 0; i != evaluateValue.length; i++) {
			if (evaluateValue[i] > valve) {
				temp[current] = i;
				current++;
			}
		}
		System.out.println(current);
		return temp;
	}

	public double[] getEvaluateValue() {
		double[] result = new double[bufferedImgs.length];
		for(int i = 0; i != result.length; i++){
			result[i] = evaluateMotionResult[i] + evaluateSenceChangeResult[i];
//			System.out.printf("%f, %f, %d\n", evaluateMotionResult[i], evaluateSenceChangeResult[i], i);
		}
		return result;
	}
	

	public BufferedImage getCurrentImageProcessed() {
		if (this.processFinished == false) {
			return null;
		}
		synchronized (currentLock) {
			if(current >= frameNumberToPlay.length){
				return null;
			}
			if (frameNumberToPlay[current] >= bufferedImgs.length) {
				return null;
			}
			synchronized (locks[current]) {
				if (last == current) {
					return null;
				} else if (bufferedImgs[frameNumberToPlay[current]] == null) {
//					System.out.println("fdsfssfsdfsf");
					return null;
				} else {

					last = current;
					return bufferedImgs[frameNumberToPlay[current]];
				}

			}
		}
	}

	public double[] getEvaluateMotionResult() {
		// EvaluateMotion evaluateMotion = new EvaluateMotionByFramePredict(15,
		// 15, 5, 5);
		EvaluateMotion evaluateMotion = new EvaluateMotionByCompareAverageValueInBlock(
				15, 15, 500);

		double[] result = new double[bufferedImgs.length];
		result[0] = 0;
		for (int i = 1; i < bufferedImgs.length; i++) {
//			System.out.println(bufferedImgs[0]);
//			System.out.println(bufferedImgs[1]);
			result[i] = evaluateMotion.evaluateMotionBetweenImage(
					bufferedImgs[i - 1], bufferedImgs[i]);
//			System.out.println(i);
//			System.out.println(result[i]);
		}
		double temp = 0;
		for (int i = 0; i < bufferedImgs.length; i+= frameRate) {
			for(int j = 0; j != frameRate; j++){
				if(temp < result[i+j]){
					temp= result[i+j];
				}
			}
			for(int j = 0; j != frameRate; j++){
				result[i+j] = temp;
			}
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
						bufferedImgs[i] = temp;
						loadedFrame = i;
					}

//					System.out.println(i);
				}
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

		if (findNextScene) {
			SceneFrameCounter = 0;
//			System.out.println("findNextScene" + findNextScene);

			for (int y = 0; y < height; y++) {

				for (int x = 0; x < width; x++) {

					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];
					// peter
					int red = r;
					if (r < 0)
						red = red + 256;
					int green = g;
					if (g < 0)
						green = green + 256;
					int blue = b;
					if (b < 0)
						blue = blue + 256;

					r = (byte) red;
					g = (byte) green;
					b = (byte) blue;

					nextHistogram[red / 64][green / 64][blue / 64]++;

					int pix = 0xff000000 | ((r & 0xff) << 16)
							| ((g & 0xff) << 8) | (b & 0xff);
					// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
			for (int i = 0; i < histogram.length; i++)
				for (int j = 0; j < histogram[i].length; j++)
					for (int p = 0; p < histogram[i][j].length; p++) {
						senceDiff = Math.abs(histogram[i][j][p]
								- nextHistogram[i][j][p]);
					}
			if (senceDiff >= senceThreshold) {
				findNextScene = false;
				for (int i = 0; i < histogram.length; i++)
					for (int j = 0; j < histogram[i].length; j++)
						for (int p = 0; p < histogram[i][j].length; p++) {
							histogram[i][j][p] = nextHistogram[i][j][p];
						}
				senceChangeIndex++;
				senceChangeFrame[senceChangeIndex] = numOfFrame;
			}
			senceDiff = 0;

		} else {
			for (int y = 0; y < height; y++) {

				for (int x = 0; x < width; x++) {

					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];
					// peter
					if (SceneFrameCounter >= frameRate * secForScene) {
						int red = r;
						if (r < 0)
							red = red + 256;
						int green = g;
						if (g < 0)
							green = green + 256;
						int blue = b;
						if (b < 0)
							blue = blue + 256;

						r = (byte) red;
						g = (byte) green;
						b = (byte) blue;

						histogram[red / 64][green / 64][blue / 64]++;
						findNextScene = true;
					}

					// peter
					// for(int i = 0; i < histogram.length; i++)
					// for(int j = 0; j < histogram[i].length; j++)
					// for(int p = 0; p < histogram[i][j].length; p++){
					//
					// }

					int pix = 0xff000000 | ((r & 0xff) << 16)
							| ((g & 0xff) << 8) | (b & 0xff);
					// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x, y, pix);
					ind++;

				}
			}
			sumFrame[sumIndex] = numOfFrame;
			sumIndex++;
			SceneFrameCounter++;
//			System.out.println(SceneFrameCounter);
		}

		numOfFrame++;
		return img;

	}

	public BufferedImage getCurrentImg() {
		synchronized (currentLock) {
			if (current >= bufferedImgs.length) {
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
			if (current >= bufferedImgs.length) {
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
