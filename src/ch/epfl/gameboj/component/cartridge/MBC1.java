package ch.epfl.gameboj.component.cartridge;

import static ch.epfl.gameboj.Preconditions.checkBits16;
import static ch.epfl.gameboj.Preconditions.checkBits8;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.Rom;

public final class MBC1 extends MBC {
    private static final int RAM_ENABLE = 0xA;

    private enum Mode { MODE_0, MODE_1 };

    private final Rom rom;
    private final Ram ram;

    private boolean ramEnabled;
    private Mode mode;
    private int romLsb5, ramRom2;
    private final int romMask, ramMask;

    /**
     * 
     * @param rom
     * @param ramSize
     */
    public MBC1(Rom rom, int ramSize) {
        super(ramSize);
        this.rom = rom;
        this.ram = new Ram(ramSize);

        this.ramEnabled = false;
        this.mode = Mode.MODE_0;
        this.romLsb5 = 1;
        this.ramRom2 = 0;

        this.romMask = rom.size() - 1;
        this.ramMask = ramSize - 1;        
    }
    
    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {
        switch (Bits.extract(checkBits16(address), 13, 3)) {
        case 0: case 1:
            return rom.read(romAddress(msb2(), 0, address));
        case 2: case 3:
            return rom.read(romAddress(ramRom2, romLsb5, address));
        case 5:
            return ramEnabled ? ram.read(ramAddress(address)) : 0xFF;
        default:
            return NO_DATA;
        }
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        checkBits8(data);
        switch (Bits.extract(checkBits16(address), 13, 3)) {
        case 0:
            ramEnabled = Bits.clip(4, data) == RAM_ENABLE;
            break;
        case 1:
            romLsb5 = Math.max(1, Bits.clip(5, data));
            break;
        case 2:
            ramRom2 = Bits.clip(2, data);
            break;
        case 3:
            mode = Bits.test(data, 0) ? Mode.MODE_1 : Mode.MODE_0;
            break;
        case 5:
            if (ramEnabled)
                ram.write(ramAddress(address), data);
            break;
        }
    }
    
    /**
     * @return a byte array with contents of whole ram
     */
    public byte[] readWholeRam() {
        byte[] ramData = new byte[ram.size()];
        
        for (int i = 0; i < ram.size(); i++) {
            ramData[i] = (byte) ram.read(i);
        }
        
        return ramData;
    }
    
    /**
     * Writes given byte array to whole ram
     * @param data
     * @throws IllegalArgumentException if array size is strictly larger than ram size
     */
    public void writeWholeRam(byte[] data) {
        Preconditions.checkArgument(data.length <= ram.size());
        
        for (int i = 0; i < data.length; i++) {
            ram.write(i, Byte.toUnsignedInt(data[i]));
        }
    }

    private int msb2() {
        switch (mode) {
		case MODE_0: return 0;
		case MODE_1: return ramRom2;
		default: throw new Error();
        }
    }

    private int romAddress(int b_20_19, int b_18_14, int b_13_0) {
        return ((b_20_19 << 19) | (b_18_14 << 14) | Bits.clip(14, b_13_0)) & romMask;
    }

    private int ramAddress(int b_12_0) {
        return ((msb2() << 13) | Bits.clip(13, b_12_0)) & ramMask;
    }
}
