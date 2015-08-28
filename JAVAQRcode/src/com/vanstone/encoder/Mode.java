package com.vanstone.encoder;

/**
 * <p>
 * 见 ISO 18004:2006, 6.4.1, Tables 2 and 3. 枚举封装，标准中各种各样可以编码的模式
 * ，例如数字，数字字符，kanji等等模式
 * </p>
 * 
 * @author Sean Owen
 */

public enum Mode {
	TERMINATOR(new int[] { 0, 0, 0 }, 0x00), // 并非正真的模式
	NUMERIC(new int[] { 10, 12, 14 }, 0x01),
	ALPHANUMERIC(new int[] { 9, 11, 13 }, 0x02), 
	STRUCTURED_APPEND(new int[] { 0, 0,0 }, 0x03), // 不支持
	BYTE(new int[] { 8, 16, 16 }, 0x04), 
	ECI(new int[] { 0, 0, 0 }, 0x07), // 标准22页，表2 iso-iec -18004
	KANJI(new int[] { 8, 10, 12 }, 0x08), 
	FNC1_FIRST_POSITION(new int[] { 0, 0,0 }, 0x05), 
	FNC1_SECOND_POSITION(new int[] { 0, 0, 0 }, 0x09),
	/** 见标准 GBT 18284-2000; "Hanzi" 中文. */
	HANZI(new int[] { 8, 10, 12 }, 0x0D);

	private final int[] characterCountBitsForVersions;
	private final int bits;

	Mode(int[] characterCountBitsForVersions, int bits) {
		this.characterCountBitsForVersions = characterCountBitsForVersions;
		this.bits = bits;
	}

	/**
	 * @param bits
	 *            数据类型模式的在标准中的编码
	 * @return 编码对应的模式
	 * @throws IllegalArgumentException
	 *             如果编码不代表任何模式，抛出异常
	 */
	public static Mode forBits(int bits) {
		switch (bits) {
		case 0x0:
			return TERMINATOR;
		case 0x1:
			return NUMERIC;
		case 0x2:
			return ALPHANUMERIC;
		case 0x3:
			return STRUCTURED_APPEND;
		case 0x4:
			return BYTE;
		case 0x5:
			return FNC1_FIRST_POSITION;
		case 0x7:
			return ECI;
		case 0x8:
			return KANJI;
		case 0x9:
			return FNC1_SECOND_POSITION;
		case 0xD:
			// 0xD 在 GBT 18284-2000定义, 在其他国家可能并不支持
			return HANZI;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @param version
	 *            版本
	 * @return 版本可以编码的数目
	 */
	public int getCharacterCountBits(Version version) {
		int number = version.getVersionNumber();
		int offset;
		if (number <= 9) {
			offset = 0;
		} else if (number <= 26) {
			offset = 1;
		} else {
			offset = 2;
		}
		return characterCountBitsForVersions[offset];
	}

	public int getBits() {
		return bits;
	}
}
