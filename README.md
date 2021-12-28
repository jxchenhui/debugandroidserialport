# debugandroidserialport
andorid developer can debug serialport applications via cellphone which has no serial device

开发安卓串口应用时，我们经常没有带串口的设备，我们一般是有自己的手机，但手机没有串口设备，所以您可以使用此代码来调试您的应用，只要在调试阶段，加上一行代码，就可以切换为虚拟模式，这样，手机上就会枚举出几个假的串口名称，例如COM1,COM2,COM3,当我们使用接口去打开它、发送数据、接收数据时，我们可以正常的地工作，因为中间将串口的操作映射到了TCP连接了。所以为了正常使用本工具，您还需要在windows电脑端运行bin目录下的exe，这个exe是连接手机与电脑串口的桥梁，手机试图打开COM1时，电脑接收到命令，就会尝试打开电脑上实际的串口（当然您也可以使用一些工具创建一些虚拟串口）

当您的应用开发调试完毕后，我们把那一行代码删除即可，删除后，带有串口的安卓设备枚举串口时就能正确的串口，然后其它操作不需要您任何的修改


SerialPortFinder.setVirtualSerialServer("XXX.XXX.XXX.XXX");

这里的参数设置为电脑端的IP即可，当然您调试的时候，需要保证手机与电脑在同一个局域网下

