package io.milton.cloud.server.video;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Calendar;

import org.apache.commons.io.input.CountingInputStream;

/**
 * Class for parsing the MOI file format, ie JVC cam corder files
 *
 * @author Administrator
 */
public class MoiData {

    public enum TvSystem {

        PAL,
        NTSC,
        Other
    }

    public enum AspectRatio {

        _4_3,
        _16_9,
        Other
    }

    public enum AudioCodec {

        AC3,
        MPEG,
        Other
    }

    public enum VideoBitrate {

        CBR_5_5,
        CBR_8_5,
        Other
    }

    public static MoiData parse(InputStream in) throws ParseException {

        MoiData moi = new MoiData();
        CountingInputStream countIn = new CountingInputStream(in);
        moi.dataIn = new DataInputStream(countIn);
        try {
            moi.readVersion();
            moi.readMoiFilesize();
            moi.readCalendar();
            moi.readVideoDuration();
            moi.dataIn.skip(110);
            moi.readRatioAndSystem();
            moi.readAudioCodec();
            moi.readAudioBitrate();
            moi.dataIn.skip(83);
            moi.readVideoBitrate();
            moi.dataIn.skip(1);
            moi.readVideoDurationPackets();
            moi.dataIn.skip(4);
            moi.dataIn.skip(2);
            moi.dataIn.skip(1);
            moi.dataIn.skip(5);
            moi.dataIn.skip(2);
            moi.readCoarseTableLength();
            moi.readFineTableLength();
            moi.dataIn.skip(12);
            //moi.readCoarseTable();
            //moi.readFineTable();
            return moi;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ParseException(e.getMessage(),
                    countIn.getCount());
        }
    }
    /*
     * The general DataInput contract assures us of big-endian reads
     */
    private DataInputStream dataIn;
    private String version;
    private int moiFilesize;
    private Calendar calendar;
    private int videoDuration;
    private TvSystem tvSystem;
    private AspectRatio aspectRatio;
    private AudioCodec audioCodec;
    private int audioBitrate;
    private VideoBitrate videoBitrate;
    private long videoDurationPackets;
    private int coarseTableLength;
    private int fineTableLength;
    private long[] coarseTable;
    private int[] fineTable;

    private MoiData() {
    }

    private void readVersion() throws IOException {
        byte[] v = new byte[2];
        dataIn.readFully(v);
        version = new String(v, "ASCII");
    }

    public void readMoiFilesize() throws IOException {
        moiFilesize = dataIn.readInt();
    }

    private void readCalendar() throws IOException {
        calendar = Calendar.getInstance();
        int year = dataIn.readUnsignedShort();
        int month = dataIn.readByte();
        int day = dataIn.readByte();
        int hour = dataIn.readByte();
        int minute = dataIn.readByte();
        int millisecs = dataIn.readUnsignedShort();
        calendar.set(year, month, day, hour, minute, millisecs / 1000);
        calendar.set(Calendar.MILLISECOND, millisecs % 1000);
    }

    private void readVideoDuration() throws IOException {
        videoDuration = dataIn.readInt();
    }

    private void readRatioAndSystem() throws IOException {
        int r = dataIn.readInt();
        int highNibble = (r >> 28) & 0x0F;
        int lowNibble = (r >> 24) & 0x0F;

        switch (highNibble) {
            case 4:
                tvSystem = TvSystem.NTSC;
                break;
            case 5:
                tvSystem = TvSystem.PAL;
                break;
            default:
                tvSystem = TvSystem.Other;
        }

        switch (lowNibble) {
            case 0:
            case 1:
                aspectRatio = AspectRatio._4_3;
                break;
            case 4:
            case 5:
                aspectRatio = AspectRatio._16_9;
                break;
            default:
                aspectRatio = AspectRatio.Other;
        }
    }

    private void readAudioCodec() throws IOException {
        int r = dataIn.readUnsignedShort();

        switch (r) {
            case 0x00C1:
                audioCodec = AudioCodec.AC3;
                break;
            case 0x4001:
                audioCodec = AudioCodec.MPEG;
                break;
            default:
                audioCodec = AudioCodec.Other;
        }
    }

    private void readAudioBitrate() throws IOException {
        audioBitrate = dataIn.readByte() * 24 + 64 - 24;
    }

    private void readVideoBitrate() throws IOException {
        int r = dataIn.readUnsignedShort();

        switch (r) {
            case 0x5896:
                videoBitrate = VideoBitrate.CBR_8_5;
                break;
            case 0x813D:
                videoBitrate = VideoBitrate.CBR_5_5;
                break;
            default:
                videoBitrate = VideoBitrate.Other;
        }
    }

    private void readVideoDurationPackets() throws IOException {
        byte[] b = new byte[5];
        dataIn.readFully(b);
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put(b);
        bb.flip();
        videoDurationPackets = bb.asLongBuffer().get(0);
    }

    private void readCoarseTableLength() throws IOException {
        coarseTableLength = dataIn.readUnsignedShort();
    }

    private void readFineTableLength() throws IOException {
        fineTableLength = dataIn.readUnsignedShort();
    }

    private void readCoarseTable() throws IOException {
        if (coarseTableLength <= 0) {
            return;
        }
        coarseTable = new long[coarseTableLength];
        byte[] buffer = new byte[8];
        for (int j = 0; j < coarseTableLength; j++) {

            dataIn.readFully(buffer, 1, 7);
            long entry = ByteBuffer.wrap(buffer).getLong();
            coarseTable[j] = entry;
        }

    }

    private void readFineTable() throws IOException {
        if (fineTableLength <= 0) {
            return;
        }
        fineTable = new int[fineTableLength];
        byte[] buffer = new byte[4];
        for (int j = 0; j < fineTableLength; j++) {

            dataIn.readFully(buffer, 1, 3);
            int entry = ByteBuffer.wrap(buffer).getInt();
            fineTable[j] = entry;
        }
    }

    public String getVersion() {
        return version;
    }

    /**
     * Size in bytes
     *
     * @return
     */
    public int getMoiFilesize() {
        return moiFilesize;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * Duration in milliseconds
     *
     * @return
     */
    public int getVideoDuration() {
        return videoDuration;
    }

    public TvSystem getTvSystem() {
        return tvSystem;
    }

    public AspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public VideoBitrate getVideoBitrate() {
        return videoBitrate;
    }

    public long getVideoBitratePackets() {
        return videoDurationPackets;
    }

    public long[] getCoarseTable() {
        return coarseTable;
    }

    public int[] getFineTable() {
        return fineTable;
    }
//	public static void main(String[] args) throws FileNotFoundException, ParseException {
//		File f = new File("MOV017.MOI");
//		InputStream in = new FileInputStream(f);
//		MoiData moi = MoiData.parse(in);
//		System.out.println(moi.getVersion());
//		System.out.println(moi.getMoiFilesize());
//		System.out.println(moi.getCalendar().getTime());
//		System.out.println(moi.getVideoDuration());
//		System.out.println(moi.getTvSystem());
//		System.out.println(moi.getAspectRatio());
//		System.out.println(moi.getAudioCodec());
//		System.out.println(moi.getAudioBitrate());
//		System.out.println(moi.getVideoBitrate());
//		System.out.println(moi.getVideoBitratePackets());
//		System.out.println(moi.coarseTableLength);
//		System.out.println(moi.fineTableLength);
//		System.out.println(moi.getCoarseTable());
//		System.out.println(moi.getFineTable());
//	}
}
