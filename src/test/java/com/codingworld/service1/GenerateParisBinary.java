package com.codingworld.service1;

/**
 * Run this main() to print the correct paris-compatible binary.
 * It does a two-pass translation:
 *   Pass 1: walk the bytecode, build a mapping of old-offset → new-offset
 *           (every PUSH0 at position X shifts everything after it by +1)
 *   Pass 2: re-emit the bytecode replacing PUSH0→PUSH1 0x00 AND patching
 *           PUSH1/PUSH2 arguments that encode internal bytecode offsets
 *           (CODECOPY size/offset in the constructor)
 *
 * The original Shanghai binary is split into two sections:
 *   [constructor] 6080604052348015600e575f80fd5b506103968061001c5f395ff3fe
 *   [runtime]     608060405234801561000f575f80fd5b...
 *
 * The constructor uses:
 *   PUSH2 0x0396  → size of runtime code  (must be updated)
 *   PUSH2 0x001c  → start offset of runtime in deploy binary (must be updated)
 */
public class GenerateParisBinary {

    // Original Shanghai binary from ChatVerification.bin
    static final String ORIGINAL =
        "6080604052348015600e575f80fd5b506103968061001c5f395ff3fe" +
        "608060405234801561000f575f80fd5b506004361061003f575f3560e01c80637269367b1461004357806385f09f1214610073578063ef020f4a1461008f575b5f80fd5b61005d600480360381019061005891906101f4565b6100bf565b60405161006a9190610237565b60405180910390f35b61008d600480360381019061008891906102aa565b6100d3565b005b6100a960048036038101906100a491906101f4565b6101a4565b6040516100b69190610237565b60405180910390f35b5f602052805f5260405f205f915090505481565b5f805f8481526020019081526020015f205414610125576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161011c90610342565b60405180910390fd5b425f808481526020019081526020015f20819055508073ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16837f87114e1f2736d94a9328671e9f05cf15e9777a1f425eec53fcf8180f423b1cf1426040516101989190610237565b60405180910390a45050565b5f805f8381526020019081526020015f20549050919050565b5f80fd5b5f819050919050565b6101d3816101c1565b81146101dd575f80fd5b50565b5f813590506101ee816101ca565b92915050565b5f60208284031215610209576102086101bd565b5b5f610216848285016101e0565b91505092915050565b5f819050919050565b6102318161021f565b82525050565b5f60208201905061024a5f830184610228565b92915050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61027982610250565b9050919050565b6102898161026f565b8114610293575f80fd5b50565b5f813590506102a481610280565b92915050565b5f80604083850312156102c0576102bf6101bd565b5b5f6102cd858286016101e0565b92505060206102de85828601610296565b9150509250929050565b5f82825260208201905092915050565b7f4861736820616c726561647920657869737473000000000000000000000000005f82015250565b5f61032c6013836102e8565b9150610337826102f8565b602082019050919050565b5f6020820190508181035f83015261035981610320565b905091905056fea26469706673582212209bda62e9950e5aa7dc840edd47f107d5acfbdf71b5f8ef509d3717dbc43b4e9464736f6c634300081a0033";

