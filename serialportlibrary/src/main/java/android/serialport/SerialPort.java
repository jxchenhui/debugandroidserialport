/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.serialport;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class SerialPort {

    private static final String TAG = "SerialPort";

    public static final String DEFAULT_SU_PATH = "/system/bin/su";

    private static String sSuPath = DEFAULT_SU_PATH;

    private boolean isvirtual = false;

    /**
     * Set the su binary path, the default su binary path is {@link #DEFAULT_SU_PATH}
     *
     * @param suPath su binary path
     */
    public static void setSuPath( String suPath) {
        if (suPath == null) {
            return;
        }
        sSuPath = suPath;
    }

    /**
     * Get the su binary path
     *
     * @return
     */

    public static String getSuPath() {
        return sSuPath;
    }

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private Socket mSocket;
    private OutputStream os;
    private InputStream is;

    public SerialPort(String path, int baudrate,int dataBits,int parity,int stopBits,int flags) throws IOException{
        if(path.toLowerCase().startsWith("com")){
            isvirtual = true;
            //????????????????????????
            InetAddress ipAddress = InetAddress.getByName(SerialPortFinder.getVirtualSerialServer());
            mSocket = new Socket(ipAddress, 8090);
            if(mSocket != null){
                os = mSocket.getOutputStream();
                is = mSocket.getInputStream();

                long starttime = System.currentTimeMillis();
                String hello = path + "," + baudrate + "," + parity + "," + dataBits + "," + stopBits + "\n";
                os.write(hello.getBytes(Charset.forName("utf8")));
                os.flush();
                while (System.currentTimeMillis() - starttime < 3000){
                    if (is.available() > 0){
                        byte b = (byte)is.read();
                        if(b == 0x00){
                            //?????????
                            break;
                        }else{
                            //?????????
                            return;
                        }
                    }else{
                        SystemClock.sleep(10);
                    }
                }
                closeTcp();
                throw new IOException();
            }else{
                throw new IOException();
           }
        }else{
            isvirtual = false;
            mFd = open(path, baudrate, dataBits, parity, stopBits, flags);
            if (mFd == null) {
                Log.e(TAG, "native open returns null");
                throw new IOException();
            }
            mFileInputStream = new FileInputStream(mFd);
            mFileOutputStream = new FileOutputStream(mFd);
        }

    }

    private void closeTcp(){
        if(os != null){
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            os = null;
        }
        if(is != null){
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            is = null;
        }
        if(mSocket != null){
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }
    }
    /**
     * ??????
     *
     * @param device ??????????????????
     * @param baudrate ?????????
     * @param dataBits ??????????????????8,????????????5~8
     * @param parity ???????????????0:????????????(NONE?????????)???1:????????????(ODD);2:????????????(EVEN)
     * @param stopBits ??????????????????1???1:1???????????????2:2????????????
     * @param flags ??????0
     * @throws SecurityException
     * @throws IOException
     */
    private SerialPort( File device, int baudrate, int dataBits, int parity, int stopBits,
        int flags) throws SecurityException, IOException {


        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec(sSuPath);
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, dataBits, parity, stopBits, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    /**
     * ??????????????????8n1
     *
     * @param device ??????????????????
     * @param baudrate ?????????
     * @throws SecurityException
     * @throws IOException
     */
    public SerialPort( File device, int baudrate) throws SecurityException, IOException {
        this(device, baudrate, 8, 0, 1, 0);
    }

    /**
     * ??????
     *
     * @param device ??????????????????
     * @param baudrate ?????????
     * @param dataBits ??????????????????8,????????????5~8
     * @param parity ???????????????0:????????????(NONE?????????)???1:????????????(ODD);2:????????????(EVEN)
     * @param stopBits ??????????????????1???1:1???????????????2:2????????????
     * @throws SecurityException
     * @throws IOException
     */
    public SerialPort( File device, int baudrate, int dataBits, int parity, int stopBits)
        throws SecurityException, IOException {
        this(device, baudrate, dataBits, parity, stopBits, 0);
    }

    // Getters and setters

    public InputStream getInputStream() {
        if(isvirtual){
            return is;
        }
        return mFileInputStream;
    }


    public OutputStream getOutputStream() {
        if(isvirtual){
            return os;
        }
        return mFileOutputStream;
    }

    // JNI
    private native FileDescriptor open(String absolutePath, int baudrate, int dataBits, int parity,
        int stopBits, int flags);

    public native void close();

    public native void tcflush();


    /** ???????????????????????????try-catch */
    public void tryClose() {
        if(isvirtual){
            closeTcp();
        }else{
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }

            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }

            try {
                close();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

    }

    static {
        System.loadLibrary("serial_port");
    }
}
