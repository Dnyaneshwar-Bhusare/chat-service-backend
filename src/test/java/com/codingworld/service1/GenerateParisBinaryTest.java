package com.codingworld.service1;

import org.junit.jupiter.api.Test;

public class GenerateParisBinaryTest {

    static final String ORIGINAL =
        "6080604052348015600e575f80fd5b506103968061001c5f395ff3fe" +
        "608060405234801561000f575f80fd5b506004361061003f575f3560e01c80637269367b1461004357806385f09f1214610073578063ef020f4a1461008f575b5f80fd5b61005d600480360381019061005891906101f4565b6100bf565b60405161006a9190610237565b60405180910390f35b61008d600480360381019061008891906102aa565b6100d3565b005b6100a960048036038101906100a491906101f4565b6101a4565b6040516100b69190610237565b60405180910390f35b5f602052805f5260405f205f915090505481565b5f805f8481526020019081526020015f205414610125576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161011c90610342565b60405180910390fd5b425f808481526020019081526020015f20819055508073ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16837f87114e1f2736d94a9328671e9f05cf15e9777a1f425eec53fcf8180f423b1cf1426040516101989190610237565b60405180910390a45050565b5f805f8381526020019081526020015f20549050919050565b5f80fd5b5f819050919050565b6101d3816101c1565b81146101dd575f80fd5b50565b5f813590506101ee816101ca565b92915050565b5f60208284031215610209576102086101bd565b5b5f610216848285016101e0565b91505092915050565b5f819050919050565b6102318161021f565b82525050565b5f60208201905061024a5f830184610228565b92915050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61027982610250565b9050919050565b6102898161026f565b8114610293575f80fd5b50565b5f813590506102a481610280565b92915050565b5f80604083850312156102c0576102bf6101bd565b5b5f6102cd858286016101e0565b92505060206102de85828601610296565b9150509250929050565b5f82825260208201905092915050565b7f4861736820616c726561647920657869737473000000000000000000000000005f82015250565b5f61032c6013836102e8565b9150610337826102f8565b602082019050919050565b5f6020820190508181035f83015261035981610320565b905091905056fea26469706673582212209bda62e9950e5aa7dc840edd47f107d5acfbdf71b5f8ef509d3717dbc43b4e9464736f6c634300081a0033";

    @Test
    public void generateAndPrint() {
        byte[] orig = hexToBytes(ORIGINAL);

        // Pass 1: build old-offset → new-offset map
        int[] oldToNew = new int[orig.length + 1];
        int newOffset = 0;
        for (int i = 0; i < orig.length; ) {
            oldToNew[i] = newOffset;
            int op = orig[i] & 0xFF;
            if (op == 0x5f) {
                newOffset += 2;
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

        int runtimeStartOld = 0x1c;
        int runtimeStartNew = oldToNew[runtimeStartOld];
        int runtimeSizeOld  = orig.length - runtimeStartOld;
        int runtimeSizeNew  = newLen - runtimeStartNew;

        System.out.println("Old len=" + orig.length + " new len=" + newLen);
        System.out.println("Old runtime start=0x" + Integer.toHexString(runtimeStartOld) + " size=0x" + Integer.toHexString(runtimeSizeOld));
        System.out.println("New runtime start=0x" + Integer.toHexString(runtimeStartNew) + " size=0x" + Integer.toHexString(runtimeSizeNew));

        // Pass 2: emit patched bytecode
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
                    byte[] arg = new byte[n];
                    for (int j = 0; j < n; j++) arg[j] = orig[i + j];
                    int argVal = 0;
                    for (byte b : arg) argVal = (argVal << 8) | (b & 0xFF);

                    boolean patched = false;
                    if ((i - 1) < runtimeStartOld && n == 2) {
                        if (argVal == runtimeSizeOld) {
                            System.out.println("  Patch SIZE  at old[" + (i-1) + "]: 0x" + Integer.toHexString(runtimeSizeOld) + " → 0x" + Integer.toHexString(runtimeSizeNew));
                            out[wi++] = (byte)((runtimeSizeNew >> 8) & 0xFF);
                            out[wi++] = (byte)(runtimeSizeNew & 0xFF);
                            patched = true;
                        } else if (argVal == runtimeStartOld) {
                            System.out.println("  Patch START at old[" + (i-1) + "]: 0x" + Integer.toHexString(runtimeStartOld) + " → 0x" + Integer.toHexString(runtimeStartNew));
                            out[wi++] = (byte)((runtimeStartNew >> 8) & 0xFF);
                            out[wi++] = (byte)(runtimeStartNew & 0xFF);
                            patched = true;
                        }
                    }
                    if (!patched) for (byte b : arg) out[wi++] = b;
                    i += n;
                }
            }
        }

        String result = bytesToHex(out);
        System.out.println("\nPARIS_BINARY=");
        System.out.println(result);

        // Verify
        byte[] check = hexToBytes(result);
        int idx = 0; boolean bad = false;
        while (idx < check.length) {
            int op2 = check[idx] & 0xFF;
            if (op2 == 0x5f) { System.out.println("PUSH0 still at " + idx); bad = true; }
            idx++;
            if (op2 >= 0x60 && op2 <= 0x7f) idx += (op2 - 0x5f);
        }
        System.out.println(bad ? "FAILED" : "OK — no PUSH0");
    }

    static byte[] hexToBytes(String hex) {
        byte[] d = new byte[hex.length()/2];
        for (int i = 0; i < hex.length(); i+=2)
            d[i/2] = (byte)((Character.digit(hex.charAt(i),16)<<4)+Character.digit(hex.charAt(i+1),16));
        return d;
    }
    static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length*2);
        for (byte x : b) sb.append(String.format("%02x", x&0xFF));
        return sb.toString();
    }
}

