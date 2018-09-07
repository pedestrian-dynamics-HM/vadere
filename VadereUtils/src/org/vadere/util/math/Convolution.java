package org.vadere.util.math;

import java.util.Random;
import java.util.function.BiFunction;

import org.vadere.util.geometry.shapes.VPoint;

public class Convolution {

	public static float[] floatGaussian1DKernel(final int size, final float sigma) {
		return floatGaussian1DKernel(size, sigma, true, 1.0f / (float) (Math.sqrt(2.0f * Math.PI) * sigma));
	}

	public static float[] floatGaussian1DKernel(final int size, final float sigma, final boolean normalize,
			final float scaleFactor) {
		float sum = 0;

		if (size % 2 == 0) {
			throw new IllegalArgumentException("size has to be odd.");
		}

		final int centerI = (size + 1) / 2 - 1;
		final float varianz = sigma * sigma;

		final float[] kernel = new float[size];

		// build the gaussian kernel
		for (int i = 0; i < size; i++) {
			float functionValue = (centerI - i) * (centerI - i);

			float value = scaleFactor * (float) Math.exp(-functionValue / (2 * varianz));

			kernel[i] = value;
			sum += value;
		}

		if (normalize) {
			// normalize the kernel
			for (int i = 0; i < size; i++) {
				kernel[i] = kernel[i] / sum;
			}
		}

		return kernel;
	}

	public static float[] floatGaussian1DKernel(final int size, final BiFunction<Integer, Integer, Float> f,
			boolean normalize) {
		float sum = 0;

		if (size % 2 == 0) {
			throw new IllegalArgumentException("size has to be odd.");
		}

		final int centerI = (size + 1) / 2 - 1;
		final float[] kernel = new float[size];

		// build the gaussian kernel
		for (int i = 0; i < size; i++) {
			float value = f.apply(centerI, i);
			kernel[i] = value;
			sum += value;
		}

		if (normalize) {
			// normalize the kernel
			for (int i = 0; i < size; i++) {
				kernel[i] = kernel[i] / sum;
			}
		}

		return kernel;
	}

	public static double[] generateDoubleGaussianKernel(final int size, final double sigma) {
		return doubleGaussianKernel(size, sigma, doubleDefaultNominator);
	}

	public static float[] generateFloatGaussianKernel(final int size, final float sigma) {
		return floatGaussian2DKernel(size, sigma, defaultNominator);
	}


	public static float[] generateDoubleDensityGaussianKernel(final int size, final float sigma) {
		return floatGaussian2DKernel(size, sigma, densityNominator);
	}

	public static float[] generdateInputMatrix(final int size) {
		float[] matrix = new float[size];
		for (int i = 0; i < size; i++) {
			matrix[i] = (float) random.nextDouble();
		}
		return matrix;
	}

	public static float[] convolve(final float[] inMatrix,
			final float[] kernelMatrix,
			final int nWidth,
			final int nHeight,
			final int nFilterWidth) {
		float[] outMatrix = new float[inMatrix.length];

		for (int yOut = 0; yOut < nHeight; yOut++) {
			for (int xOut = 0; xOut < nWidth; xOut++) {
				final int idxOut = yOut * nWidth + xOut;
				outMatrix[idxOut] = convolve(inMatrix, kernelMatrix, nWidth, nHeight, nFilterWidth, xOut, yOut);

			}
		}
		return outMatrix;
	}

	public static float[] convolveSeperate(final float[] inMatrix,
			final float[] rowVector,
			final float[] colVector,
			final int nWidth,
			final int nHeight,
			final int nFilterWidth) {
		float[] tmpOutMatrix = new float[inMatrix.length];
		float[] outMatrix = new float[inMatrix.length];

		for (int yOut = 0; yOut < nHeight; yOut++) {
			for (int xOut = 0; xOut < nWidth; xOut++) {
				final int idxOut = yOut * nWidth + xOut;
				tmpOutMatrix[idxOut] = convolveRow(inMatrix, rowVector, nWidth, nHeight, nFilterWidth, xOut, yOut);
			}
		}

		for (int yOut = 0; yOut < nHeight; yOut++) {
			for (int xOut = 0; xOut < nWidth; xOut++) {
				final int idxOut = yOut * nWidth + xOut;
				outMatrix[idxOut] = convolveCol(tmpOutMatrix, colVector, nWidth, nHeight, nFilterWidth, xOut, yOut);
			}
		}
		return outMatrix;
	}

