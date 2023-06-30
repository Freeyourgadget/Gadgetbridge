/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, João
    Paulo Barraca, JohnnySun

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.adablefs;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class AdaBleFsProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile {
    final byte PADDING_BYTE = 0x00;
    final byte REQUEST_CONTINUED = 0x01;
    final byte REQUEST_WRITE_FILE_START = 0x20;
    final byte RESPONSE_WRITE_FILE = 0x21;
    final byte REQUEST_WRITE_FILE_DATA = 0x22;
    final byte REQUEST_DELETE_FILE = 0x30;
    final byte RESPONSE_DELETE_FILE = 0x31;
    final byte REQUEST_MAKE_DIRECTORY = 0x40;
    final byte RESPONSE_MAKE_DIRECTORY = 0x41;
    final byte REQUEST_LIST_DIRECTORY = 0x50;
    final byte RESPONSE_LIST_DIRECTORY = 0x51;
    final byte REQUEST_MOVE_FILE_DIRECTORY = 0x60;
    final byte RESPONSE_MOVE_FILE_DIRECTORY = 0x61;


    final byte STATUS_OK = 0x01;

    static final UUID UUID_SERVICE_FS = UUID.fromString("0000febb-0000-1000-8000-00805f9b34fb");
    static final UUID UUID_CHARACTERISTIC_FS_VERSION = UUID.fromString("adaf0100-4669-6c65-5472-616e73666572");
    static final public UUID UUID_CHARACTERISTIC_FS_TRANSFER = UUID.fromString("adaf0200-4669-6c65-5472-616e73666572");

    private BtLEQueue btleQueue;
    private GBDevice device;

    private LinkedList<AdaBleFsAction> adaBleFsQueue;
    int bytesOfFileWritten;
    int locationToWriteTo;
    int chunkSize;

    private static final Logger LOG = LoggerFactory.getLogger(AdaBleFsProfile.class);

    public AdaBleFsProfile(T support) {
        super(support);
        adaBleFsQueue = new LinkedList<>();
        chunkSize = 20; // Default MTU for android, I believe?
    }

    public void loadResources(Uri uri, Context context, BtLEQueue queue) {
        this.btleQueue = queue;
        // Unzip
        try {
            UriHelper uriHelper = UriHelper.get(uri, context);
            ZipFile zipPackage = new ZipFile(uriHelper.openInputStream());

            JSONObject resources_manifest = new JSONObject(new String(zipPackage.getFileFromZip(("resources.json"))));
            JSONArray resources = resources_manifest.getJSONArray("resources");
            for(int completed = 0; completed < resources.length(); completed++) {
                JSONObject fileItem = resources.getJSONObject(completed);
                adaBleFsQueue.add(
                        new AdaBleFsAction(
                                AdaBleFsAction.Method.UPLOAD,
                                fileItem.getString("path"),
                                zipPackage.getFileFromZip(fileItem.getString("filename"))
                        )
                );
            }
            // TODO Get version, and proper mechanism to compare versions
            // for each obsolete in obsolete_files
            // if obsolete["since"] < this_version ; delete obsolete["path"]
        } catch (ZipFileException e) {
            LOG.error("Unable to read the zip file.", e);
        } catch (FileNotFoundException e) {
            LOG.error("The update file was not found.", e);
        } catch (IOException e) {
            LOG.error("General IO error occurred.", e);
        } catch (Exception e) {
            LOG.error("Unknown error occurred.", e);
        }
        this.startNextAdaFsAction();
    }

    private void startNextAdaFsAction() {
        if (adaBleFsQueue.size() == 0) {
            return;
        }
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        switch (nextAction.method) {
            case UPLOAD:
                uploadFileStart();
                break;
            case DELETE:
                deleteFile();
                break;
        }
    }


    public void enableNotify(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), enable);
    }
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(UUID_CHARACTERISTIC_FS_TRANSFER)) {
                handleNextStatus(gatt, characteristic);
                return true;
            } else {
                LOG.info("Unexpected onCharacteristicRead: " + GattCharacteristic.toString(characteristic));
            }
        } else {
            LOG.warn("error reading from characteristic:" + GattCharacteristic.toString(characteristic));
        }
        return false;
    }

    private void handleNextStatus(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // Determine what we are waiting on
        final byte[] returned = characteristic.getValue();
        // if doing file upload
        boolean handled = false;
        switch (returned[0]) {
            case RESPONSE_WRITE_FILE:
                handled = checkContinueFileUpload(returned);
                break;
            case RESPONSE_DELETE_FILE:
                handled = checkDeleteFile(returned);
                break;
            case RESPONSE_MAKE_DIRECTORY:
                handled = checkMakeDirectory(returned);
                break;
            case RESPONSE_LIST_DIRECTORY:
                // TODO We just throw away the results for now, not sure what to do with them
                handled = checkListDirectory(returned);
                break;
            case RESPONSE_MOVE_FILE_DIRECTORY:
                handled = checkMove(returned);
                break;
        }
        // Read bytes
        // Check for OK
        // queue next event
        return;
    }

    private void checkStatus(byte status) {
        // 0x00 is Error
        // 0x01 / STATUS_OK is OK
        // 0x05 is error modifying read-only FS
        // all others are errors too
        if (status != STATUS_OK) {
            // Raise an exception?
        }

    }

    private boolean checkMove(byte[] returned) {
        byte status = returned[1];

        return true;
    }

    private boolean checkListDirectory(byte[] returned) {
        /**
         * Check that a list directory command worked okay
         *
         * @return true if everything is okay
         */
        // Note that we get one response per entry in the directory.
        byte status = returned[1];
        checkStatus(status);
        int length = returned[2] + (returned[3] >> 8);
        int entryNum = returned[4] + (returned[5] >> 8) + (returned[6] >> 16) + (returned[7] >> 24);
        int totalEntries = returned[8] + (returned[9] >> 8) + (returned[10] >> 16) + (returned[11] >> 24);
        // returned[12:15] = flags, but only bit 0 matters
        int flags = returned[12] + (returned[13] >> 8) + (returned[14] >> 16) + (returned[15] >> 24);
        boolean isDirectory = (flags & 1) == 1;
        long timeStamp = returned[16] + (returned[17] >> 8) + (returned[18] >> 16) + (returned[19] >> 24) +
                (returned[20] >> 32) + (returned[21] >> 40) + (returned[22] >> 48) + (returned[23] >> 56);
        int fileSize = returned[24] + (returned[25] >> 8) + (returned[26] >> 16) + (returned[27] >> 24);
        // Rest is the path, relative to the path we requested
        byte[] stringBytes = new byte[length+1];
        for(int counter = 0; counter < length; counter++) {
            stringBytes[counter] = returned[28+counter];
        }
        // Just throwing away this path?
        try {
            String path = new String(stringBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Pretty sure this branch will never happen
        }
        return entryNum == totalEntries;
    }

    private boolean checkMakeDirectory(byte[] returned) {
        /**
         * Check that a make directory command worked okay
         *
         * @return true if everything is okay
         */
        byte status = returned[1];
        checkStatus(status);
        // returned[2:7] are padding
        // returned[8:15] are a unix timestamp of the directory
        return true;
    }

    private boolean checkDeleteFile(byte[] returned) {
        /**
         * Check that a delete command worked okay
         *
         * @return true if everything is okay
         */
        byte status = returned[1];
        checkStatus(status);
        // Not sure what to do here
        return true;
    }

    private boolean checkContinueFileUpload(byte[] returned) {
        /**
         * Check that a file upload command has completed
         *
         * @return true if everything is okay
         */
        byte status = returned[1];
        checkStatus(status);
        // returned[2:3] are padding
        locationToWriteTo = returned[4] + (returned[5] << 8) + (returned[6] << 16) + (returned[7] << 24);
        // returned[8:15] is uint64 encoding unix timestamp
        int sizeLeft = returned[16] + (returned[17] <<8) + (returned[18] << 16) + (returned[19] << 24);
        if (sizeLeft > 0) {
            uploadNextFileChunk();
        }
        return (sizeLeft == 0);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return onCharacteristicRead(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);
    }

    private void uploadNextFileChunk() {
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        final int toSendSize = Math.min(chunkSize, nextAction.data.length - bytesOfFileWritten);
        ArrayList<Byte> command = new ArrayList<>();
        command.add(REQUEST_WRITE_FILE_DATA);
        command.add(REQUEST_CONTINUED);
        command.add(PADDING_BYTE);
        command.add(PADDING_BYTE);
        command.add((byte) (locationToWriteTo & 0xFF)); // 32-bit offset
        command.add((byte) ((locationToWriteTo >> 8) & 0xFF));
        command.add((byte) ((locationToWriteTo >> 16) & 0xFF));
        command.add((byte) ((locationToWriteTo >> 24) & 0xFF));
        command.add((byte) (toSendSize & 0xFF)); // File size as 32-bit
        command.add((byte) ((toSendSize >> 8) & 0xFF));
        command.add((byte) ((toSendSize >> 16) & 0xFF));
        command.add((byte) ((toSendSize >> 24) & 0xFF));
        for(int counter = bytesOfFileWritten; counter < toSendSize; counter++) {
            command.add(nextAction.data[counter]);
        }
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("Upload file chunk");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
    }

    private void uploadFileStart() {
        bytesOfFileWritten = 0;
        // TODO Should this be the time we upload, or should it somehow be the timestamp of the
        // resources.zip archive file
        long unixTime = System.currentTimeMillis() / 1000L;
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        Vector<Byte> command = new Vector<>();
        command.add(REQUEST_WRITE_FILE_START);
        command.add(PADDING_BYTE);
        command.add((byte) (nextAction.filenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.filenameorpath.length() >> 8) & 0xFF));
        command.add((byte) 0x00); // 32-bit offset is 0 for file start
        command.add((byte) 0x00);
        command.add((byte) 0x00);
        command.add((byte) 0x00);
        command.add((byte) (unixTime & 0xFF)); // Timestamp as 64-bit
        command.add((byte) ((unixTime >> 8) & 0xFF));
        command.add((byte) ((unixTime >> 16) & 0xFF));
        command.add((byte) ((unixTime >> 24) & 0xFF));
        command.add((byte) ((unixTime >> 32) & 0xFF));
        command.add((byte) ((unixTime >> 40) & 0xFF));
        command.add((byte) ((unixTime >> 48) & 0xFF));
        command.add((byte) ((unixTime >> 56) & 0xFF));
        command.add((byte) (nextAction.data.length & 0xFF)); // File size as 32-bit
        command.add((byte) ((nextAction.data.length >> 8) & 0xFF));
        command.add((byte) ((nextAction.data.length >> 16) & 0xFF));
        command.add((byte) ((nextAction.data.length >> 24) & 0xFF));
        // Is there a better way to construct a bytes[] ?
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("Upload file start");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
        return;
    }

    private void requestListDirectory() {
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        Vector<Byte> command = new Vector<Byte>();
        command.add(REQUEST_LIST_DIRECTORY);
        command.add(PADDING_BYTE);
        command.add((byte) (nextAction.filenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.filenameorpath.length() >> 8) & 0xFF));
        for(int count = 0; count < nextAction.filenameorpath.length(); count++) {
            command.add((byte) nextAction.filenameorpath.charAt(count));
        }
        // send command, wait for response
        // Is there a better way to construct a bytes[] ?
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("List directory");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
        return;
    }
    private void deleteFile() {
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        Vector<Byte> command = new Vector<Byte>();
        command.add(REQUEST_DELETE_FILE);
        command.add(PADDING_BYTE);
        command.add((byte) (nextAction.filenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.filenameorpath.length() >> 8) & 0xFF));
        for(int count = 0; count < nextAction.filenameorpath.length(); count++) {
            command.add((byte) nextAction.filenameorpath.charAt(count));
        }
        // send command, wait for response
        // Is there a better way to construct a bytes[] ?
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("Delete file or directory");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
        return;
    }

    private void makeDirectory() {
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        long unixTime = System.currentTimeMillis() / 1000L;
        Vector<Byte> command = new Vector<Byte>();
        command.add(REQUEST_MAKE_DIRECTORY);
        command.add(PADDING_BYTE);
        command.add((byte) (nextAction.filenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.filenameorpath.length() >> 8) & 0xFF));
        command.add(PADDING_BYTE);
        command.add(PADDING_BYTE);
        command.add(PADDING_BYTE);
        command.add(PADDING_BYTE);
        command.add((byte) (unixTime & 0xFF)); // Timestamp as 64-bit
        command.add((byte) ((unixTime >> 8) & 0xFF));
        command.add((byte) ((unixTime >> 16) & 0xFF));
        command.add((byte) ((unixTime >> 24) & 0xFF));
        command.add((byte) ((unixTime >> 32) & 0xFF));
        command.add((byte) ((unixTime >> 40) & 0xFF));
        command.add((byte) ((unixTime >> 48) & 0xFF));
        command.add((byte) ((unixTime >> 56) & 0xFF));
        for(int count = 0; count < nextAction.filenameorpath.length(); count++) {
            command.add((byte) nextAction.filenameorpath.charAt(count));
        }
        // send command, wait for response
        // Is there a better way to construct a bytes[] ?
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("Create directory");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
        return;
    }

    private void moveFileOrDirectory() {
        final AdaBleFsAction nextAction = adaBleFsQueue.getFirst();
        long unixTime = System.currentTimeMillis() / 1000L;
        Vector<Byte> command = new Vector<Byte>();
        command.add(REQUEST_MOVE_FILE_DIRECTORY);
        command.add(PADDING_BYTE);
        command.add((byte) (nextAction.filenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.filenameorpath.length() >> 8) & 0xFF));
        command.add((byte) (nextAction.secondFilenameorpath.length() & 0xFF)); // 16-bit path length
        command.add((byte) ((nextAction.secondFilenameorpath.length() >> 8) & 0xFF));
        for(int count = 0; count < nextAction.filenameorpath.length(); count++) {
            command.add((byte) nextAction.filenameorpath.charAt(count));
        }
        command.add(PADDING_BYTE);
        for(int count = 0; count < nextAction.secondFilenameorpath.length(); count++) {
            command.add((byte) nextAction.secondFilenameorpath.charAt(count));
        }
        // send command, wait for response
        // Is there a better way to construct a bytes[] ?
        byte[] bytes = new byte[command.size()];
        for(int i = 0; i < command.size(); i++) {
            bytes[i] = command.get(i);
        }
        TransactionBuilder builder = new TransactionBuilder("Move file or directory");
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER), bytes);
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_FS_TRANSFER));
        builder.queue(getQueue());
        return;
    }

}
