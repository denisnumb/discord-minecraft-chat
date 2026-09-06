package com.denisnumb.discord_chat_mod.network;

import java.util.*;

public final class BigPacketsTransceiver {
    private BigPacketsTransceiver() {}

    @FunctionalInterface
    public interface SendFunction {
        void send(int partIndex, int totalParts, byte[] part);
    }

    private static final int MAX_PART_SIZE = 32000;

    public static void send(byte[] data, SendFunction sendFunction) {
        int totalParts = (int) Math.ceil(data.length / (double) MAX_PART_SIZE);

        for (int i = 0; i < totalParts; i++) {
            int start = i * MAX_PART_SIZE;
            int end = Math.min(start + MAX_PART_SIZE, data.length);
            byte[] part = Arrays.copyOfRange(data, start, end);
            sendFunction.send(i, totalParts, part);
        }
    }

    public static Optional<byte[]> receivePart(
            Map<Long, ArrayList<byte[]>> receivedParts,
            long sendTime,
            int partIndex,
            int totalParts,
            byte[] partData
    ) {
        receivedParts.computeIfAbsent(sendTime, t -> new ArrayList<>(Collections.nCopies(totalParts, null)));
        receivedParts.get(sendTime).set(partIndex, partData);

        if (receivedParts.get(sendTime).stream().noneMatch(Objects::isNull)) {
            byte[] data = mergeParts(receivedParts.get(sendTime));
            receivedParts.remove(sendTime);
            return Optional.of(data);
        }
        return Optional.empty();
    }

    private static byte[] mergeParts(ArrayList<byte[]> parts) {
        int totalSize = parts.stream().mapToInt(arr -> arr.length).sum();
        byte[] result = new byte[totalSize];

        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }

        return result;
    }
}
