package com.vanstone.encoder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;



public final class QREncoder {
	// The original table is defined in the table 5 of JISX0510:2004 (p.19).
	  private static final int[] ALPHANUMERIC_TABLE = {
	      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
	      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
	      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
	      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
	      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
	      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
	  };

	  static final String DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";

	  private QREncoder() {
	  }

	  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
	  // Basically it applies four rules and summate all penalties.
	  private static int calculateMaskPenalty(ByteMatrix matrix) {
	    return MaskUtil.applyMaskPenaltyRule1(matrix)
	        + MaskUtil.applyMaskPenaltyRule2(matrix)
	        + MaskUtil.applyMaskPenaltyRule3(matrix)
	        + MaskUtil.applyMaskPenaltyRule4(matrix);
	  }

	  /**
	   * @param content text to encode
	   * @param ecLevel error correction level to use
	   * @return {@link QRCode} representing the encoded QR code
	   * @throws WriterException if encoding can't succeed, because of for example invalid content
	   *   or configuration
	   */
	  public static QRCode encode(String content, ErrorCorrectionLevel ecLevel) throws WriterException {
	    return encode(content, ecLevel, null);
	  }
	  /**
	   * 编码给定的内容进二维码
	   * @param content 待编码的字符串
	   * @param ecLevel 错误水平
	   * @param hints 其他参数，例如字符编码等
	   * @return 返回二维码对象
	   * @throws WriterException
	   */
	  public static QRCode encode(String content,
	                              ErrorCorrectionLevel ecLevel,
	                              Map<EncodeHintType,?> hints) throws WriterException {

	    // 字符编码，如果没有设置，采用默认ISO-8859-1
	    String encoding = hints == null ? null : (String) hints.get(EncodeHintType.CHARACTER_SET);
	    if (encoding == null) {
	      encoding = DEFAULT_BYTE_MODE_ENCODING;
	    }

	    //步骤1，根据内容选择合适的模式
	    Mode mode = chooseMode(content, encoding);

	    //用来存储头部信息，例如模式，长度，ECI块等。
	    BitArray headerBits = new BitArray();

	    // 步骤2，如果可能，增加ECI块
	    if (mode == Mode.BYTE && !DEFAULT_BYTE_MODE_ENCODING.equals(encoding)) {
	      CharacterSetECI eci = CharacterSetECI.getCharacterSetECIByName(encoding);
	      if (eci != null) {
	        appendECI(eci, headerBits);
	      }
	    }

	    // 文档24页example ECI+Mode
	    appendModeInfo(mode, headerBits);

	    
	    // 将主要数据单独放在dataBits
	    BitArray dataBits = new BitArray();
	    //将主数据content根据不同的mode追加到dataBits中
	    appendBytes(content, mode, dataBits, encoding);

	    //选择版本，需要知道数据的量，这里先假设为最小版本。

	    int provisionalBitsNeeded = headerBits.getSize()
	        + mode.getCharacterCountBits(Version.getVersionForNumber(1))
	        + dataBits.getSize();
	    Version provisionalVersion = chooseVersion(provisionalBitsNeeded, ecLevel);


	    int bitsNeeded = headerBits.getSize()
	        + mode.getCharacterCountBits(provisionalVersion)
	        + dataBits.getSize();
	    Version version = chooseVersion(bitsNeeded, ecLevel);

	    BitArray headerAndDataBits = new BitArray();
	    headerAndDataBits.appendBitArray(headerBits);
	    // 找到数据块的长度
	    int numLetters = mode == Mode.BYTE ? dataBits.getSizeInBytes() : content.length();
	    appendLengthInfo(numLetters, version, mode, headerAndDataBits);
	    // 头部数据和主要数据封装
	    headerAndDataBits.appendBitArray(dataBits);

	    Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
	    int numDataBytes = version.getTotalCodewords() - ecBlocks.getTotalECCodewords();

	    // 结束bits
	    terminateBits(numDataBytes, headerAndDataBits);

	    // 插入错误校验码数据，分块的数目可以参考38 table 9
	    BitArray finalBits = interleaveWithECBytes(headerAndDataBits,
	                                               version.getTotalCodewords(),
	                                               numDataBytes,
	                                               ecBlocks.getNumBlocks());

	    QRCode qrCode = new QRCode();

	    qrCode.setECLevel(ecLevel);
	    qrCode.setMode(mode);
	    qrCode.setVersion(version);

	    //  选择 mask pattern 并设置
	    int dimension = version.getDimensionForVersion();
	    ByteMatrix matrix = new ByteMatrix(dimension, dimension);
	    int maskPattern = chooseMaskPattern(finalBits, ecLevel, version, matrix);
	    qrCode.setMaskPattern(maskPattern);

	    // 构建矩阵
	    MatrixUtil.buildMatrix(finalBits, ecLevel, version, maskPattern, matrix);
	    qrCode.setMatrix(matrix);

	    return qrCode;
	  }

