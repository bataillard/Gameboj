package ch.epfl.gameboj.bits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class BitVectorTest {
    
    private int DEFAULT_SIZE = 256;    
    
    @Test
    void constructorsAreCompatible() {
        BitVector v1 = new BitVector(DEFAULT_SIZE);
        BitVector v2 = new BitVector(DEFAULT_SIZE, false);
        
        assertTrue(v1.equals(v2));
    }
    
    @Test
    void constructorThrowsForNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new BitVector(-1));
    }
    
    @Test
    void constructorThrowsForInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new BitVector(1));
    }
    
    @Test
    void sizeWorks() {
        assertEquals(DEFAULT_SIZE, new BitVector(DEFAULT_SIZE).size());
        assertEquals(32, new BitVector(32).size());
    }
    
    @Test 
    void notWorksOnKnownValues() {
        BitVector v1 = new BitVector(DEFAULT_SIZE, true);
        BitVector v2 = new BitVector(DEFAULT_SIZE, false);
        BitVector v3 = new BitVector.Builder(32).setByte(2, (byte) 0xFF).build();
        
        assertTrue(v1.not().equals(v2));
        assertTrue(v2.not().equals(v1));
        assertEquals("11111111000000001111111111111111", v3.not().toString());
    }
    
    @Test
    void andWorksOnKnownValues() {
        BitVector v1 = new BitVector(DEFAULT_SIZE, true);
        BitVector v2 = new BitVector.Builder(DEFAULT_SIZE).setByte(1, (byte) 0xFF).build();
        
        int[] check = {0, 0, 0, 0 ,0, 0, 0, 0xFF00};
        assertEquals(integerArrayToBinaryString(check), v1.and(v2).toString());
    }
    
    @Test
    void andThrowsForInvalidVector() {
        BitVector v1 = new BitVector(DEFAULT_SIZE, true);
        BitVector v2 = new BitVector(32, true);
        
        assertThrows(IllegalArgumentException.class, () -> v1.and(v2));
    }
    
    @Test
    void orWorksOnKnownValues() {
        BitVector v1 = new BitVector(DEFAULT_SIZE, false);
        BitVector v2 = new BitVector.Builder(DEFAULT_SIZE).setByte(1, (byte) 0xFF).setByte(3, (byte) 0xDD).build();
        
        int[] check = {0, 0, 0, 0 ,0, 0, 0, 0xDD00FF00};
        assertEquals(integerArrayToBinaryString(check), v1.or(v2).toString());
    }
    
    @Test
    void orThrowsForInvalidVector() {
        BitVector v1 = new BitVector(DEFAULT_SIZE, true);
        BitVector v2 = new BitVector(32, true);
        
        assertThrows(IllegalArgumentException.class, () -> v1.or(v2));
    }
    
    @Test
    void extractWorkWithinVector() {
        int[] bytes = {0xE1B8_5117, 0x0D01_8ECA, 0x3A03_9EB0, 0xDF72_D16A};
        BitVector bv = fill(bytes);

        for (int i = 0; i < bv.size(); i++) {
            for (int size = 32; size <= bv.size() - i; size += 32) {
                String reversed = new StringBuilder(integerArrayToBinaryString(bytes)).reverse().substring(i, i+size);
                String sub = new StringBuilder(reversed).reverse().toString();
                                
                assertEquals(sub, bv.extractZeroExtended(i, size).toString()); 
                assertEquals(sub, bv.extractWrapped(i, size).toString());
            }
        }
    }
    
    @Test
    void extractWrappedExtendsCorrectly() {
        int[] bytes = {0xE1B8_5117, 0x0D01_8ECA, 0x3A03_9EB0, 0xDF72_D16A};
        BitVector bv = fill(bytes);                        
        String[] wraps = {
                hexToBin("DF72D16A"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("0D018ECA3A039EB0DF72D16A"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("3A039EB0DF72D16A"+"E1B851170D018ECA3A039EB0DF72D16A"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("E1B851170D018ECA3A039EB0DF72D16A"+"E1B85117"),
                hexToBin("70DC288B8680C7651D01CF586FB968B570DC288B"),
                hexToBin("0D018ECA3A039EB0DF72D16A"+"E1B851170D018ECA3A039EB0DF72D16A"+"E1B851170D018ECA3A039EB0"),
                hexToBin("E1B851170D018ECA3A039EB0")
            };
                
        assertEquals(wraps[0], bv.extractWrapped(0, 160).toString()); 
        assertEquals(wraps[1], bv.extractWrapped(0, 224).toString());
        assertEquals(wraps[2], bv.extractWrapped(0, 320).toString());
        assertEquals(wraps[3], bv.extractWrapped(-32, 160).toString());
        assertEquals(wraps[4], bv.extractWrapped(-31, 160).toString());
        assertEquals(wraps[5], bv.extractWrapped(-96, 320).toString());
        assertEquals(wraps[6], bv.extractWrapped(288, 96).toString());
    }
    
    @Test
    void extractZeroExtendedWorks() {
        int[] bytes = {0xE1B8_5117, 0x0D01_8ECA, 0x3A03_9EB0, 0xDF72_D16A};
        BitVector bv = fill(bytes);                        
        String[] wraps = {
                hexToBin("00000000"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("000000000000000000000000"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("0000000000000000"+"00000000000000000000000000000000"+"E1B851170D018ECA3A039EB0DF72D16A"),
                hexToBin("E1B851170D018ECA3A039EB0DF72D16A"+"00000000"),
                hexToBin("70DC288B8680C7651D01CF586FB968B500000000"),
                hexToBin("000000000000000000000000"+"E1B851170D018ECA3A039EB0DF72D16A"+"000000000000000000000000"),
                hexToBin("000000000000000000000000")
            };
                
        assertEquals(wraps[0], bv.extractZeroExtended(0, 160).toString()); 
        assertEquals(wraps[1], bv.extractZeroExtended(0, 224).toString());
        assertEquals(wraps[2], bv.extractZeroExtended(0, 320).toString());
        assertEquals(wraps[3], bv.extractZeroExtended(-32, 160).toString());
        assertEquals(wraps[4], bv.extractZeroExtended(-31, 160).toString());
        assertEquals(wraps[5], bv.extractZeroExtended(-96, 320).toString());
        assertEquals(wraps[6], bv.extractZeroExtended(288, 96).toString());
    }
    
    @Test
    void extractCombinesOutOfRangeCorrectly() {
        int[] bytes = {0xFC16_AFEE};
        BitVector bv = fill(bytes);                        
        String[] wraps = {
                "110"+"11111100000101101010111111101",
                "000"+"11111100000101101010111111101",
                "1011101111110000010110101011111110111011111100000101101010111111",
            };
                
        assertEquals(wraps[0], bv.extractWrapped(3, 32).toString()); 
        assertEquals(wraps[1], bv.extractZeroExtended(3, 32).toString());
        assertEquals(wraps[2], bv.extractWrapped(-26, 64).toString());
    }


    public static String integerArrayToBinaryString(int[] ints) {
        StringBuilder sb = new StringBuilder();
        
        for(int i : ints) {
            sb.append(String.format("%32s", Integer.toBinaryString(i)).replace(' ', '0'));
        }
        
        return sb.toString();
    }
    
    private BitVector fill(int[] blocks) {
        BitVector.Builder bvb = new BitVector.Builder(32*blocks.length);
        
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < 4; j++) {
                byte b = (byte)((blocks[i] >> 8*j) & 0xFF);
                bvb.setByte(4*(blocks.length - i - 1) + j, b);
            }
        }
        
        return bvb.build();
    }
    
    private String hexToBin(String hex) { 
        return String.format("%" + hex.length()*4 + "s", new BigInteger(hex, 16).toString(2)).replace(' ', '0');
    }
}
