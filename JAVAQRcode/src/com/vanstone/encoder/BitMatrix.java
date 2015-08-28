package com.vanstone.encoder;

import java.util.Arrays;

/**
 * <p>
 * 一个二维bits矩阵的封装，一个int代表32个bits。在下面的使用过程中，x代表列位置，y代表行位置。原点在左上角
 * </p> 
 * <p>
 * 内部使用一个int[]代表bits，每一行的开始使用一个新的int，这样便于方便的拷贝到BitArray。
 * </p>
 * @author penghong
 */

public class BitMatrix implements Cloneable {
	private final int width;
	private final int height;
	/**
	 * 每一行的int数目，一个int32bit
	 */
	private final int rowSize;
	private final int[] bits;

	public BitMatrix(int dimension) {
		this(dimension, dimension);
	}

	public BitMatrix(int width, int height) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException(
					"宽度和高度都必须大于 0");
		}
		this.width = width;
		this.height = height;
		this.rowSize = (width + 31) / 32;
		bits = new int[rowSize * height];
	}

	private BitMatrix(int width, int height, int rowSize, int[] bits) {
		this.width = width;
		this.height = height;
		this.rowSize = rowSize;
		this.bits = bits;
	}

	public static BitMatrix parse(String stringRepresentation,
			String setString, String unsetString) {
		if (stringRepresentation == null) {
			throw new IllegalArgumentException();
		}

		boolean[] bits = new boolean[stringRepresentation.length()];
		int bitsPos = 0;
		int rowStartPos = 0;
		int rowLength = -1;
		int nRows = 0;
		int pos = 0;
		while (pos < stringRepresentation.length()) {
			if (stringRepresentation.charAt(pos) == '\n'
					|| stringRepresentation.charAt(pos) == '\r') {
				if (bitsPos > rowStartPos) {
					if (rowLength == -1) {
						rowLength = bitsPos - rowStartPos;
					} else if (bitsPos - rowStartPos != rowLength) {
						throw new IllegalArgumentException(
								"行长度不匹配");
					}
					rowStartPos = bitsPos;
					nRows++;
				}
				pos++;
			} else if (stringRepresentation.substring(pos,
					pos + setString.length()).equals(setString)) {
				pos += setString.length();
				bits[bitsPos] = true;
				bitsPos++;
			} else if (stringRepresentation.substring(pos,
					pos + unsetString.length()).equals(unsetString)) {
				pos += unsetString.length();
				bits[bitsPos] = false;
				bitsPos++;
			} else {
				throw new IllegalArgumentException(
						"无法识别的字符："
								+ stringRepresentation.substring(pos));
			}
		}

		if (bitsPos > rowStartPos) {
			if (rowLength == -1) {
				rowLength = bitsPos - rowStartPos;
			} else if (bitsPos - rowStartPos != rowLength) {
				throw new IllegalArgumentException("行长度不匹配");
			}
			nRows++;
		}

		BitMatrix matrix = new BitMatrix(rowLength, nRows);
		for (int i = 0; i < bitsPos; i++) {
			if (bits[i]) {
				matrix.set(i % rowLength, i / rowLength);
			}
		}
		return matrix;
	}

	/**
	 * <p>
	 * 获取请求的一个bit，返回true代表黑色
	 * </p>
	 * 
	 * @param x
	 *            列
	 * @param y
	 *            行
	 * @return 矩阵中该位置的值
	 */
	public boolean get(int x, int y) {
		int offset = y * rowSize + (x / 32);
		return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
	}

	/**
	 * <p>
	 * 设置矩阵中的一个bit
	 * </p>
	 * 
	 * @param x
	 *            列
	 * @param y
	 *            行
	 */
	public void set(int x, int y) {
		int offset = y * rowSize + (x / 32);
		bits[offset] |= 1 << (x & 0x1f);
	}

	public void unset(int x, int y) {
		int offset = y * rowSize + (x / 32);
		bits[offset] &= ~(1 << (x & 0x1f));
	}

	/**
	 * <p>
	 * 翻转给定的点
	 * </p>
	 * 
	 * @param x
	 *            列
	 * @param y
	 *            行
	 */
	public void flip(int x, int y) {
		int offset = y * rowSize + (x / 32);
		bits[offset] ^= 1 << (x & 0x1f);
	}

	/**
	 * 异或
	 * 
	 * @param mask
	 *            
	 */
	public void xor(BitMatrix mask) {
		if (width != mask.getWidth() || height != mask.getHeight()
				|| rowSize != mask.getRowSize()) {
			throw new IllegalArgumentException(
					"input matrix dimensions do not match");
		}
		BitArray rowArray = new BitArray(width / 32 + 1);
		for (int y = 0; y < height; y++) {
			int offset = y * rowSize;
			int[] row = mask.getRow(y, rowArray).getBitArray();
			for (int x = 0; x < rowSize; x++) {
				bits[offset + x] ^= row[x];
			}
		}
	}

	/**
	 * 重置矩阵全部为0
	 */
	public void clear() {
		int max = bits.length;
		for (int i = 0; i < max; i++) {
			bits[i] = 0;
		}
	}

	/**
	 * <p>
	 * 设置某一个方形区域为true
	 * </p>
	 * 
	 * @param left
	 *            水平起始位置（包括该点）
	 * @param top
	 *            垂直起始点（包括该点）
	 * @param width
	 *            区域宽度
	 * @param height
	 *            区域高度
	 */
	public void setRegion(int left, int top, int width, int height) {
		if (top < 0 || left < 0) {
			throw new IllegalArgumentException(
					"左上起始点不能为负数");
		}
		if (height < 1 || width < 1) {
			throw new IllegalArgumentException(
					"宽度高度不能小于1");
		}
		int right = left + width;
		int bottom = top + height;
		if (bottom > this.height || right > this.width) {
			throw new IllegalArgumentException(
					"区域越界");
		}
		for (int y = top; y < bottom; y++) {
			int offset = y * rowSize;
			for (int x = left; x < right; x++) {
				bits[offset + (x / 32)] |= 1 << (x & 0x1f);
			}
		}
	}

	/**
	 * 快速获取一行数据
	 * 
	 * @param y
	 *            行
	 */
	public BitArray getRow(int y, BitArray row) {
		if (row == null || row.getSize() < width) {
			row = new BitArray(width);
		} else {
			row.clear();
		}
		int offset = y * rowSize;
		for (int x = 0; x < rowSize; x++) {
			row.setBulk(x * 32, bits[offset + x]);
		}
		return row;
	}

	/**
	 * 设置某一行数据
	 * @param y
	 *            行
	 * @param row
	 *            数据源
	 */
	public void setRow(int y, BitArray row) {
		System.arraycopy(row.getBitArray(), 0, bits, y * rowSize, rowSize);
	}

	/**
	 * 旋转180°
	 */
	public void rotate180() {
		int width = getWidth();
		int height = getHeight();
		BitArray topRow = new BitArray(width);
		BitArray bottomRow = new BitArray(width);
		for (int i = 0; i < (height + 1) / 2; i++) {
			topRow = getRow(i, topRow);
			bottomRow = getRow(height - 1 - i, bottomRow);
			topRow.reverse();
			bottomRow.reverse();
			setRow(i, bottomRow);
			setRow(height - 1 - i, topRow);
		}
	}

	/**
	 * 得到二维码的封闭矩形
	 * 
	 * @return {@code left,top,width,height} 
	 *         或者 null如果是空白
	 */
	public int[] getEnclosingRectangle() {
		int left = width;
		int top = height;
		int right = -1;
		int bottom = -1;

		for (int y = 0; y < height; y++) {
			for (int x32 = 0; x32 < rowSize; x32++) {
				int theBits = bits[y * rowSize + x32];
				if (theBits != 0) {
					if (y < top) {
						top = y;
					}
					if (y > bottom) {
						bottom = y;
					}
					if (x32 * 32 < left) {
						int bit = 0;
						while ((theBits << (31 - bit)) == 0) {
							bit++;
						}
						if ((x32 * 32 + bit) < left) {
							left = x32 * 32 + bit;
						}
					}
					if (x32 * 32 + 31 > right) {
						int bit = 31;
						while ((theBits >>> bit) == 0) {
							bit--;
						}
						if ((x32 * 32 + bit) > right) {
							right = x32 * 32 + bit;
						}
					}
				}
			}
		}

		int width = right - left;
		int height = bottom - top;

		if (width < 0 || height < 0) {
			return null;
		}

		return new int[] { left, top, width, height };
	}

	/**
	 * 返回二维码的左上角坐标
	 * 
	 * @return 左上角的xy坐标
	 */
	public int[] getTopLeftOnBit() {
		int bitsOffset = 0;
		while (bitsOffset < bits.length && bits[bitsOffset] == 0) {
			bitsOffset++;
		}
		if (bitsOffset == bits.length) {
			return null;
		}
		int y = bitsOffset / rowSize;
		int x = (bitsOffset % rowSize) * 32;

		int theBits = bits[bitsOffset];
		int bit = 0;
		while ((theBits << (31 - bit)) == 0) {
			bit++;
		}
		x += bit;
		return new int[] { x, y };
	}

	public int[] getBottomRightOnBit() {
		int bitsOffset = bits.length - 1;
		while (bitsOffset >= 0 && bits[bitsOffset] == 0) {
			bitsOffset--;
		}
		if (bitsOffset < 0) {
			return null;
		}

		int y = bitsOffset / rowSize;
		int x = (bitsOffset % rowSize) * 32;

		int theBits = bits[bitsOffset];
		int bit = 31;
		while ((theBits >>> bit) == 0) {
			bit--;
		}
		x += bit;

		return new int[] { x, y };
	}

	/**
	 * @return 矩阵的宽度
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return 矩阵的高度
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return 矩阵的行尺寸int，每一行bit采用多少个int
	 */
	public int getRowSize() {
		return rowSize;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BitMatrix)) {
			return false;
		}
		BitMatrix other = (BitMatrix) o;
		return width == other.width && height == other.height
				&& rowSize == other.rowSize && Arrays.equals(bits, other.bits);
	}

	@Override
	public int hashCode() {
		int hash = width;
		hash = 31 * hash + width;
		hash = 31 * hash + height;
		hash = 31 * hash + rowSize;
		hash = 31 * hash + Arrays.hashCode(bits);
		return hash;
	}

	@Override
	public String toString() {
		return toString("X ", "  ");
	}

	/**
	 * @param setString
	 *            representation of a set bit
	 * @param unsetString
	 *            representation of an unset bit
	 * @return string representation of entire matrix utilizing given strings
	 */
	public String toString(String setString, String unsetString) {
		return toString(setString, unsetString, "\n");
	}

	/**
	 * @param setString
	 *            representation of a set bit
	 * @param unsetString
	 *            representation of an unset bit
	 * @param lineSeparator
	 *            newline character in string representation
	 * @return string representation of entire matrix utilizing given strings
	 *         and line separator
	 * @deprecated call {@link #toString(String,String)} only, which uses \n
	 *             line separator always
	 */
	@Deprecated
	public String toString(String setString, String unsetString,
			String lineSeparator) {
		StringBuilder result = new StringBuilder(height * (width + 1));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result.append(get(x, y) ? setString : unsetString);
			}
			result.append(lineSeparator);
		}
		return result.toString();
	}

	@Override
	public BitMatrix clone() {
		return new BitMatrix(width, height, rowSize, bits.clone());
	}

}
