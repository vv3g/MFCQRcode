# MFCQRcode #
## 简介 ##
  一个生成QRcode二维码的MFC项目。核心生成二维码采用C语言实现，开发环境vs2010，解决方案中包括以下两个项目。
- **LibQRCode**
	一个vs2010静态库项目，包括QRCode生成的核心代码实现，采用C语言完成，参考至项目[libqrencode](https://github.com/fukuchi/libqrencode)。该项目可生成一个lib库供调用。
- **QRCode**
	一个MFC项目，通过引用上面LibQRCode项目生成的lib来生成二维码。为了方便使用已经已经将生成好的lib已经包含在debug文件夹LibQRCode.lib文件即使，因此此项目在使用过程中并不依赖第一个项目，第一个项目供研究实现原理使用。
测试程序截图如下：
![](http://i.imgur.com/F9nZxLP.png)


## 使用 ##
### 编译 ###
直接采用vs2010打开.sln文件即可打开解决方法。如果要单独尝试编译第一个静态库lib，需要在该项目中。属性--> c/c++-->预处理器中定义HAVE_CONFIG_H。如下图![](http://i.imgur.com/z9EueLv.png)
### 事例代码###
使用lib中的C方法进行生成QRcode
- 首先需要添加头文件[qrencode.h](https://github.com/elicec/MFCQRcode/blob/master/QRCode/QRCode/src/qrencode.h)
- 然后即可调用到lib中C语言实现的encode方法
- example
	    QRcode*	pQRC = QRcode_encodeString(szSourceString, 0, QR_ECLEVEL_L, QR_MODE_8, 1)

参数中定义了
- 需要编码的字符串*szSourceString*
- QRcode的版本version 0
- 纠错级别 *QR_ECLEVEL_L*([qrencode.h](https://github.com/elicec/MFCQRcode/blob/master/QRCode/QRCode/src/qrencode.h)中有定义)
- 编码模式 *QR_MODE_8*（同样定义在[qrencode.h](https://github.com/elicec/MFCQRcode/blob/master/QRCode/QRCode/src/qrencode.h)）

    


## 关于 ##
Email：elicec@foxmail.com
任何问题欢迎交流。