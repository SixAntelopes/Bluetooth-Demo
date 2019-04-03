# Bluetooth-Demo

Android app use Bluetooth to send message.

## 简单的讲解
因readme没什么东西，所以就顺便写在此处。本教程试图讲解此程序所实现的蓝牙（非低功耗蓝牙）功能。其实 [官方文档](https://developer.android.com/guide/topics/connectivity/bluetooth/ "Bluetooth") 介绍得比我好多了。

1. 声明一个 `BluetoothAdapter` 类型的变量，来接收 `BluetoothAdapter.getDefaultAdapter()` ，如果蓝牙没有打开或设备没有蓝牙，则 `mBluetoothAdapter.isEnabled()` 为 `false` ，因此可以用来判断蓝牙是否打开。
2. 询问想连接的设备已在配对设备列表里，因求精简而没有“搜索设备”这个功能，所以用户在第一次使用时需要在系统设置里搜索并配对设备，配对后设备信息会被存储在配对列表，下次若想连接此设备则不用搜索直接在配对列表中获取，调用 `BluetoothAdapter.getBondedDevices()` 获取配对列表，这里用 `set` 来存放，对于每个设备，类型是 `BluetoothDevice` 。在 `ArrayAdapter` 存放其名字和地址以显示出来。
3. 被选中的设备，会记录下地址，于是便可以建立连接了。
4. 建立连接，在 [官方文档](https://developer.android.com/guide/topics/connectivity/bluetooth#ConnectingDevices "ConnectingDevices") 中是这样说的：
> 要在两台设备上的应用之间创建连接，必须同时实现服务器端和客户端机制，因为其中一台设备必须开放服务器套接字，而另一台设备必须发起连接（使用服务器设备的 MAC 地址发起连接）。 当服务器和客户端在同一 RFCOMM 通道上分别拥有已连接的 BluetoothSocket 时，二者将被视为彼此连接。 这种情况下，每台设备都能获得输入和输出流式传输，并且可以开始传输数据，在有关管理连接的部分将会讨论这一主题。 
6. 此程序的蓝牙是连接为客户端，这里创建了一个线程，调用 `BluetoothAdapter.getRemoteDevice(地址)` 获得 `BluetoothDevice` ，调用 `BluetoothDevice.createRfcommSocketToServiceRecord(UUID)` 获得 `BluetoothSocket` (蓝牙套接字接口)。这是发起连接必须的，在 [官方文档](https://developer.android.com/guide/topics/connectivity/bluetooth#ConnectingAsAClient "ConnectingAsAClient") 是这样说的：
> 要发起与远程设备（保持开放的服务器套接字的设备）的连接，必须首先获取表示该远程设备的 BluetoothDevice 对象。（在前面有关查找设备的部分介绍了如何获取 BluetoothDevice）。 然后必须使用 BluetoothDevice 来获取 BluetoothSocket 并发起连接。
7. 调用 `BluetoothSocket.connect()` 就可以发起连接了。若连接上，则可以传输数据了。此程序只实现了发送数据而没有实现收取数据。
8. 这里再次创建一个线程，使用 `Thread.start()` 调用而不是 `Thread.run()` 。获取 `BluetoothSocket.getOutputStream()` 给 `OutputStream` ，于是把要发送的数据转换为 `byte` 类型（如 `String.getBytes()` ）就可以调用 `OutputStream.write()` 发送出去了。

以上。