	public static float[] convolveCol(final float[] inMatrix,
			final float[] rowVector,
			final int nWidth,
			final int nHeight,
			final int nFilterWidth) {
		float[] outMatrix = new float[inMatrix.length];

		for (int yOut = 0; yOut < nHeight; yOut++) {
			for (int xOut = 0; xOut < nWidth; xOut++) {
				final int idxOut = yOut * nWidth + xOut;
				outMatrix[idxOut] = convolveCol(inMatrix, rowVector, nWidth, nHeight, nFilterWidth, xOut, yOut);
			}
		}
		return outMatrix;
	}

	public static float[] convolveRow(final float[] inMatrix,
			final float[] rowVector,
			final int nWidth,
			final int nHeight,
			final int nFilterWidth) {
		float[] outMatrix = new float[inMatrix.length];

		for (int yOut = 0; yOut < nHeight; yOut++) {
			for (int xOut = 0; xOut < nWidth; xOut++) {
				final int idxOut = yOut * nWidth + xOut;
				outMatrix[idxOut] = convolveRow(inMatrix, rowVector, nWidth, nHeight, nFilterWidth, xOut, yOut);
			}
		}

		return outMatrix;
	}

	public static float convolve(final float[] inMatrix,
			final float[] kernelMatrix,
			final int inWidth,
			final int inHeight,
			final int kernelWidth,
			final int x,
			final int y) {
		float sum = 0;

		/**
		 * Crop strategy: Any pixel in the output image which would require values from beyond the
		 * edge is skipped
		 */
		int bottomBorder = (kernelWidth / 2 + 1) - (inHeight - y);
		bottomBorder = bottomBorder > 0 ? bottomBorder : 0;
		int topBorder = y - (kernelWidth / 2) < 0 ? y - (kernelWidth / 2) : 0;

		int rightBorder = (kernelWidth / 2 + 1) - (inWidth - x);
		rightBorder = rightBorder > 0 ? rightBorder : 0;
		int leftBorder = x - (kernelWidth / 2) < 0 ? x - (kernelWidth / 2) : 0;

		for (int r = -kernelWidth / 2 - topBorder; r <= kernelWidth / 2 - bottomBorder; r++) {
			final int idxFtmp = (r + kernelWidth / 2) * kernelWidth;

			final int yIn = y + r;
			final int idxIntmp = yIn * inWidth + x;

			for (int c = -kernelWidth / 2 - leftBorder; c <= kernelWidth / 2 - rightBorder; c++) {
				final int idxF = idxFtmp + (c + kernelWidth / 2);
				final int idxIn = idxIntmp + c;

				if (idxIn < inMatrix.length && idxF >= 0 && idxIn >= 0) {
					sum += kernelMatrix[idxF] * inMatrix[idxIn];
				}
			}
		}
		return sum;
	}


	private static BiFunction<VPoint, VPoint, Float> defaultNominator = (p1, p2) -> {
		VPoint distance = p1.subtract(p2);
		return (float) (distance.x * distance.x + distance.y * distance.y);
	};

	private static BiFunction<VPoint, VPoint, Double> doubleDefaultNominator = (p1, p2) -> {
		VPoint distance = p1.subtract(p2);
		return (distance.x * distance.x + distance.y * distance.y);
	};

	private static BiFunction<VPoint, VPoint, Float> densityNominator =
			(p1, p2) -> (float) Math.sqrt(defaultNominator.apply(p1, p2));