	  /**
	   * @return the code point of the table used in alphanumeric mode or
	   *  -1 if there is no corresponding code in the table.
	   */
	  static int getAlphanumericCode(int code) {
	    if (code < ALPHANUMERIC_TABLE.length) {
	      return ALPHANUMERIC_TABLE[code];
	    }
	    return -1;
	  }

	  public static Mode chooseMode(String content) {
	    return chooseMode(content, null);
	  }

	  /**
	   * 通过检查编码内容来选择最适合的模式，其中参数 encoding 只是建议模式
	   */
	  private static Mode chooseMode(String content, String encoding) {
	    if ("Shift_JIS".equals(encoding)) {
	      // 如果所有字符都是双字节编码，则选择Kanji
	      return isOnlyDoubleByteKanji(content) ? Mode.KANJI : Mode.BYTE;
	    }
	    boolean hasNumeric = false;
	    boolean hasAlphanumeric = false;
	    for (int i = 0; i < content.length(); ++i) {
	      char c = content.charAt(i);
	      if (c >= '0' && c <= '9') {
	        hasNumeric = true;
	      } else if (getAlphanumericCode(c) != -1) {
	        hasAlphanumeric = true;
	      } else {
	        return Mode.BYTE;
	      }
	    }
	    if (hasAlphanumeric) {
	      return Mode.ALPHANUMERIC;
	    }
	    if (hasNumeric) {
	      return Mode.NUMERIC;
	    }
	    return Mode.BYTE;
	  }

