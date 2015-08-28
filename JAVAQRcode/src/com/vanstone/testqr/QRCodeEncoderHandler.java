package com.vanstone.testqr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import com.vanstone.encoder.BarcodeFormat;
import com.vanstone.encoder.BitMatrix;
import com.vanstone.encoder.EncodeHintType;
import com.vanstone.encoder.Encoder;

public class QRCodeEncoderHandler extends JFrame implements ActionListener {

	private JButton btnGenrate;
	private JLabel pic;
	private JTextField edittext;
	private static QRCodeEncoderHandler handler;
	private static String imgPath = "D:/test/Michael_QRCode.png";

	public QRCodeEncoderHandler() {
		super();
		this.setSize(400, 400);
		this.getContentPane().setLayout(null);
		this.add(getJLabel(), null);
		this.add(getJTextField(), null);
		this.add(getJButton(), null);
		this.setTitle("QRCode");
		setResizable(false);
	}

	private JButton getJButton() {
		if (btnGenrate == null) {
			btnGenrate = new JButton();
			btnGenrate.setBounds(260, 300, 71, 27);
			btnGenrate.setText("生成");
			btnGenrate.setMnemonic(KeyEvent.VK_G);
			btnGenrate.addActionListener(this);

		}
		return btnGenrate;
	}

	private JTextField getJTextField() {
		if (edittext == null) {
			edittext = new JTextField();
			edittext.setBounds(40, 300, 200, 27);
		}

		return edittext;
	}

	private JLabel getJLabel() {
		if (pic == null) {
			pic = new JLabel();
			pic.setBounds(60, 60, 200, 200);
		}
		return pic;
	}

	public void encoderQRCode(String content, String imgPath) {

		try {
			byte[] contentBytes = content.getBytes("gb2312");
			// 输出内容> 二维码
			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
			hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
			BitMatrix bitMatrix = new Encoder().encode(content,
					BarcodeFormat.QR_CODE, 200, 200, hints);

			if (contentBytes.length > 0 && contentBytes.length < 120) {

				int[] pixels = new int[200 * 200];

				for (int y = 0; y < 200; y++) {
					for (int x = 0; x < 200; x++) {
						if (bitMatrix.get(x, y)) {
							pixels[y * 200 + x] = 0xff000000;
						} else {
							pixels[y * 200 + x] = 0xffffffff;
						}

					}
				}

				writeImageFromArray(imgPath, "jpg", pixels, 200, 200);
				// pic.set

			} else {

				System.err.println("QRCode content bytes length = "

				+ contentBytes.length + " not in [ 0,120 ]. ");

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public static void writeImageFromArray(String imageFile, String type,
			int[] data, int width, int height) {

		BufferedImage bf = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_BGR);

		bf.setRGB(0, 0, width, height, data, 0, width);

		// 输出图片

		try {

			File file = new File(imageFile);

			ImageIO.write((RenderedImage) bf, type, file);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		handler = new QRCodeEncoderHandler();
		handler.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (edittext.getText().length() > 0) {
			handler.encoderQRCode(edittext.getText(), imgPath);
			System.out.println("encoder QRcode success");
			try {
				pic.setIcon(new ImageIcon(ImageIO.read(new File(imgPath))));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

}