	private static Random random = new Random();

	private static double[] doubleGaussianKernel(final int size,
			final double sigma,
			final BiFunction<VPoint, VPoint, Double> fDistance) {
		double sum = 0;

		if (size % 2 == 0) {
			throw new IllegalArgumentException("size has to be odd.");
		}

		final int centerX = (size + 1) / 2 - 1;
		final int centerY = centerX;
		final VPoint pCenter = new VPoint(centerX, centerY);
		final double varianz = sigma * sigma;
		final double scaleFactor = 1.0 / (2.0 * Math.PI * varianz);
		final double[] kernel = new double[size * size];

		// build the gaussian kernel
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				double functionValue = fDistance.apply(new VPoint(x, y), pCenter);

				double value = scaleFactor * Math.exp(-functionValue / (2 * varianz));

				kernel[y * size + x] = value;
				sum += value;
			}
		}

		// normalize the kernel
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				kernel[y * size + x] = kernel[y * size + x] / sum;
			}
		}
		return kernel;
	}

	private static float[] floatGaussian2DKernel(final int size,
			final float sigma,
			final BiFunction<VPoint, VPoint, Float> fDistance) {
		float sum = 0;

		if (size % 2 == 0) {
			throw new IllegalArgumentException("size has to be odd.");
		}

		final int centerX = (size + 1) / 2 - 1;
		final int centerY = centerX;
		final VPoint pCenter = new VPoint(centerX, centerY);
		final float varianz = sigma * sigma;
		final float scaleFactor = 1.0f / (2.0f * (float) Math.PI * varianz);
		final float[] kernel = new float[size * size];

		// build the gaussian kernel
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				float functionValue = fDistance.apply(new VPoint(x, y), pCenter);

				float value = scaleFactor * (float) Math.exp(-functionValue / (2 * varianz));

				kernel[y * size + x] = value;
				sum += value;
			}
		}

		// normalize the kernel
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				kernel[y * size + x] = kernel[y * size + x] / sum;
			}
		}
		return kernel;
	}

	private static float convolveRow(final float[] inMatrix,
			final float[] kernelVector,
			final int inWidth,
			final int inHeight,
			final int kernelWidth,
			final int x, final int y) {
		float sum = 0;
		int bottomBorder = (kernelWidth / 2 + 1) - (inHeight - y);
		bottomBorder = bottomBorder > 0 ? bottomBorder : 0;
		int topBorder = y - (kernelWidth / 2) < 0 ? y - (kernelWidth / 2) : 0;

		for (int r = -kernelWidth / 2 - topBorder; r <= kernelWidth / 2 - bottomBorder; r++) {
			final int idxF = (r + kernelWidth / 2);
			final int yIn = y * inWidth;
			final int idxIn = yIn + x + r * inWidth;

			// if(idxIn < inMatrix.length && idxF >= 0 && idxIn >= 0) {
			sum += kernelVector[idxF] * inMatrix[idxIn];
			// }
		}

		return sum;
	}

	private static float convolveCol(final float[] inMatrix,
			final float[] kernelVector,
			final int inWidth,
			final int inHeight,
			final int kernelWidth,
			final int x, final int y) {
		float sum = 0;
		int rightBorder = (kernelWidth / 2 + 1) - (inWidth - x);
		rightBorder = rightBorder > 0 ? rightBorder : 0;
		int leftBorder = x - (kernelWidth / 2) < 0 ? x - (kernelWidth / 2) : 0;

		for (int r = -kernelWidth / 2 - leftBorder; r <= kernelWidth / 2 - rightBorder; r++) {
			final int idxF = (r + kernelWidth / 2);
			final int yIn = y * inWidth;
			final int idxIn = yIn + x + r;

			if (idxIn < inMatrix.length && idxF >= 0 && idxIn >= 0) {
				sum += kernelVector[idxF] * inMatrix[idxIn];
			}
		}

		return sum;
	}
}
