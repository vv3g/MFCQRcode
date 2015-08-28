package com.vanstone.encoder;

import java.util.Map;

public class Encoder {
	 private static final int QUIET_ZONE_SIZE = 4;

	public BitMatrix encode(String contents, BarcodeFormat format, int width,
			int height, Map<EncodeHintType, ?> hints) throws WriterException {

		if (contents.isEmpty()) {
			throw new IllegalArgumentException("内容为空");
		}

		if (format != BarcodeFormat.QR_CODE) {
			throw new IllegalArgumentException(
					"只能编码QR，当前格式为： " + format);
		}

		if (width < 0 || height < 0) {
			throw new IllegalArgumentException(
					"输出尺寸错误: " + width + 'x'
							+ height);
		}

		ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
		int quietZone = QUIET_ZONE_SIZE;
		if (hints != null) {
			ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints
					.get(EncodeHintType.ERROR_CORRECTION);
			if (requestedECLevel != null) {
				errorCorrectionLevel = requestedECLevel;
			}
			Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
			if (quietZoneInt != null) {
				quietZone = quietZoneInt;
			}
		}

		QRCode code=QREncoder.encode(contents, errorCorrectionLevel, hints);
		return renderResult(code, width, height, quietZone);
	}

	
	/**
	 * 返回一个编码后二维码的BitMatrix,在输入矩阵中，使用0代表白色，1代表黑色。而在输出矩阵中
	 * 使用0代表黑色，使用255代表白色（一个8bit灰度级的bitmap）
	 * 
	 * @param code 输入的QRCode
	 * @param width 宽度
	 * @param height 高度
	 * @param quietZone 图像与四周的边距
	 * @return 编码后的BitMatrix
	 */
	private static BitMatrix renderResult(QRCode code, int width, int height,
			int quietZone) {
		ByteMatrix input = code.getMatrix();
		if (input == null) {
			throw new IllegalStateException("二维码为null");
		}
		int inputWidth = input.getWidth();
		int inputHeight = input.getHeight();
		int qrWidth = inputWidth + (quietZone * 2);
		int qrHeight = inputHeight + (quietZone * 2);
		int outputWidth = Math.max(width, qrWidth);
		int outputHeight = Math.max(height, qrHeight);

		int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
		//二维码空白部分的填补包括两部分，一部分是设置的四周页边距，另一部分是额外的
		//空白像素(为了适应请求输出图像的大小)，例如输入25*25的二维码，加上四周页边距（4）
		//后的实际大小为33*33。这时，如果请求输出一个200*160大小的图片时，此时的扩大倍数
		//应该为160/33=4,因此须将从100*100（实际的QR）填补到200*160.
		int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
		int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

		BitMatrix output = new BitMatrix(outputWidth, outputHeight);

		for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
			// Write the contents of this row of the barcode
			for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
				if (input.get(inputX, inputY) == 1) {
					output.setRegion(outputX, outputY, multiple, multiple);
				}
			}
		}

		return output;
	}
}