    public static void main(String[] args) {
        byte[] orig = hexToBytes(ORIGINAL);

        // --- Pass 1: count PUSH0 opcodes (each adds 1 byte) to know the new length ---
        // Also build old→new offset map
        int[] oldToNew = new int[orig.length + 1];
        int newOffset = 0;
        for (int i = 0; i < orig.length; ) {
            oldToNew[i] = newOffset;
            int op = orig[i] & 0xFF;
            if (op == 0x5f) {
                newOffset += 2; // PUSH0(1) → PUSH1 0x00 (2)
                i += 1;
            } else {
                newOffset += 1;
                i += 1;
                if (op >= 0x60 && op <= 0x7f) {
                    int n = op - 0x5f;
                    newOffset += n;
                    i += n;
                }
            }
        }
        oldToNew[orig.length] = newOffset;
        int newLen = newOffset;

        // The constructor section ends at the 0xfe separator byte.
        // Find the split: the constructor copies runtime to memory and returns it.
        // The runtime starts right after the fe (INVALID) opcode used as separator.
        // In this binary the constructor is bytes 0..27 (28 bytes = 0x1c) and
        // runtime starts at offset 0x1c. The PUSH2 0x001c at position 0x09 encodes this.
        // We need to know where the runtime starts in the NEW binary.
        int runtimeStartOld = 0x1c; // known from original binary
        int runtimeStartNew = oldToNew[runtimeStartOld];

        // Runtime size in old binary
        int runtimeSizeOld = orig.length - runtimeStartOld;
        // Runtime size in new binary
        int runtimeSizeNew = newLen - runtimeStartNew;

        System.out.println("Old total length : " + orig.length + " (0x" + Integer.toHexString(orig.length) + ")");
        System.out.println("New total length : " + newLen    + " (0x" + Integer.toHexString(newLen) + ")");
        System.out.println("Old runtime start: 0x" + Integer.toHexString(runtimeStartOld) + ", size: 0x" + Integer.toHexString(runtimeSizeOld));
        System.out.println("New runtime start: 0x" + Integer.toHexString(runtimeStartNew) + ", size: 0x" + Integer.toHexString(runtimeSizeNew));

        // --- Pass 2: emit the new bytecode ---
        byte[] out = new byte[newLen];
        int wi = 0;
        for (int i = 0; i < orig.length; ) {
            int op = orig[i] & 0xFF;
            if (op == 0x5f) {
                out[wi++] = 0x60;
                out[wi++] = 0x00;
                i++;
            } else {
                out[wi++] = (byte) op;
                i++;
                if (op >= 0x60 && op <= 0x7f) {
                    int n = op - 0x5f;
                    // Read the original argument bytes
                    byte[] argBytes = new byte[n];
                    for (int j = 0; j < n; j++) {
                        argBytes[j] = orig[i + j];
                    }
                    // Check if this PUSH encodes the runtime size or runtime start offset
                    // These appear in the constructor only (before runtimeStartOld)
                    // as PUSH2 instructions at specific known positions
                    int argVal = 0;
                    for (byte b : argBytes) argVal = (argVal << 8) | (b & 0xFF);

                    boolean patched = false;
                    if ((i - 1) < runtimeStartOld) { // we are in the constructor
                        if (argVal == runtimeSizeOld && n == 2) {
                            // Patch runtime size
                            int newVal = runtimeSizeNew;
                            System.out.println("  Patching runtime SIZE at old pos " + (i-1) + ": 0x" +
                                Integer.toHexString(runtimeSizeOld) + " → 0x" + Integer.toHexString(newVal));
                            out[wi++] = (byte)((newVal >> 8) & 0xFF);
                            out[wi++] = (byte)(newVal & 0xFF);
                            patched = true;
                        } else if (argVal == runtimeStartOld && n == 2) {
                            // Patch runtime start offset
                            int newVal = runtimeStartNew;
                            System.out.println("  Patching runtime START at old pos " + (i-1) + ": 0x" +
                                Integer.toHexString(runtimeStartOld) + " → 0x" + Integer.toHexString(newVal));
                            out[wi++] = (byte)((newVal >> 8) & 0xFF);
                            out[wi++] = (byte)(newVal & 0xFF);
                            patched = true;
                        }
                    }
                    if (!patched) {
                        for (byte b : argBytes) out[wi++] = b;
                    }
                    i += n;
                }
            }
        }

        String result = bytesToHex(out);
        System.out.println("\nParis-compatible binary:");
        System.out.println(result);

        // Verify no PUSH0 remains
        System.out.println("\nVerifying no PUSH0 in opcode positions...");
        byte[] check = hexToBytes(result);
        int idx = 0;
        boolean bad = false;
        while (idx < check.length) {
            int op = check[idx] & 0xFF;
            if (op == 0x5f) {
                System.out.println("  PUSH0 still at position " + idx);
                bad = true;
            }
            idx++;
            if (op >= 0x60 && op <= 0x7f) idx += (op - 0x5f);
        }
        if (!bad) System.out.println("  OK — no PUSH0 found.");
    }

    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i/2] = (byte)((Character.digit(hex.charAt(i),16)<<4) + Character.digit(hex.charAt(i+1),16));
        return data;
    }

    static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }
}