	  private static boolean isOnlyDoubleByteKanji(String content) {
	    byte[] bytes;
	    try {
	      bytes = content.getBytes("Shift_JIS");
	    } catch (UnsupportedEncodingException ignored) {
	      return false;
	    }
	    int length = bytes.length;
	    if (length % 2 != 0) {
	      return false;
	    }
	    for (int i = 0; i < length; i += 2) {
	      int byte1 = bytes[i] & 0xFF;
	      if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
	        return false;
	      }
	    }
	    return true;
	  }

	  private static int chooseMaskPattern(BitArray bits,
	                                       ErrorCorrectionLevel ecLevel,
	                                       Version version,
	                                       ByteMatrix matrix) throws WriterException {

	    int minPenalty = Integer.MAX_VALUE;  // Lower penalty is better.
	    int bestMaskPattern = -1;
	    // We try all mask patterns to choose the best one.
	    for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++) {
	      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
	      int penalty = calculateMaskPenalty(matrix);
	      if (penalty < minPenalty) {
	        minPenalty = penalty;
	        bestMaskPattern = maskPattern;
	      }
	    }
	    return bestMaskPattern;
	  }

	  private static Version chooseVersion(int numInputBits, ErrorCorrectionLevel ecLevel) throws WriterException {
	    // In the following comments, we use numbers of Version 7-H.
	    for (int versionNum = 1; versionNum <= 40; versionNum++) {
	      Version version = Version.getVersionForNumber(versionNum);
	      // numBytes = 196
	      int numBytes = version.getTotalCodewords();
	      // getNumECBytes = 130
	      Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
	      int numEcBytes = ecBlocks.getTotalECCodewords();
	      // getNumDataBytes = 196 - 130 = 66
	      int numDataBytes = numBytes - numEcBytes;
	      int totalInputBytes = (numInputBits + 7) / 8;
	      if (numDataBytes >= totalInputBytes) {
	        return version;
	      }
	    }
	    throw new WriterException("Data too big");
	  }

	  /**
	   * Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
	   */
	  static void terminateBits(int numDataBytes, BitArray bits) throws WriterException {
	    int capacity = numDataBytes * 8;
	    if (bits.getSize() > capacity) {
	      throw new WriterException("data bits cannot fit in the QR Code" + bits.getSize() + " > " +
	          capacity);
	    }
	    for (int i = 0; i < 4 && bits.getSize() < capacity; ++i) {
	      bits.appendBit(false);
	    }
	    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
	    // If the last byte isn't 8-bit aligned, we'll add padding bits.
	    int numBitsInLastByte = bits.getSize() & 0x07;    
	    if (numBitsInLastByte > 0) {
	      for (int i = numBitsInLastByte; i < 8; i++) {
	        bits.appendBit(false);
	      }
	    }
	    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
	    int numPaddingBytes = numDataBytes - bits.getSizeInBytes();
	    for (int i = 0; i < numPaddingBytes; ++i) {
	      bits.appendBits((i & 0x01) == 0 ? 0xEC : 0x11, 8);
	    }
	    if (bits.getSize() != capacity) {
	      throw new WriterException("Bits size does not equal capacity");
	    }
	  }

	  /**
	   * Get number of data bytes and number of error correction bytes for block id "blockID". Store
	   * the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
	   * JISX0510:2004 (p.30)
	   */
	  static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes,
	                                                     int numDataBytes,
	                                                     int numRSBlocks,
	                                                     int blockID,
	                                                     int[] numDataBytesInBlock,
	                                                     int[] numECBytesInBlock) throws WriterException {
	    if (blockID >= numRSBlocks) {
	      throw new WriterException("Block ID too large");
	    }
	    // numRsBlocksInGroup2 = 196 % 5 = 1
	    int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
	    // numRsBlocksInGroup1 = 5 - 1 = 4
	    int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
	    // numTotalBytesInGroup1 = 196 / 5 = 39
	    int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
	    // numTotalBytesInGroup2 = 39 + 1 = 40
	    int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
	    // numDataBytesInGroup1 = 66 / 5 = 13
	    int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
	    // numDataBytesInGroup2 = 13 + 1 = 14
	    int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
	    // numEcBytesInGroup1 = 39 - 13 = 26
	    int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
	    // numEcBytesInGroup2 = 40 - 14 = 26
	    int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
	    // Sanity checks.
	    // 26 = 26
	    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
	      throw new WriterException("EC bytes mismatch");
	    }
	    // 5 = 4 + 1.
	    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
	      throw new WriterException("RS blocks mismatch");
	    }
	    // 196 = (13 + 26) * 4 + (14 + 26) * 1
	    if (numTotalBytes !=
	        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
	            numRsBlocksInGroup1) +
	            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
	                numRsBlocksInGroup2)) {
	      throw new WriterException("Total bytes mismatch");
	    }

	    if (blockID < numRsBlocksInGroup1) {
	      numDataBytesInBlock[0] = numDataBytesInGroup1;
	      numECBytesInBlock[0] = numEcBytesInGroup1;
	    } else {
	      numDataBytesInBlock[0] = numDataBytesInGroup2;
	      numECBytesInBlock[0] = numEcBytesInGroup2;
	    }
	  }

	  /**
	   * 利用RS编码插入错误检测码
	   */
	  static BitArray interleaveWithECBytes(BitArray bits,
	                                        int numTotalBytes,
	                                        int numDataBytes,
	                                        int numRSBlocks) throws WriterException {

	    if (bits.getSizeInBytes() != numDataBytes) {
	      throw new WriterException("Number of bits and data bytes does not match");
	    }

	    //步骤1.划分数据成块，并生成块的错误检测码。存储块和块检测码到新的块
	    int dataBytesOffset = 0;
	    int maxNumDataBytes = 0;
	    int maxNumEcBytes = 0;

	    //rs块的数目是已知的，通过文档可以知道
	    Collection<BlockPair> blocks = new ArrayList<>(numRSBlocks);

	    for (int i = 0; i < numRSBlocks; ++i) {
	      int[] numDataBytesInBlock = new int[1];
	      int[] numEcBytesInBlock = new int[1];
	      getNumDataBytesAndNumECBytesForBlockID(
	          numTotalBytes, numDataBytes, numRSBlocks, i,
	          numDataBytesInBlock, numEcBytesInBlock);

	      int size = numDataBytesInBlock[0];
	      byte[] dataBytes = new byte[size];
	      bits.toBytes(8*dataBytesOffset, dataBytes, 0, size);
	      byte[] ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
	      blocks.add(new BlockPair(dataBytes, ecBytes));

	      maxNumDataBytes = Math.max(maxNumDataBytes, size);
	      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.length);
	      dataBytesOffset += numDataBytesInBlock[0];
	    }
	    if (numDataBytes != dataBytesOffset) {
	      throw new WriterException("Data bytes does not match offset");
	    }

	    BitArray result = new BitArray();

	    // 首先放入数据块
	    for (int i = 0; i < maxNumDataBytes; ++i) {
	      for (BlockPair block : blocks) {
	        byte[] dataBytes = block.getDataBytes();
	        if (i < dataBytes.length) {
	          result.appendBits(dataBytes[i], 8);
	        }
	      }
	    }
	    // 其次，放入错误检测码块
	    for (int i = 0; i < maxNumEcBytes; ++i) {
	      for (BlockPair block : blocks) {
	        byte[] ecBytes = block.getErrorCorrectionBytes();
	        if (i < ecBytes.length) {
	          result.appendBits(ecBytes[i], 8);
	        }
	      }
	    }
	    if (numTotalBytes != result.getSizeInBytes()) {  // Should be same.
	      throw new WriterException("Interleaving error: " + numTotalBytes + " and " +
	          result.getSizeInBytes() + " differ.");
	    }

	    return result;
	  }

	  static byte[] generateECBytes(byte[] dataBytes, int numEcBytesInBlock) {
	    int numDataBytes = dataBytes.length;
	    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
	    for (int i = 0; i < numDataBytes; i++) {
	      toEncode[i] = dataBytes[i] & 0xFF;
	    }
	    new ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256).encode(toEncode, numEcBytesInBlock);

	    byte[] ecBytes = new byte[numEcBytesInBlock];
	    for (int i = 0; i < numEcBytesInBlock; i++) {
	      ecBytes[i] = (byte) toEncode[numDataBytes + i];
	    }
	    return ecBytes;
	  }

	  /**
	   * Append mode info. On success, store the result in "bits".
	   */
	  static void appendModeInfo(Mode mode, BitArray bits) {
	    bits.appendBits(mode.getBits(), 4);
	  }


	  /**
	   * Append length info. On success, store the result in "bits".
	   */
	  static void appendLengthInfo(int numLetters, Version version, Mode mode, BitArray bits) throws WriterException {
	    int numBits = mode.getCharacterCountBits(version);
	    if (numLetters >= (1 << numBits)) {
	      throw new WriterException(numLetters + " is bigger than " + ((1 << numBits) - 1));
	    }
	    bits.appendBits(numLetters, numBits);
	  }

	  /**
	   * 以不同的mode追加bytes，如果成功，则存储结果到bits
	   * 
	   */
	  static void appendBytes(String content,
	                          Mode mode,
	                          BitArray bits,
	                          String encoding) throws WriterException {
	    switch (mode) {
	      case NUMERIC:
	        appendNumericBytes(content, bits);
	        break;
	      case ALPHANUMERIC:
	        appendAlphanumericBytes(content, bits);
	        break;
	      case BYTE:
	        append8BitBytes(content, bits, encoding);
	        break;
	      case KANJI:
	        appendKanjiBytes(content, bits);
	        break;
	      default:
	        throw new WriterException("Invalid mode: " + mode);
	    }
	  }

	  static void appendNumericBytes(CharSequence content, BitArray bits) {
	    int length = content.length();
	    int i = 0;
	    while (i < length) {
	      int num1 = content.charAt(i) - '0';
	      if (i + 2 < length) {
	        // Encode three numeric letters in ten bits.
	        int num2 = content.charAt(i + 1) - '0';
	        int num3 = content.charAt(i + 2) - '0';
	        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
	        i += 3;
	      } else if (i + 1 < length) {
	        // Encode two numeric letters in seven bits.
	        int num2 = content.charAt(i + 1) - '0';
	        bits.appendBits(num1 * 10 + num2, 7);
	        i += 2;
	      } else {
	        // Encode one numeric letter in four bits.
	        bits.appendBits(num1, 4);
	        i++;
	      }
	    }
	  }

	  static void appendAlphanumericBytes(CharSequence content, BitArray bits) throws WriterException {
	    int length = content.length();
	    int i = 0;
	    while (i < length) {
	      int code1 = getAlphanumericCode(content.charAt(i));
	      if (code1 == -1) {
	        throw new WriterException();
	      }
	      if (i + 1 < length) {
	        int code2 = getAlphanumericCode(content.charAt(i + 1));
	        if (code2 == -1) {
	          throw new WriterException();
	        }
	        //编码两个字符进11bits
	        bits.appendBits(code1 * 45 + code2, 11);
	        i += 2;
	      } else {
	    	  //编码一个字符到6bits
	        bits.appendBits(code1, 6);
	        i++;
	      }
	    }
	  }

	  static void append8BitBytes(String content, BitArray bits, String encoding)
	      throws WriterException {
	    byte[] bytes;
	    try {
	      bytes = content.getBytes(encoding);
	    } catch (UnsupportedEncodingException uee) {
	      throw new WriterException(uee);
	    }
	    for (byte b : bytes) {
	      bits.appendBits(b, 8);
	    }
	  }

	  static void appendKanjiBytes(String content, BitArray bits) throws WriterException {
	    byte[] bytes;
	    try {
	      bytes = content.getBytes("Shift_JIS");
	    } catch (UnsupportedEncodingException uee) {
	      throw new WriterException(uee);
	    }
	    int length = bytes.length;
	    for (int i = 0; i < length; i += 2) {
	      int byte1 = bytes[i] & 0xFF;
	      int byte2 = bytes[i + 1] & 0xFF;
	      int code = (byte1 << 8) | byte2;
	      int subtracted = -1;
	      if (code >= 0x8140 && code <= 0x9ffc) {
	        subtracted = code - 0x8140;
	      } else if (code >= 0xe040 && code <= 0xebbf) {
	        subtracted = code - 0xc140;
	      }
	      if (subtracted == -1) {
	        throw new WriterException("Invalid byte sequence");
	      }
	      int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
	      bits.appendBits(encoded, 13);
	    }
	  }

	  private static void appendECI(CharacterSetECI eci, BitArray bits) {
	    bits.appendBits(Mode.ECI.getBits(), 4);
	    bits.appendBits(eci.getValue(), 8);
	  }

}
