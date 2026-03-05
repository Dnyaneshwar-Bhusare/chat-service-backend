package com.codingworld.service1;

/**
 * Standalone Java program — paste into any Java environment and run main().
 * Produces the correct paris-compatible binary and prints it.
 */
public class BinaryTranslator {

    static final String ORIGINAL =
        "6080604052348015600e575f80fd5b506103968061001c5f395ff3fe" +
        "608060405234801561000f575f80fd5b506004361061003f575f3560e01c" +
        "80637269367b1461004357806385f09f1214610073578063ef020f4a1461" +
        "008f575b5f80fd5b61005d600480360381019061005891906101f4565b61" +
        "00bf565b60405161006a9190610237565b60405180910390f35b61008d60" +
        "0480360381019061008891906102aa565b6100d3565b005b6100a9600480" +
        "36038101906100a491906101f4565b6101a4565b6040516100b691906102" +
        "37565b60405180910390f35b5f602052805f5260405f205f91509050548" +
        "1565b5f805f8481526020019081526020015f205414610125576040517f0" +
        "8c379a000000000000000000000000000000000000000000000000000000" +
        "000815260040161011c90610342565b60405180910390fd5b425f808481" +
        "526020019081526020015f20819055508073ffffffffffffffffffffffffffff" +
        "ffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16837" +
        "f87114e1f2736d94a9328671e9f05cf15e9777a1f425eec53fcf8180f423b" +
        "1cf1426040516101989190610237565b60405180910390a45050565b5f805f" +
        "8381526020019081526020015f20549050919050565b5f80fd5b5f81905091" +
        "9050565b6101d3816101c1565b81146101dd575f80fd5b50565b5f81359050" +
        "6101ee816101ca565b92915050565b5f60208284031215610209576102086101" +
        "bd565b5b5f610216848285016101e0565b91505092915050565b5f81905091" +
        "9050565b6102318161021f565b82525050565b5f6020820190506102" +
        "4a5f830184610228565b92915050565b5f73ffffffffffffffffffffffffffffff" +
        "ffffffffff82169050919050565b5f61027982610250565b9050919050565b6102" +
        "898161026f565b8114610293575f80fd5b50565b5f81359050" +
        "6102a481610280565b92915050565b5f80604083850312156102c0576102bf6101" +
        "bd565b5b5f6102cd858286016101e0565b92505060206102de85828601610296565b" +
        "9150509250929050565b5f82825260208201905092915050565b7f486173682061" +
        "6c726561647920657869737473000000000000000000000000005f82015250565b5f" +
        "61032c6013836102e8565b9150610337826102f8565b602082019050919050565b5f" +
        "6020820190508181035f83015261035981610320565b905091905056fea264697066" +
        "73582212209bda62e9950e5aa7dc840edd47f107d5acfbdf71b5f8ef509d3717dbc4" +
        "3b4e9464736f6c634300081a0033";

    public static void main(String[] args) {
        // Use the exact original hex without line breaks
        String hex = ORIGINAL.replaceAll("\\s+", "");
        byte[] orig = hexToBytes(hex);

        System.out.println("Original length: " + orig.length);

        // Pass 1: build old-offset to new-offset map
        int[] oldToNew = new int[orig.length + 1];
        int newOff = 0;
        for (int i = 0; i < orig.length; ) {
            oldToNew[i] = newOff;
            int op = orig[i] & 0xFF;
            if (op == 0x5f) {
                newOff += 2; i++;
            } else {
                newOff++; i++;
                if (op >= 0x60 && op <= 0x7f) {
                    int n = op - 0x5f;
                    newOff += n; i += n;
                }
            }
        }
        oldToNew[orig.length] = newOff;
        int newLen = newOff;
        System.out.println("New length:      " + newLen);

        // Constructor ends at old offset 0x1c (= 28)
        final int CTOR_END = 0x1c;
        int newRuntimeStart = oldToNew[CTOR_END];
        int oldRuntimeSize  = orig.length - CTOR_END;
        int newRuntimeSize  = newLen - newRuntimeStart;
        System.out.println("Old runtime: start=0x1c size=0x" + Integer.toHexString(oldRuntimeSize));
        System.out.println("New runtime: start=0x" + Integer.toHexString(newRuntimeStart) +
                           " size=0x" + Integer.toHexString(newRuntimeSize));

        // Pass 2: emit patched bytes
        byte[] out = new byte[newLen];
        int wi = 0;
        for (int i = 0; i < orig.length; ) {
            int op = orig[i] & 0xFF;
            if (op == 0x5f) {
                out[wi++] = 0x60; out[wi++] = 0x00;
                i++;
            } else {
                out[wi++] = (byte) op;
                i++;
                if (op >= 0x60 && op <= 0x7f) {
                    int n = op - 0x5f;
                    int argVal = 0;
                    for (int j = 0; j < n; j++) argVal = (argVal << 8) | (orig[i + j] & 0xFF);

                    boolean patched = false;
                    // In constructor only: patch runtime-size and runtime-start PUSH2 args
                    if ((i - 1) < CTOR_END && n == 2) {
                        if (argVal == oldRuntimeSize) {
                            System.out.println("  Patched runtime SIZE  0x" +
                                Integer.toHexString(oldRuntimeSize) + " -> 0x" + Integer.toHexString(newRuntimeSize));
                            out[wi++] = (byte)((newRuntimeSize >> 8) & 0xFF);
                            out[wi++] = (byte)(newRuntimeSize & 0xFF);
                            patched = true;
                        } else if (argVal == CTOR_END) {
                            System.out.println("  Patched runtime START 0x1c -> 0x" +
                                Integer.toHexString(newRuntimeStart));
                            out[wi++] = (byte)((newRuntimeStart >> 8) & 0xFF);
                            out[wi++] = (byte)(newRuntimeStart & 0xFF);
                            patched = true;
                        }
                        // Also patch the JUMPDEST target (PUSH1 0x0e -> new offset)
                        else if (n == 1 && argVal < CTOR_END) {
                            int newTarget = oldToNew[argVal];
                            if (newTarget != argVal) {
                                System.out.println("  Patched jump target 0x" +
                                    Integer.toHexString(argVal) + " -> 0x" + Integer.toHexString(newTarget));
                                out[wi++] = (byte)(newTarget & 0xFF);
                                patched = true;
                            }
                        }
                    }
                    if (!patched) {
                        for (int j = 0; j < n; j++) out[wi++] = orig[i + j];
                    }
                    i += n;
                }
            }
        }

        String result = bytesToHex(out);
        System.out.println("\n=== PARIS BINARY ===");
        System.out.println(result);
        System.out.println("====================");

        // Verify no PUSH0 in opcode positions
        byte[] v = hexToBytes(result);
        int vi = 0; boolean fail = false;
        while (vi < v.length) {
            int op = v[vi] & 0xFF;
            if (op == 0x5f) { System.err.println("PUSH0 at " + vi); fail = true; }
            vi++;
            if (op >= 0x60 && op <= 0x7f) vi += (op - 0x5f);
        }
        System.out.println(fail ? "VERIFICATION FAILED" : "VERIFICATION OK — no PUSH0");
    }

    static byte[] hexToBytes(String hex) {
        byte[] d = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2)
            d[i/2] = (byte)((Character.digit(hex.charAt(i), 16) << 4)
                          + Character.digit(hex.charAt(i+1), 16));
        return d;
    }

    static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }
}

