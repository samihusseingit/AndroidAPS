package info.nightscout.androidaps.plugins.PumpDanaRS.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.Config;
import com.cozmo.danar.util.BleCommandUtil;

public class DanaRS_Packet_Basal_Set_Basal_Rate extends DanaRS_Packet {
    private static Logger log = LoggerFactory.getLogger(DanaRS_Packet_Basal_Set_Basal_Rate.class);

    private double[] profileBasalRate;
    public int error;

    public DanaRS_Packet_Basal_Set_Basal_Rate() {
        super();
        opCode = BleCommandUtil.DANAR_PACKET__OPCODE_BASAL__SET_BASAL_RATE;
    }

    public DanaRS_Packet_Basal_Set_Basal_Rate(double[] profileBasalRate) {
        this();
        this.profileBasalRate = profileBasalRate;
        if (Config.logDanaMessageDetail) {
            log.debug("Setting new basal rates");
        }
    }

    @Override
    public byte[] getRequestParams() {
        byte[] request = new byte[48];
        for (int i = 0, size = 24; i < size; i++) {
            int rate = (int) (profileBasalRate[i] * 100d);
            request[0 + (i * 2)] = (byte) (rate & 0xff);
            request[1 + (i * 2)] = (byte) ((rate >>> 8) & 0xff);
        }
        return request;
    }

    @Override
    public void handleMessage(byte[] data) {
        int dataIndex = DATA_START;
        int dataSize = 1;
        error = byteArrayToInt(getBytes(data, dataIndex, dataSize));
        if (Config.logDanaMessageDetail) {
            log.debug("Result: " + error);
        }
    }

    @Override
    public String getFriendlyName() {
        return "BASAL__SET_BASAL_RATE";
    }
}
